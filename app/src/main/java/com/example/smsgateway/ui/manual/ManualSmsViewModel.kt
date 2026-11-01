package com.example.smsgateway.ui.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsgateway.domain.model.JobSource
import com.example.smsgateway.domain.model.SmsJob
import com.example.smsgateway.domain.model.SmsJobStatus
import com.example.smsgateway.domain.repository.SmsJobRepository
import com.example.smsgateway.platform.sms.SimSubscriptionHelper
import com.example.smsgateway.platform.sms.SmsSender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ManualSmsViewModel @Inject constructor(
    private val repository: SmsJobRepository,
    private val smsSender: SmsSender,
    private val subscriptionHelper: SimSubscriptionHelper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManualSmsUiState())
    val uiState: StateFlow<ManualSmsUiState> = _uiState.asStateFlow()

    private var activeJobObserver: Job? = null
    private var activeHistoryObserver: Job? = null

    init {
        refreshPermissionsAndSubscriptions()
    }

    fun onRecipientChanged(newRecipient: String) {
        _uiState.update { it.copy(recipient = newRecipient, userNotice = null) }
    }

    fun onMessageBodyChanged(newBody: String) {
        _uiState.update { it.copy(messageBody = newBody, userNotice = null) }
    }

    fun onSubscriptionSelected(subscriptionId: Int?) {
        _uiState.update { it.copy(selectedSubscriptionId = subscriptionId) }
    }

    fun refreshPermissionsAndSubscriptions() {
        val hasSms = subscriptionHelper.hasSendSmsPermission()
        val hasPhoneState = subscriptionHelper.hasPhoneStatePermission()
        val subs = if (hasPhoneState) subscriptionHelper.getActiveSubscriptions() else emptyList()

        _uiState.update { state ->
            state.copy(
                hasSendSmsPermission = hasSms,
                hasPhoneStatePermission = hasPhoneState,
                availableSubscriptions = subs,
                selectedSubscriptionId = state.selectedSubscriptionId
                    ?: subs.firstOrNull()?.subscriptionId,
            )
        }
    }

    fun sendTestSms() {
        val state = _uiState.value
        if (!state.canSubmit) return

        val jobId = "manual-${UUID.randomUUID().toString().take(8)}"
        val job = SmsJob(
            jobId = jobId,
            recipient = state.recipient.trim(),
            messageBody = state.messageBody,
            subscriptionId = state.selectedSubscriptionId,
            status = SmsJobStatus.QUEUED,
            source = JobSource.MANUAL,
        )

        _uiState.update {
            it.copy(
                isSending = true,
                currentJob = job,
                userNotice = "Submitting SMS job...",
            )
        }

        observeJob(jobId)

        viewModelScope.launch {
            val createResult = repository.createJob(job)
            if (createResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        userNotice = "Failed to record job: ${createResult.exceptionOrNull()?.message}",
                    )
                }
                return@launch
            }

            val submitResult = smsSender.submitSms(
                jobId = jobId,
                recipient = job.recipient,
                messageBody = job.messageBody,
                subscriptionId = job.subscriptionId,
            )

            _uiState.update {
                it.copy(
                    isSending = false,
                    userNotice = if (submitResult.isSuccess) {
                        "SMS dispatched to carrier layer"
                    } else {
                        "Dispatch error: ${submitResult.exceptionOrNull()?.message}"
                    },
                )
            }
        }
    }

    private fun observeJob(jobId: String) {
        activeJobObserver?.cancel()
        activeHistoryObserver?.cancel()

        activeJobObserver = viewModelScope.launch {
            repository.observeJobById(jobId).collect { updatedJob ->
                _uiState.update { it.copy(currentJob = updatedJob) }
            }
        }

        activeHistoryObserver = viewModelScope.launch {
            repository.observeStatusHistory(jobId).collect { history ->
                _uiState.update { it.copy(currentJobHistory = history) }
            }
        }
    }
}

