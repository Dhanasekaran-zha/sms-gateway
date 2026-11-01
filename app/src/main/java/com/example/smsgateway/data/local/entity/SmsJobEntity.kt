package com.example.smsgateway.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.smsgateway.domain.model.JobSource
import com.example.smsgateway.domain.model.SmsJobStatus

@Entity(
    tableName = "sms_jobs",
    indices = [
        Index(value = ["status"]),
        Index(value = ["created_at"]),
    ],
)
data class SmsJobEntity(
    @PrimaryKey
    @ColumnInfo(name = "job_id")
    val jobId: String,

    @ColumnInfo(name = "recipient")
    val recipient: String,

    @ColumnInfo(name = "message_body")
    val messageBody: String,

    @ColumnInfo(name = "sim_slot")
    val simSlot: Int?,

    @ColumnInfo(name = "subscription_id")
    val subscriptionId: Int?,

    @ColumnInfo(name = "status")
    val status: SmsJobStatus,

    @ColumnInfo(name = "source")
    val source: JobSource,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "sent_at")
    val sentAt: Long?,

    @ColumnInfo(name = "delivered_at")
    val deliveredAt: Long?,

    @ColumnInfo(name = "error_message")
    val errorMessage: String?,

    @ColumnInfo(name = "retry_count")
    val retryCount: Int,
)

