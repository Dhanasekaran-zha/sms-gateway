package com.example.smsgateway.domain.repository

import com.example.smsgateway.domain.model.SmsJob
import com.example.smsgateway.domain.model.SmsJobStatus
import com.example.smsgateway.domain.model.StatusHistoryEntry
import kotlinx.coroutines.flow.Flow

interface SmsJobRepository {
    suspend fun createJob(job: SmsJob): Result<SmsJob>

    suspend fun updateJobStatus(
        jobId: String,
        newStatus: SmsJobStatus,
        detail: String? = null,
        errorMessage: String? = null,
    ): Result<SmsJob>

    suspend fun getJobById(jobId: String): SmsJob?

    fun observeJobById(jobId: String): Flow<SmsJob?>

    fun observeAllJobs(): Flow<List<SmsJob>>

    fun observeJobsByStatus(status: SmsJobStatus): Flow<List<SmsJob>>

    suspend fun getJobsByStatuses(statuses: List<SmsJobStatus>): List<SmsJob>

    fun observeStatusHistory(jobId: String): Flow<List<StatusHistoryEntry>>

    suspend fun getStatusHistory(jobId: String): List<StatusHistoryEntry>
}

