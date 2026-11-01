package com.example.smsgateway

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.smsgateway.data.local.AppDatabase
import com.example.smsgateway.data.repository.SmsJobRepositoryImpl
import com.example.smsgateway.domain.model.JobSource
import com.example.smsgateway.domain.model.SmsJob
import com.example.smsgateway.domain.model.SmsJobStatus
import com.example.smsgateway.domain.repository.SmsJobRepository
import com.example.smsgateway.platform.sms.AndroidSmsSender
import com.example.smsgateway.platform.sms.SimSubscriptionHelper
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
class AndroidSmsSenderTest {

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

    @Test
    fun `submitSms fails when SEND_SMS permission is not granted`() = runBlocking {
        // Without grant, permission helper will return false in tests
        val helper = SimSubscriptionHelper(context)
        val sender = AndroidSmsSender(context, helper, repository)

        val job = SmsJob(
            jobId = "perm-test-1",
            recipient = "+919876543210",
            messageBody = "Test message",
            status = SmsJobStatus.QUEUED,
            source = JobSource.MANUAL,
        )
        repository.createJob(job)

        val result = sender.submitSms(
            jobId = "perm-test-1",
            recipient = job.recipient,
            messageBody = job.messageBody,
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(SecurityException::class.java)

        // Job status updated to FAILED with reason
        val updatedJob = repository.getJobById("perm-test-1")
        assertThat(updatedJob?.status).isEqualTo(SmsJobStatus.FAILED)
        assertThat(updatedJob?.errorMessage).contains("SEND_SMS")
    }

    @Test
    fun `submitSms fails with invalid recipient number`() = runBlocking {
        val helper = object : SimSubscriptionHelper(context) {
            // Simulate granted permission
            override fun hasSendSmsPermission(): Boolean = true
        }
        val sender = AndroidSmsSender(context, helper, repository)

        val job = SmsJob(
            jobId = "invalid-num-1",
            recipient = "invalid-phone",
            messageBody = "Test message",
            status = SmsJobStatus.QUEUED,
            source = JobSource.MANUAL,
        )
        repository.createJob(job)

        val result = sender.submitSms(
            jobId = "invalid-num-1",
            recipient = job.recipient,
            messageBody = job.messageBody,
        )

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)

        val updatedJob = repository.getJobById("invalid-num-1")
        assertThat(updatedJob?.status).isEqualTo(SmsJobStatus.FAILED)
    }

    @Test
    fun `submitSms fails with empty message body`() = runBlocking {
        val helper = object : SimSubscriptionHelper(context) {
            override fun hasSendSmsPermission(): Boolean = true
        }
        val sender = AndroidSmsSender(context, helper, repository)

        val job = SmsJob(
            jobId = "empty-body-1",
            recipient = "+919876543210",
            messageBody = "   ",
            status = SmsJobStatus.QUEUED,
            source = JobSource.MANUAL,
        )
        repository.createJob(job)

        val result = sender.submitSms(
            jobId = "empty-body-1",
            recipient = job.recipient,
            messageBody = job.messageBody,
        )

        assertThat(result.isFailure).isTrue()
        val updatedJob = repository.getJobById("empty-body-1")
        assertThat(updatedJob?.status).isEqualTo(SmsJobStatus.FAILED)
    }
}

