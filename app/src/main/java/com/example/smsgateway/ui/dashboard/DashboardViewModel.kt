package com.example.smsgateway.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsgateway.domain.model.SmsJob
import com.example.smsgateway.domain.repository.SmsJobRepository
import com.example.smsgateway.platform.fcm.FcmTokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val statusMessage: String = "SMS Gateway Active",
    val jobs: List<SmsJob> = emptyList(),
    val fcmToken: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: SmsJobRepository,
    private val fcmTokenManager: FcmTokenManager,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observeAllJobs(),
        fcmTokenManager.fcmToken,
    ) { jobs, token ->
        DashboardUiState(
            statusMessage = if (jobs.isEmpty()) {
                "SMS Gateway Active — No jobs yet"
            } else {
                "SMS Gateway Active (${jobs.size} jobs recorded)"
            },
            jobs = jobs,
            fcmToken = token,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(),
    )

    fun refreshFcmToken() {
        fcmTokenManager.retrieveToken()
    }
}
