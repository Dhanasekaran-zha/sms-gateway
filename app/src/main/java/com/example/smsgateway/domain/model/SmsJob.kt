package com.example.smsgateway.domain.model

data class SmsJob(
    val jobId: String,
    val recipient: String,
    val messageBody: String,
    val simSlot: Int? = null,
    val subscriptionId: Int? = null,
    val status: SmsJobStatus = SmsJobStatus.QUEUED,
    val source: JobSource = JobSource.MANUAL,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sentAt: Long? = null,
    val deliveredAt: Long? = null,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
)

