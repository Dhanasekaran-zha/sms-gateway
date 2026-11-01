package com.example.smsgateway.platform.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smsgateway.domain.backend.BackendJobFetcher
import com.example.smsgateway.domain.model.SmsJobStatus
import com.example.smsgateway.domain.repository.SmsJobRepository
import com.example.smsgateway.platform.sms.SimSubscriptionHelper
import com.example.smsgateway.platform.sms.SmsSender
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class SmsSendWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val repository: SmsJobRepository,
    private val smsSender: SmsSender,
    private val subscriptionHelper: SimSubscriptionHelper,
    private val backendFetcher: BackendJobFetcher,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val jobId = inputData.getString(KEY_JOB_ID)
        if (jobId.isNullOrBlank()) {
            Timber.e("SmsSendWorker: Missing KEY_JOB_ID in input data")
            return Result.failure()
        }

        Timber.i("SmsSendWorker: Processing SMS job $jobId (attempt: $runAttemptCount)")

        var job = repository.getJobById(jobId)

        // If job details are not yet in local database, fetch them
        if (job == null) {
            val fetchResult = backendFetcher.fetchJob(jobId)
            if (fetchResult.isFailure) {
                Timber.e(fetchResult.exceptionOrNull(), "SmsSendWorker: Could not fetch details for job $jobId")
                return Result.failure()
            }
            job = fetchResult.getOrNull()
            if (job != null) {
                repository.createJob(job)
            }
        }

        if (job == null) {
            Timber.e("SmsSendWorker: Job $jobId is null after fetch")
            return Result.failure()
        }

        // Do not resend or retry if already submitted, delivered, failed, or submission status is unknown
        if (job.status == SmsJobStatus.SUBMITTED ||
            job.status == SmsJobStatus.DELIVERED ||
            job.status == SmsJobStatus.FAILED ||
            job.status == SmsJobStatus.SUBMISSION_UNKNOWN
        ) {
            Timber.w("SmsSendWorker: Skipping execution for job $jobId with non-actionable status: ${job.status}")
            return Result.success()
        }

        // Verify runtime SEND_SMS permission
        if (!subscriptionHelper.hasSendSmsPermission()) {
            val error = "SEND_SMS permission is not granted on device at execution time"
            Timber.e("SmsSendWorker: $error")
            repository.updateJobStatus(
                jobId = jobId,
                newStatus = SmsJobStatus.FAILED,
                detail = error,
                errorMessage = error,
            )
            return Result.failure()
        }

        // Submit SMS via SmsSender
        val submitResult = smsSender.submitSms(
            jobId = jobId,
            recipient = job.recipient,
            messageBody = job.messageBody,
            subscriptionId = job.subscriptionId,
        )

        return if (submitResult.isSuccess) {
            Timber.i("SmsSendWorker: Successfully initiated SMS submission for job $jobId")
            Result.success()
        } else {
            Timber.e(submitResult.exceptionOrNull(), "SmsSendWorker: Submission initiation failed for job $jobId")
            Result.failure()
        }
    }

    companion object {
        const val KEY_JOB_ID = "key_job_id"
    }
}

