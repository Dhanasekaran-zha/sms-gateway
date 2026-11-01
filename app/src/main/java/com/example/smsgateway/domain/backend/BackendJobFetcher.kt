package com.example.smsgateway.domain.backend

import com.example.smsgateway.domain.model.SmsJob

interface BackendJobFetcher {
    suspend fun fetchJob(jobId: String): Result<SmsJob>
}

