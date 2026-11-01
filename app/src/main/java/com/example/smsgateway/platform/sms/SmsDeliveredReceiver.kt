package com.example.smsgateway.platform.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.smsgateway.domain.model.SmsJobStatus
import com.example.smsgateway.domain.repository.SmsJobRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SmsDeliveredReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: SmsJobRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SmsResultConstants.ACTION_SMS_DELIVERED) return

        val jobId = intent.getStringExtra(SmsResultConstants.EXTRA_JOB_ID) ?: return
        val partIndex = intent.getIntExtra(SmsResultConstants.EXTRA_PART_INDEX, 0)
        val totalParts = intent.getIntExtra(SmsResultConstants.EXTRA_TOTAL_PARTS, 1)
        val code = resultCode

        Timber.d("SmsDeliveredReceiver: jobId=$jobId, part=${partIndex + 1}/$totalParts, resultCode=$code")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (code == Activity.RESULT_OK) {
                    val detail = if (totalParts > 1) {
                        "Part ${partIndex + 1} of $totalParts confirmed delivered to handset"
                    } else {
                        "Delivery confirmed by recipient handset"
                    }

                    if (partIndex == totalParts - 1) {
                        repository.updateJobStatus(
                            jobId = jobId,
                            newStatus = SmsJobStatus.DELIVERED,
                            detail = detail,
                        )
                    }
                } else {
                    val detail = "Delivery report indicated failure (code: $code)"
                    repository.updateJobStatus(
                        jobId = jobId,
                        newStatus = SmsJobStatus.FAILED,
                        detail = detail,
                        errorMessage = detail,
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing SMS delivery callback for job $jobId")
            } finally {
                pendingResult.finish()
            }
        }
    }
}

