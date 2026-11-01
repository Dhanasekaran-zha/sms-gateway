package com.example.smsgateway

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.smsgateway.data.local.AppDatabase
import com.example.smsgateway.data.local.dao.SmsJobDao
import com.example.smsgateway.data.local.entity.SmsJobEntity
import com.example.smsgateway.data.local.entity.StatusHistoryEntity
import com.example.smsgateway.domain.model.JobSource
import com.example.smsgateway.domain.model.SmsJobStatus
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
class SmsJobDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: SmsJobDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.smsJobDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createSampleJobEntity(
        jobId: String = "job-123",
        status: SmsJobStatus = SmsJobStatus.QUEUED,
    ): SmsJobEntity {
        return SmsJobEntity(
            jobId = jobId,
            recipient = "+919876543210",
            messageBody = "Test OTP: 123456",
            simSlot = 0,
            subscriptionId = 1,
            status = status,
            source = JobSource.MANUAL,
            createdAt = 1000L,
            updatedAt = 1000L,
            sentAt = null,
            deliveredAt = null,
            errorMessage = null,
            retryCount = 0,
        )
    }

    @Test
    fun insertAndGetJobById() = runBlocking {
        val job = createSampleJobEntity("job-1")
        dao.insertJob(job)

        val retrieved = dao.getJobById("job-1")
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.jobId).isEqualTo("job-1")
        assertThat(retrieved?.recipient).isEqualTo("+919876543210")
        assertThat(retrieved?.status).isEqualTo(SmsJobStatus.QUEUED)
    }

    @Test
    fun insertDuplicateJobId_throwsConstraintException() = runBlocking {
        val job1 = createSampleJobEntity("job-duplicate")
        val job2 = createSampleJobEntity("job-duplicate")

        dao.insertJob(job1)
        var exceptionThrown = false
        try {
            dao.insertJob(job2)
        } catch (e: SQLiteConstraintException) {
            exceptionThrown = true
        }
        assertThat(exceptionThrown).isTrue()
    }

    @Test
    fun updateJob_persistsChanges() = runBlocking {
        val job = createSampleJobEntity("job-update")
        dao.insertJob(job)

        val updated = job.copy(
            status = SmsJobStatus.SUBMITTED,
            sentAt = 2000L,
            updatedAt = 2000L,
        )
        dao.updateJob(updated)

        val retrieved = dao.getJobById("job-update")
        assertThat(retrieved?.status).isEqualTo(SmsJobStatus.SUBMITTED)
        assertThat(retrieved?.sentAt).isEqualTo(2000L)
    }

    @Test
    fun observeJobsByStatus_filtersCorrectly() = runBlocking {
        dao.insertJob(createSampleJobEntity("job-q1", SmsJobStatus.QUEUED))
        dao.insertJob(createSampleJobEntity("job-s1", SmsJobStatus.SUBMITTED))
        dao.insertJob(createSampleJobEntity("job-q2", SmsJobStatus.QUEUED))

        val queuedJobs = dao.observeJobsByStatus(SmsJobStatus.QUEUED).first()
        assertThat(queuedJobs).hasSize(2)
        assertThat(queuedJobs.map { it.jobId }).containsExactly("job-q1", "job-q2")

        val submittedJobs = dao.observeJobsByStatus(SmsJobStatus.SUBMITTED).first()
        assertThat(submittedJobs).hasSize(1)
        assertThat(submittedJobs.first().jobId).isEqualTo("job-s1")
    }

    @Test
    fun insertAndObserveStatusHistory() = runBlocking {
        dao.insertJob(createSampleJobEntity("job-hist"))

        dao.insertStatusHistory(
            StatusHistoryEntity(
                jobId = "job-hist",
                fromStatus = null,
                toStatus = SmsJobStatus.QUEUED,
                timestamp = 1000L,
                detail = "Created",
            )
        )
        dao.insertStatusHistory(
            StatusHistoryEntity(
                jobId = "job-hist",
                fromStatus = SmsJobStatus.QUEUED,
                toStatus = SmsJobStatus.SUBMITTING,
                timestamp = 1500L,
                detail = "Sending via SmsManager",
            )
        )

        val history = dao.observeStatusHistory("job-hist").first()
        assertThat(history).hasSize(2)
        assertThat(history[0].toStatus).isEqualTo(SmsJobStatus.QUEUED)
        assertThat(history[1].toStatus).isEqualTo(SmsJobStatus.SUBMITTING)
    }
}

