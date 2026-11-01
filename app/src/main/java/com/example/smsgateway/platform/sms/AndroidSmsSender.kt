package com.example.smsgateway.platform.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import com.example.smsgateway.domain.model.SmsJobStatus
import com.example.smsgateway.domain.repository.SmsJobRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSmsSender @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subscriptionHelper: SimSubscriptionHelper,
    private val repository: SmsJobRepository,
) : SmsSender {

    private val requestCodeCounter = AtomicInteger(1000)

    override suspend fun submitSms(
        jobId: String,
        recipient: String,
        messageBody: String,
        subscriptionId: Int?,
    ): Result<Unit> {
        if (!subscriptionHelper.hasSendSmsPermission()) {
            val errorMsg = "SEND_SMS runtime permission is not granted"
            repository.updateJobStatus(
                jobId = jobId,
                newStatus = SmsJobStatus.FAILED,
                detail = errorMsg,
                errorMessage = errorMsg,
            )
            return Result.failure(SecurityException(errorMsg))
        }

        if (!SmsMessageValidator.isValidPhoneNumber(recipient)) {
            val errorMsg = "Invalid recipient phone number format"
            repository.updateJobStatus(
                jobId = jobId,
                newStatus = SmsJobStatus.FAILED,
                detail = errorMsg,
                errorMessage = errorMsg,
            )
            return Result.failure(IllegalArgumentException(errorMsg))
        }

        if (messageBody.isBlank()) {
            val errorMsg = "Message body cannot be empty"
            repository.updateJobStatus(
                jobId = jobId,
                newStatus = SmsJobStatus.FAILED,
                detail = errorMsg,
                errorMessage = errorMsg,
            )
            return Result.failure(IllegalArgumentException(errorMsg))
        }

        return try {
            // Persist transition to SUBMITTING before initiating the platform call
            val transitionResult = repository.updateJobStatus(
                jobId = jobId,
                newStatus = SmsJobStatus.SUBMITTING,
                detail = "Dispatching to SmsManager (subId: $subscriptionId)",
            )
            if (transitionResult.isFailure) {
                return Result.failure(
                    transitionResult.exceptionOrNull()
                        ?: IllegalStateException("Failed to transition job to SUBMITTING")
                )
            }

            val smsManager: SmsManager = getSmsManager(subscriptionId)
            val parts: ArrayList<String> = smsManager.divideMessage(messageBody)
            val totalParts = parts.size

            Timber.i("Submitting SMS for job $jobId: $totalParts part(s)")

            val sentIntents = ArrayList<PendingIntent>(totalParts)
            val deliveryIntents = ArrayList<PendingIntent>(totalParts)

            for (partIndex in 0 until totalParts) {
                val sentRequestCode = requestCodeCounter.incrementAndGet()
                val sentIntent = Intent(context, SmsSentReceiver::class.java).apply {
                    action = SmsResultConstants.ACTION_SMS_SENT
                    putExtra(SmsResultConstants.EXTRA_JOB_ID, jobId)
                    putExtra(SmsResultConstants.EXTRA_PART_INDEX, partIndex)
                    putExtra(SmsResultConstants.EXTRA_TOTAL_PARTS, totalParts)
                }
                sentIntents.add(
                    PendingIntent.getBroadcast(
                        context,
                        sentRequestCode,
                        sentIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                )

                val deliveryRequestCode = requestCodeCounter.incrementAndGet()
                val deliveryIntent = Intent(context, SmsDeliveredReceiver::class.java).apply {
                    action = SmsResultConstants.ACTION_SMS_DELIVERED
                    putExtra(SmsResultConstants.EXTRA_JOB_ID, jobId)
                    putExtra(SmsResultConstants.EXTRA_PART_INDEX, partIndex)
                    putExtra(SmsResultConstants.EXTRA_TOTAL_PARTS, totalParts)
                }
                deliveryIntents.add(
                    PendingIntent.getBroadcast(
                        context,
                        deliveryRequestCode,
                        deliveryIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                )
            }

            if (totalParts == 1) {
                smsManager.sendTextMessage(
                    recipient,
                    null,
                    parts[0],
                    sentIntents[0],
                    deliveryIntents[0],
                )
            } else {
                smsManager.sendMultipartTextMessage(
                    recipient,
                    null,
                    parts,
                    sentIntents,
                    deliveryIntents,
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Exception while submitting SMS for job $jobId")
            repository.updateJobStatus(
                jobId = jobId,
                newStatus = SmsJobStatus.FAILED,
                detail = "SmsManager exception: ${e.message}",
                errorMessage = e.message,
            )
            Result.failure(e)
        }
    }

    @Suppress("DEPRECATION")
    private fun getSmsManager(subscriptionId: Int?): SmsManager {
        val validSubId = subscriptionId != null &&
            subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val base = context.getSystemService(SmsManager::class.java)
            if (validSubId) base.createForSubscriptionId(subscriptionId!!) else base
        } else {
            if (validSubId) {
                SmsManager.getSmsManagerForSubscriptionId(subscriptionId!!)
            } else {
                SmsManager.getDefault()
            }
        }
    }
}

