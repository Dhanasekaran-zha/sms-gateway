package com.example.smsgateway

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.smsgateway.domain.model.JobSource
import com.example.smsgateway.domain.model.SmsJob
import com.example.smsgateway.domain.model.SmsJobStatus
import com.example.smsgateway.domain.repository.SmsJobRepository
import com.example.smsgateway.navigation.AppNavigation
import com.example.smsgateway.platform.sms.SmsSender
import com.example.smsgateway.ui.theme.SmsGatewayTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: SmsJobRepository

    @Inject
    lateinit var smsSender: SmsSender

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleNotificationIntent(intent)
        enableEdgeToEdge()
        setContent {
            SmsGatewayTheme {
                AppNavigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val extras = intent?.extras ?: return
        val jobId = extras.getString("job_id") ?: extras.getString("jobId")
        val recipient = extras.getString("recipient") ?: extras.getString("phone")
        val messageBody = extras.getString("message_body") ?: extras.getString("message") ?: extras.getString("body")

        if (!recipient.isNullOrBlank() && !messageBody.isNullOrBlank()) {
            val resolvedJobId = jobId ?: "fcm-notif-${UUID.randomUUID().toString().take(8)}"
            Timber.i("MainActivity: Processing SMS trigger delivered via notification tap: $resolvedJobId")
            lifecycleScope.launch {
                val existing = repository.getJobById(resolvedJobId)
                if (existing == null) {
                    val job = SmsJob(
                        jobId = resolvedJobId,
                        recipient = recipient.trim(),
                        messageBody = messageBody,
                        status = SmsJobStatus.QUEUED,
                        source = JobSource.BACKEND_FCM,
                    )
                    repository.createJob(job)
                }
                smsSender.submitSms(
                    jobId = resolvedJobId,
                    recipient = recipient.trim(),
                    messageBody = messageBody,
                )
            }
        }
    }
}
