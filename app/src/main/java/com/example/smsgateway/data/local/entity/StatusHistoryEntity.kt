package com.example.smsgateway.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.smsgateway.domain.model.SmsJobStatus

@Entity(
    tableName = "status_history",
    foreignKeys = [
        ForeignKey(
            entity = SmsJobEntity::class,
            parentColumns = ["job_id"],
            childColumns = ["job_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["job_id"]),
        Index(value = ["timestamp"]),
    ],
)
data class StatusHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "job_id")
    val jobId: String,

    @ColumnInfo(name = "from_status")
    val fromStatus: SmsJobStatus?,

    @ColumnInfo(name = "to_status")
    val toStatus: SmsJobStatus,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "detail")
    val detail: String?,
)

