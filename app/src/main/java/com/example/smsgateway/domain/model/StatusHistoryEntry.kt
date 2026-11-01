package com.example.smsgateway.domain.model

data class StatusHistoryEntry(
    val id: Long = 0,
    val jobId: String,
    val fromStatus: SmsJobStatus?,
    val toStatus: SmsJobStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val detail: String? = null,
)

