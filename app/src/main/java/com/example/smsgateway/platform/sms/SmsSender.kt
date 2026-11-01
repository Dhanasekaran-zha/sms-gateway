package com.example.smsgateway.platform.sms

interface SmsSender {
    suspend fun submitSms(
        jobId: String,
        recipient: String,
        messageBody: String,
        subscriptionId: Int? = null,
    ): Result<Unit>
}

