package com.example.smsgateway.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.smsgateway.data.local.entity.SmsJobEntity
import com.example.smsgateway.data.local.entity.StatusHistoryEntity
import com.example.smsgateway.domain.model.SmsJobStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsJobDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertJob(job: SmsJobEntity): Long

    @Update
    suspend fun updateJob(job: SmsJobEntity)

    @Query("SELECT * FROM sms_jobs WHERE job_id = :jobId")
    suspend fun getJobById(jobId: String): SmsJobEntity?

    @Query("SELECT * FROM sms_jobs WHERE job_id = :jobId")
    fun observeJobById(jobId: String): Flow<SmsJobEntity?>

    @Query("SELECT * FROM sms_jobs ORDER BY created_at DESC")
    fun observeAllJobs(): Flow<List<SmsJobEntity>>

    @Query("SELECT * FROM sms_jobs WHERE status = :status ORDER BY created_at ASC")
    fun observeJobsByStatus(status: SmsJobStatus): Flow<List<SmsJobEntity>>

    @Query("SELECT * FROM sms_jobs WHERE status IN (:statuses) ORDER BY created_at ASC")
    suspend fun getJobsByStatuses(statuses: List<SmsJobStatus>): List<SmsJobEntity>

    @Insert
    suspend fun insertStatusHistory(entry: StatusHistoryEntity): Long

    @Query("SELECT * FROM status_history WHERE job_id = :jobId ORDER BY timestamp ASC")
    fun observeStatusHistory(jobId: String): Flow<List<StatusHistoryEntity>>

    @Query("SELECT * FROM status_history WHERE job_id = :jobId ORDER BY timestamp ASC")
    suspend fun getStatusHistory(jobId: String): List<StatusHistoryEntity>
}

