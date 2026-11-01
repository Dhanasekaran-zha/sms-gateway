package com.example.smsgateway.platform.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class SimSubscriptionInfo(
    val subscriptionId: Int,
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String,
)

@Singleton
open class SimSubscriptionHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    open fun hasSendSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    open fun hasPhoneStatePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE,
        ) == PackageManager.PERMISSION_GRANTED
    }

    open fun getActiveSubscriptions(): List<SimSubscriptionInfo> {
        if (!hasPhoneStatePermission()) {
            Timber.d("READ_PHONE_STATE permission not granted; cannot query active subscriptions")
            return emptyList()
        }

        return try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                as? SubscriptionManager ?: return emptyList()

            val infoList: List<SubscriptionInfo>? = subscriptionManager.activeSubscriptionInfoList
            infoList?.map { info ->
                SimSubscriptionInfo(
                    subscriptionId = info.subscriptionId,
                    slotIndex = info.simSlotIndex,
                    displayName = info.displayName?.toString() ?: "SIM ${info.simSlotIndex + 1}",
                    carrierName = info.carrierName?.toString() ?: "Carrier",
                )
            } ?: emptyList()
        } catch (e: SecurityException) {
            Timber.w(e, "SecurityException while querying subscriptions")
            emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error querying subscriptions")
            emptyList()
        }
    }
}
