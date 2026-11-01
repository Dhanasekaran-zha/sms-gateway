package com.example.smsgateway

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.example.smsgateway.data.backend.StubBackendJobFetcher
import com.example.smsgateway.data.local.AppDatabase
import com.example.smsgateway.data.repository.SmsJobRepositoryImpl
import com.example.smsgateway.domain.model.JobSource
import com.example.smsgateway.domain.model.SmsJob
import com.example.smsgateway.domain.model.SmsJobStatus
import com.example.smsgateway.domain.repository.SmsJobRepository
import com.example.smsgateway.platform.sms.SimSubscriptionHelper
import com.example.smsgateway.platform.sms.SmsSender
import com.example.smsgateway.platform.work.SmsSendWorker
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SmsSendWorkerTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: SmsJobRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SmsJobRepositoryImpl(database, database.smsJobDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private class FakeSmsSender : SmsSender {
        var submittedJobId: String? = null
        var shouldSucceed: Boolean = true

        override suspend fun submitSms(
            jobId: String,
            recipient: String,
            messageBody: String,
            subscriptionId: Int?,
        ): Result<Unit> {
            submittedJobId = jobId
            return if (shouldSucceed) Result.success(Unit) else Result.failure(RuntimeException("Submission failed"))
        }
    }

    private class FakeSimHelper(context: Context, val hasSms: Boolean) : SimSubscriptionHelper(context) {
        override fun hasSendSmsPermission(): Boolean = hasSms
    }

    @Test
    fun `worker fails when permission is missing and marks job as FAILED`() = runBlocking {
        val job = SmsJob(
            jobId = "worker-perm-1",
            recipient = "+919876543210",
            messageBody = "Test OTP",
            status = SmsJobStatus.QUEUED,
            source = JobSource.BACKEND_FCM,
        )
        repository.createJob(job)

        val simHelper = FakeSimHelper(context, hasSms = false)
        val fakeSender = FakeSmsSender()
        val backendFetcher = StubBackendJobFetcher(repository)

        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker {
                return SmsSendWorker(appContext, workerParameters, repository, fakeSender, simHelper, backendFetcher)
            }
        }

        val worker = TestListenableWorkerBuilder<SmsSendWorker>(context)
            .setInputData(workDataOf(SmsSendWorker.KEY_JOB_ID to "worker-perm-1"))
            .setWorkerFactory(factory)
            .build()

        val result = worker.doWork()
        assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)

        val updated = repository.getJobById("worker-perm-1")
        assertThat(updated?.status).isEqualTo(SmsJobStatus.FAILED)
        assertThat(fakeSender.submittedJobId).isNull()
    }

    @Test
    fun `worker submits SMS when permission is granted and job is QUEUED`() = runBlocking {
        val job = SmsJob(
            jobId = "worker-queued-1",
            recipient = "+919876543210",
            messageBody = "Test message for worker",
            status = SmsJobStatus.QUEUED,
            source = JobSource.BACKEND_FCM,
        )
        repository.createJob(job)

        val simHelper = FakeSimHelper(context, hasSms = true)
        val fakeSender = FakeSmsSender()
        val backendFetcher = StubBackendJobFetcher(repository)

        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker {
                return SmsSendWorker(appContext, workerParameters, repository, fakeSender, simHelper, backendFetcher)
            }
        }

        val worker = TestListenableWorkerBuilder<SmsSendWorker>(context)
            .setInputData(workDataOf(SmsSendWorker.KEY_JOB_ID to "worker-queued-1"))
            .setWorkerFactory(factory)
            .build()

        val result = worker.doWork()
        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        assertThat(fakeSender.submittedJobId).isEqualTo("worker-queued-1")
    }

    @Test
    fun `worker skips execution when job is already SUBMITTED or SUBMISSION_UNKNOWN`() = runBlocking {
        val jobSubmitted = SmsJob(
            jobId = "worker-sub-1",
            recipient = "+919876543210",
            messageBody = "Already submitted",
            status = SmsJobStatus.SUBMITTED,
            source = JobSource.BACKEND_FCM,
        )
        repository.createJob(jobSubmitted)

        val simHelper = FakeSimHelper(context, hasSms = true)
        val fakeSender = FakeSmsSender()
        val backendFetcher = StubBackendJobFetcher(repository)

        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker {
                return SmsSendWorker(appContext, workerParameters, repository, fakeSender, simHelper, backendFetcher)
            }
        }

        val worker = TestListenableWorkerBuilder<SmsSendWorker>(context)
            .setInputData(workDataOf(SmsSendWorker.KEY_JOB_ID to "worker-sub-1"))
            .setWorkerFactory(factory)
            .build()

        val result = worker.doWork()
        assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        // Ensure no redundant SMS submission occurred
        assertThat(fakeSender.submittedJobId).isNull()
    }
}
