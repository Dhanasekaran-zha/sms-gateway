package com.example.smsgateway.platform.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import com.example.smsgateway.domain.model.SmsJobStatus
import com.example.smsgateway.domain.repository.SmsJobRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SmsSentReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: SmsJobRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SmsResultConstants.ACTION_SMS_SENT) return

        val jobId = intent.getStringExtra(SmsResultConstants.EXTRA_JOB_ID) ?: return
        val partIndex = intent.getIntExtra(SmsResultConstants.EXTRA_PART_INDEX, 0)
        val totalParts = intent.getIntExtra(SmsResultConstants.EXTRA_TOTAL_PARTS, 1)
        val code = resultCode

        Timber.d("SmsSentReceiver: jobId=$jobId, part=${partIndex + 1}/$totalParts, resultCode=$code")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (code) {
                    Activity.RESULT_OK -> {
                        val detail = if (totalParts > 1) {
                            "Part ${partIndex + 1} of $totalParts submitted to cellular network"
                        } else {
                            "Submitted to cellular network"
                        }

                        // When the last part reports success, transition job to SUBMITTED
                        if (partIndex == totalParts - 1) {
                            repository.updateJobStatus(
                                jobId = jobId,
                                newStatus = SmsJobStatus.SUBMITTED,
                                detail = detail,
                            )
                        } else {
                            // Intermediate part status recorded via transition or history
                            Timber.i("Job $jobId: $detail")
                        }
                    }

                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                        recordFailure(jobId, "Generic failure (RESULT_ERROR_GENERIC_FAILURE)")
                    }

                    SmsManager.RESULT_ERROR_RADIO_OFF -> {
                        recordFailure(jobId, "Radio turned off / Airplane mode (RESULT_ERROR_RADIO_OFF)")
                    }

                    SmsManager.RESULT_ERROR_NULL_PDU -> {
                        recordFailure(jobId, "Null PDU (RESULT_ERROR_NULL_PDU)")
                    }

                    SmsManager.RESULT_ERROR_NO_SERVICE -> {
                        recordFailure(jobId, "No cellular service (RESULT_ERROR_NO_SERVICE)")
                    }

                    else -> {
                        // Ambiguous or vendor-specific code
                        Timber.w("Job $jobId returned ambiguous result code: $code")
                        repository.updateJobStatus(
                            jobId = jobId,
                            newStatus = SmsJobStatus.SUBMISSION_UNKNOWN,
                            detail = "Ambiguous submission result code: $code",
                            errorMessage = "Ambiguous result code $code",
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing SMS sent callback for job $jobId")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun recordFailure(jobId: String, reason: String) {
        Timber.e("SMS submission failed for job $jobId: $reason")
        repository.updateJobStatus(
            jobId = jobId,
            newStatus = SmsJobStatus.FAILED,
            detail = reason,
            errorMessage = reason,
        )
    }
}

