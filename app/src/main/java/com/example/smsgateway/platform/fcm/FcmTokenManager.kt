package com.example.smsgateway.platform.fcm

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmTokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val prefs = context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
    private val _fcmToken = MutableStateFlow(prefs.getString(KEY_FCM_TOKEN, null))
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    init {
        retrieveToken()
    }

    fun retrieveToken() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    saveToken(token)
                    Timber.d("FCM Registration Token retrieved: $token")
                } else {
                    Timber.w(task.exception, "Fetching FCM registration token failed")
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "FirebaseMessaging not initialized or missing google-services config")
        }
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
        _fcmToken.value = token
    }

    companion object {
        private const val KEY_FCM_TOKEN = "key_fcm_token"
    }
}

