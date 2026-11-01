package com.example.smsgateway.data.repository

import androidx.room.withTransaction
import com.example.smsgateway.data.local.AppDatabase
import com.example.smsgateway.data.local.dao.SmsJobDao
import com.example.smsgateway.data.local.entity.StatusHistoryEntity
import com.example.smsgateway.data.local.entity.toDomain
import com.example.smsgateway.data.local.entity.toEntity
import com.example.smsgateway.domain.model.DuplicateJobException
import com.example.smsgateway.domain.model.InvalidStateTransitionException
import com.example.smsgateway.domain.model.JobNotFoundException
import com.example.smsgateway.domain.model.SmsJob
import com.example.smsgateway.domain.model.SmsJobStatus
import com.example.smsgateway.domain.model.StatusHistoryEntry
import com.example.smsgateway.domain.repository.SmsJobRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsJobRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val dao: SmsJobDao,
) : SmsJobRepository {

    override suspend fun createJob(job: SmsJob): Result<SmsJob> {
        return try {
            database.withTransaction {
                val existing = dao.getJobById(job.jobId)
                if (existing != null) {
                    return@withTransaction Result.failure(DuplicateJobException(job.jobId))
                }

                dao.insertJob(job.toEntity())
                dao.insertStatusHistory(
                    StatusHistoryEntity(
                        jobId = job.jobId,
                        fromStatus = null,
                        toStatus = job.status,
                        timestamp = job.createdAt,
                        detail = "Job created with initial status ${job.status}",
                    )
                )
                Result.success(job)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create SMS job ${job.jobId}")
            Result.failure(e)
        }
    }

    override suspend fun updateJobStatus(
        jobId: String,
        newStatus: SmsJobStatus,
        detail: String?,
        errorMessage: String?,
    ): Result<SmsJob> {
        return try {
            database.withTransaction {
                val existing = dao.getJobById(jobId)
                    ?: return@withTransaction Result.failure(JobNotFoundException(jobId))

                val currentStatus = existing.status
                if (!currentStatus.canTransitionTo(newStatus)) {
                    Timber.w("Illegal state transition attempted for job $jobId: $currentStatus -> $newStatus")
                    return@withTransaction Result.failure(
                        InvalidStateTransitionException(currentStatus, newStatus)
                    )
                }

                val now = System.currentTimeMillis()
                val updated = existing.copy(
                    status = newStatus,
                    updatedAt = now,
                    sentAt = if (newStatus == SmsJobStatus.SUBMITTED && existing.sentAt == null) now else existing.sentAt,
                    deliveredAt = if (newStatus == SmsJobStatus.DELIVERED && existing.deliveredAt == null) now else existing.deliveredAt,
                    errorMessage = errorMessage ?: existing.errorMessage,
                )

                dao.updateJob(updated)
                dao.insertStatusHistory(
                    StatusHistoryEntity(
                        jobId = jobId,
                        fromStatus = currentStatus,
                        toStatus = newStatus,
                        timestamp = now,
                        detail = detail,
                    )
                )

                Timber.d("Job $jobId transitioned: $currentStatus -> $newStatus")
                Result.success(updated.toDomain())
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update status for job $jobId to $newStatus")
            Result.failure(e)
        }
    }

    override suspend fun getJobById(jobId: String): SmsJob? {
        return dao.getJobById(jobId)?.toDomain()
    }

    override fun observeJobById(jobId: String): Flow<SmsJob?> {
        return dao.observeJobById(jobId).map { it?.toDomain() }
    }

    override fun observeAllJobs(): Flow<List<SmsJob>> {
        return dao.observeAllJobs().map { entities -> entities.map { it.toDomain() } }
    }

    override fun observeJobsByStatus(status: SmsJobStatus): Flow<List<SmsJob>> {
        return dao.observeJobsByStatus(status).map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getJobsByStatuses(statuses: List<SmsJobStatus>): List<SmsJob> {
        return dao.getJobsByStatuses(statuses).map { it.toDomain() }
    }

    override fun observeStatusHistory(jobId: String): Flow<List<StatusHistoryEntry>> {
        return dao.observeStatusHistory(jobId).map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getStatusHistory(jobId: String): List<StatusHistoryEntry> {
        return dao.getStatusHistory(jobId).map { it.toDomain() }
    }
}

