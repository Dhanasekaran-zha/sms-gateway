package com.example.smsgateway.data.backend

import com.example.smsgateway.domain.backend.BackendJobFetcher
import com.example.smsgateway.domain.model.JobNotFoundException
import com.example.smsgateway.domain.model.SmsJob
import com.example.smsgateway.domain.repository.SmsJobRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub implementation of BackendJobFetcher for when the backend API trigger is left empty.
 * Returns the job if already stored locally (e.g. from FCM data payload) or fails gracefully.
 */
@Singleton
class StubBackendJobFetcher @Inject constructor(
    private val repository: SmsJobRepository,
) : BackendJobFetcher {

    override suspend fun fetchJob(jobId: String): Result<SmsJob> {
        val existing = repository.getJobById(jobId)
        return if (existing != null) {
            Timber.d("StubBackendJobFetcher: Found existing local job $jobId")
            Result.success(existing)
        } else {
            Timber.w("StubBackendJobFetcher: Backend API call is empty/stubbed. Job $jobId not found locally.")
            Result.failure(
                JobNotFoundException(
                    "Job $jobId details could not be fetched (Backend API is not yet configured). " +
                        "Include 'recipient' and 'message_body' in the FCM data payload for direct testing."
                )
            )
        }
    }
}

