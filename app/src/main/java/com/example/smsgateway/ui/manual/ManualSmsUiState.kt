package com.example.smsgateway.ui.manual

import com.example.smsgateway.domain.model.SmsJob
import com.example.smsgateway.domain.model.StatusHistoryEntry
import com.example.smsgateway.platform.sms.SimSubscriptionInfo
import com.example.smsgateway.platform.sms.SmsMessageValidator

data class ManualSmsUiState(
    val recipient: String = "",
    val messageBody: String = "",
    val hasSendSmsPermission: Boolean = false,
    val hasPhoneStatePermission: Boolean = false,
    val availableSubscriptions: List<SimSubscriptionInfo> = emptyList(),
    val selectedSubscriptionId: Int? = null,
    val isSending: Boolean = false,
    val currentJob: SmsJob? = null,
    val currentJobHistory: List<StatusHistoryEntry> = emptyList(),
    val userNotice: String? = null,
) {
    val partitionInfo: SmsMessageValidator.MessagePartition
        get() = SmsMessageValidator.calculatePartition(messageBody)

    val isRecipientValid: Boolean
        get() = SmsMessageValidator.isValidPhoneNumber(recipient)

    val canSubmit: Boolean
        get() = hasSendSmsPermission && isRecipientValid && messageBody.isNotBlank() && !isSending
}

