package com.example.smsgateway

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.smsgateway.data.local.AppDatabase
import com.example.smsgateway.data.repository.SmsJobRepositoryImpl
import com.example.smsgateway.domain.model.DuplicateJobException
import com.example.smsgateway.domain.model.InvalidStateTransitionException
import com.example.smsgateway.domain.model.JobNotFoundException
import com.example.smsgateway.domain.model.JobSource
import com.example.smsgateway.domain.model.SmsJob
import com.example.smsgateway.domain.model.SmsJobStatus
import com.example.smsgateway.domain.repository.SmsJobRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SmsJobRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: SmsJobRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SmsJobRepositoryImpl(database, database.smsJobDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun sampleJob(jobId: String = "test-job-1"): SmsJob {
        return SmsJob(
            jobId = jobId,
            recipient = "+919876543210",
            messageBody = "Your OTP is 654321",
            simSlot = 0,
            status = SmsJobStatus.QUEUED,
            source = JobSource.MANUAL,
        )
    }

    @Test
    fun createJob_persistsJobAndRecordsInitialHistory() = runBlocking {
        val job = sampleJob("job-create")
        val result = repository.createJob(job)

        assertThat(result.isSuccess).isTrue()
        val created = repository.getJobById("job-create")
        assertThat(created).isNotNull()
        assertThat(created?.status).isEqualTo(SmsJobStatus.QUEUED)

        val history = repository.getStatusHistory("job-create")
        assertThat(history).hasSize(1)
        assertThat(history.first().toStatus).isEqualTo(SmsJobStatus.QUEUED)
        assertThat(history.first().fromStatus).isNull()
    }

    @Test
    fun createJob_duplicateJobId_failsWithDuplicateJobException() = runBlocking {
        val job = sampleJob("duplicate-id")
        val firstResult = repository.createJob(job)
        assertThat(firstResult.isSuccess).isTrue()

        val secondResult = repository.createJob(job)
        assertThat(secondResult.isFailure).isTrue()
        assertThat(secondResult.exceptionOrNull()).isInstanceOf(DuplicateJobException::class.java)
    }

    @Test
    fun updateJobStatus_validTransition_updatesStatusAndAddsAuditLog() = runBlocking {
        val job = sampleJob("job-transition")
        repository.createJob(job)

        // QUEUED -> SUBMITTING
        val step1 = repository.updateJobStatus("job-transition", SmsJobStatus.SUBMITTING, "Sending via SmsManager")
        assertThat(step1.isSuccess).isTrue()
        assertThat(step1.getOrNull()?.status).isEqualTo(SmsJobStatus.SUBMITTING)

        // SUBMITTING -> SUBMITTED
        val step2 = repository.updateJobStatus("job-transition", SmsJobStatus.SUBMITTED, "Result code RESULT_OK")
        assertThat(step2.isSuccess).isTrue()
        val submittedJob = repository.getJobById("job-transition")
        assertThat(submittedJob?.status).isEqualTo(SmsJobStatus.SUBMITTED)
        assertThat(submittedJob?.sentAt).isNotNull()

        // SUBMITTED -> DELIVERED
        val step3 = repository.updateJobStatus("job-transition", SmsJobStatus.DELIVERED, "Delivery PDU received")
        assertThat(step3.isSuccess).isTrue()
        val deliveredJob = repository.getJobById("job-transition")
        assertThat(deliveredJob?.status).isEqualTo(SmsJobStatus.DELIVERED)
        assertThat(deliveredJob?.deliveredAt).isNotNull()

        // Verify complete audit history
        val history = repository.getStatusHistory("job-transition")
        assertThat(history).hasSize(4) // Initial QUEUED + SUBMITTING + SUBMITTED + DELIVERED
        assertThat(history.map { it.toStatus }).containsExactly(
            SmsJobStatus.QUEUED,
            SmsJobStatus.SUBMITTING,
            SmsJobStatus.SUBMITTED,
            SmsJobStatus.DELIVERED,
        ).inOrder()
    }

    @Test
    fun updateJobStatus_invalidTransition_failsAndLeavesStateUnchanged() = runBlocking {
        val job = sampleJob("job-invalid")
        repository.createJob(job)

        // QUEUED directly to DELIVERED is illegal (must go through SUBMITTING and SUBMITTED)
        val result = repository.updateJobStatus("job-invalid", SmsJobStatus.DELIVERED)
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(InvalidStateTransitionException::class.java)

        // State remains QUEUED
        val current = repository.getJobById("job-invalid")
        assertThat(current?.status).isEqualTo(SmsJobStatus.QUEUED)

        // Only initial history exists
        val history = repository.getStatusHistory("job-invalid")
        assertThat(history).hasSize(1)
    }

    @Test
    fun updateJobStatus_unknownJob_failsWithJobNotFoundException() = runBlocking {
        val result = repository.updateJobStatus("non-existent-id", SmsJobStatus.SUBMITTING)
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(JobNotFoundException::class.java)
    }

    @Test
    fun updateJobStatus_submissionUnknown_cannotDirectlyRetryToSubmitting() = runBlocking {
        val job = sampleJob("job-unknown")
        repository.createJob(job)
        repository.updateJobStatus("job-unknown", SmsJobStatus.SUBMITTING)
        repository.updateJobStatus("job-unknown", SmsJobStatus.SUBMISSION_UNKNOWN, "SMS timeout")

        // Cannot blindly go back to SUBMITTING
        val retryResult = repository.updateJobStatus("job-unknown", SmsJobStatus.SUBMITTING)
        assertThat(retryResult.isFailure).isTrue()
        assertThat(retryResult.exceptionOrNull()).isInstanceOf(InvalidStateTransitionException::class.java)

        // Can be reconciled to FAILED
        val reconcileResult = repository.updateJobStatus("job-unknown", SmsJobStatus.FAILED, "Reconciled as failed by user")
        assertThat(reconcileResult.isSuccess).isTrue()
    }

    @Test
    fun observeAllJobs_emitsUpdatedList() = runBlocking {
        val initialJobs = repository.observeAllJobs().first()
        assertThat(initialJobs).isEmpty()

        repository.createJob(sampleJob("job-obs-1"))
        repository.createJob(sampleJob("job-obs-2"))

        val jobs = repository.observeAllJobs().first()
        assertThat(jobs).hasSize(2)
    }
}

