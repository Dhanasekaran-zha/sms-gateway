package com.example.smsgateway.platform.fcm

import android.content.Context
import android.os.PowerManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.smsgateway.domain.model.JobSource
import com.example.smsgateway.domain.model.SmsJob
import com.example.smsgateway.domain.model.SmsJobStatus
import com.example.smsgateway.domain.repository.SmsJobRepository
import com.example.smsgateway.platform.sms.SimSubscriptionHelper
import com.example.smsgateway.platform.sms.SmsSender
import com.example.smsgateway.platform.work.SmsSendWorker
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class GatewayFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var repository: SmsJobRepository

    @Inject
    lateinit var tokenManager: FcmTokenManager

    @Inject
    lateinit var smsSender: SmsSender

    @Inject
    lateinit var subscriptionHelper: SimSubscriptionHelper

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.i("GatewayFirebaseMessagingService: onNewToken received")
        tokenManager.saveToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        Timber.i("GatewayFirebaseMessagingService: FCM Message received while app state may be background/locked. Data keys: ${data.keys}")

        val jobId = data["job_id"] ?: data["jobId"] ?: "fcm-${UUID.randomUUID().toString().take(8)}"
        val recipient = data["recipient"] ?: data["phone"]
        val messageBody = data["message_body"] ?: data["message"] ?: data["body"]
        val subscriptionId = data["subscription_id"]?.toIntOrNull()

        // Acquire a partial wake lock so CPU does not sleep before modem submission finishes
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SmsGateway:FcmDispatchWakeLock",
        )?.apply {
            acquire(45_000L) // 45 seconds max
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // If message body and recipient are included, persist job
                if (!recipient.isNullOrBlank() && !messageBody.isNullOrBlank()) {
                    val job = SmsJob(
                        jobId = jobId,
                        recipient = recipient.trim(),
                        messageBody = messageBody,
                        subscriptionId = subscriptionId,
                        status = SmsJobStatus.QUEUED,
                        source = JobSource.BACKEND_FCM,
                    )
                    val result = repository.createJob(job)
                    if (result.isSuccess) {
                        Timber.i("Persisted FCM SMS job $jobId locally")
                    } else {
                        Timber.w("Job $jobId was already created or failed to persist: ${result.exceptionOrNull()?.message}")
                    }

                    // Immediate dispatch: execute directly while wake lock is held
                    if (subscriptionHelper.hasSendSmsPermission()) {
                        Timber.i("Immediate dispatch: submitting SMS directly for job $jobId while in background")
                        smsSender.submitSms(
                            jobId = jobId,
                            recipient = recipient.trim(),
                            messageBody = messageBody,
                            subscriptionId = subscriptionId,
                        )
                    } else {
                        Timber.e("Immediate dispatch aborted: SEND_SMS permission missing for job $jobId")
                    }
                }

                // Schedule durable WorkManager execution as fallback
                val workRequest = OneTimeWorkRequestBuilder<SmsSendWorker>()
                    .setInputData(workDataOf(SmsSendWorker.KEY_JOB_ID to jobId))
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()

                WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                    "sms_job_$jobId",
                    ExistingWorkPolicy.KEEP,
                    workRequest,
                )

                Timber.d("Enqueued WorkManager fallback task for job $jobId")
            } catch (e: Exception) {
                Timber.e(e, "Error handling FCM message for job $jobId")
            } finally {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Error releasing wake lock")
                }
            }
        }
    }
}

