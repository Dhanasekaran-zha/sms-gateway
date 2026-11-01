package com.example.smsgateway.ui.manual

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smsgateway.domain.model.SmsJob
import com.example.smsgateway.domain.model.SmsJobStatus
import com.example.smsgateway.domain.model.StatusHistoryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSmsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManualSmsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ -> viewModel.refreshPermissionsAndSubscriptions() },
    )

    val phoneStatePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ -> viewModel.refreshPermissionsAndSubscriptions() },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manual Test SMS") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Permission Banners
            if (!uiState.hasSendSmsPermission) {
                item {
                    PermissionCard(
                        title = "SEND_SMS Permission Required",
                        rationale = "Android requires explicit runtime permission to submit SMS through your cellular connection. This app does not bypass security or use hidden APIs.",
                        buttonText = "Grant SMS Permission",
                        onRequestPermission = {
                            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                        },
                    )
                }
            }

            if (!uiState.hasPhoneStatePermission) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Optional: Dual-SIM Detection",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "To select a specific SIM slot on dual-SIM devices, READ_PHONE_STATE permission is required.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                            )
                            OutlinedButton(
                                onClick = {
                                    phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                                },
                            ) {
                                Text("Allow Dual-SIM Detection")
                            }
                        }
                    }
                }
            }

            // SIM Selection Chips
            if (uiState.availableSubscriptions.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            text = "Select SIM Card",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 6.dp),
                        ) {
                            uiState.availableSubscriptions.forEach { sub ->
                                FilterChip(
                                    selected = uiState.selectedSubscriptionId == sub.subscriptionId,
                                    onClick = { viewModel.onSubscriptionSelected(sub.subscriptionId) },
                                    label = {
                                        Text("${sub.displayName} (${sub.carrierName})")
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Recipient Input
            item {
                OutlinedTextField(
                    value = uiState.recipient,
                    onValueChange = viewModel::onRecipientChanged,
                    label = { Text("Recipient Phone Number") },
                    placeholder = { Text("+919876543210") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.recipient.isNotEmpty() && !uiState.isRecipientValid,
                    supportingText = {
                        if (uiState.recipient.isNotEmpty() && !uiState.isRecipientValid) {
                            Text("Use standard national or international format (+91...)")
                        }
                    },
                )
            }

            // Message Body Input
            item {
                val partInfo = uiState.partitionInfo
                OutlinedTextField(
                    value = uiState.messageBody,
                    onValueChange = viewModel::onMessageBodyChanged,
                    label = { Text("SMS Message Content") },
                    placeholder = { Text("Enter test message body...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    supportingText = {
                        val encoding = if (partInfo.isUnicode) "Unicode (UCS-2)" else "GSM-7"
                        Text(
                            text = "${uiState.messageBody.length} chars | ${partInfo.partsCount} part(s) ($encoding)",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                )
            }

            // Submit Button
            item {
                Button(
                    onClick = viewModel::sendTestSms,
                    enabled = uiState.canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Submitting to Modem...")
                    } else {
                        Text("Submit Test SMS via SIM", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Feedback notice
            if (uiState.userNotice != null) {
                item {
                    Text(
                        text = uiState.userNotice ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Active Job Card
            if (uiState.currentJob != null) {
                item {
                    JobResultCard(
                        job = uiState.currentJob!!,
                        history = uiState.currentJobHistory,
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    rationale: String,
    buttonText: String,
    onRequestPermission: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = rationale, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun JobResultCard(
    job: SmsJob,
    history: List<StatusHistoryEntry>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Job: ${job.jobId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                )
                StatusBadge(status = job.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Semantic status explanation
            Text(
                text = when (job.status) {
                    SmsJobStatus.QUEUED -> "Job queued locally, awaiting dispatch."
                    SmsJobStatus.FETCHED -> "Job fetched and ready for submission."
                    SmsJobStatus.SUBMITTING -> "Dispatching to device cellular modem..."
                    SmsJobStatus.SUBMITTED -> "Cellular modem accepted SMS (RESULT_OK). Not proof of handset delivery."
                    SmsJobStatus.DELIVERED -> "Handset confirmed delivery via carrier network."
                    SmsJobStatus.FAILED -> "Submission failed: ${job.errorMessage ?: "Unknown error"}"
                    SmsJobStatus.SUBMISSION_UNKNOWN -> "Ambiguous submission state. Do not retry blindly; verify status before resending."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (history.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(
                    text = "Audit Trail",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
                history.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${timeFormat.format(Date(entry.timestamp))} → ${entry.toStatus}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                        if (entry.detail != null) {
                            Text(
                                text = entry.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: SmsJobStatus) {
    val (bgColor, textColor) = when (status) {
        SmsJobStatus.QUEUED -> Color(0xFFE0E0E0) to Color(0xFF424242)
        SmsJobStatus.FETCHED -> Color(0xFFBBDEFB) to Color(0xFF0D47A1)
        SmsJobStatus.SUBMITTING -> Color(0xFFFFF9C4) to Color(0xFFF57F17)
        SmsJobStatus.SUBMITTED -> Color(0xFFC8E6C9) to Color(0xFF1B5E20)
        SmsJobStatus.DELIVERED -> Color(0xFF81C784) to Color(0xFF003300)
        SmsJobStatus.FAILED -> Color(0xFFFFCDD2) to Color(0xFFB71C1C)
        SmsJobStatus.SUBMISSION_UNKNOWN -> Color(0xFFE1BEE7) to Color(0xFF4A148C)
    }

    Box(
        modifier = Modifier
            .background(color = bgColor, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = status.name,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

