# Android Private SMS Gateway

A robust, private Android SMS Gateway application built with modern Android best practices: **Kotlin**, **Jetpack Compose (Material 3)**, **MVVM**, **Room**, **Hilt**, **WorkManager**, and **Firebase Cloud Messaging (FCM)**.

This gateway enables your backend services or automated triggers to dispatch single-part and multipart SMS messages directly through an Android device's cellular SIM card, with strict state tracking, anti-duplicate protection, and delivery callback reporting.

---

## Table of Contents
1. [Core Features](#core-features)
2. [Architecture & Job Lifecycle](#architecture--job-lifecycle)
3. [Prerequisites & Project Setup](#prerequisites--project-setup)
4. [Permissions & Device Configuration](#permissions--device-configuration)
5. [How to Use the Gateway](#how-to-use-the-gateway)
   - [Method 1: Manual Test Screen](#method-1-manual-test-screen)
   - [Method 2: Background Trigger via Firebase (FCM Data Message)](#method-2-background-trigger-via-firebase-fcm-data-message)
   - [Method 3: Firebase Console Notification with Custom Data](#method-3-firebase-console-notification-with-custom-data)
6. [How to Make the API Call After SMS is Delivered](#how-to-make-the-api-call-after-sms-is-delivered)
   - [Understanding Delivery Callbacks](#understanding-delivery-callbacks)
   - [Step-by-Step Callback Implementation](#step-by-step-callback-implementation)
   - [Recommended Retrofit Callback Service](#recommended-retrofit-callback-service)
   - [Connecting the Callback in `SmsDeliveredReceiver`](#connecting-the-callback-in-smsdeliveredreceiver)
   - [Handling Offline Scenarios with WorkManager](#handling-offline-scenarios-with-workmanager)
7. [Testing and Verification](#testing-and-verification)
8. [Compliance & Legal Notices](#compliance--legal-notices)

---

## Core Features

- **Direct Cellular Radio Integration**: Sends SMS via `SmsManager` with sent and delivery `PendingIntent`s.
- **Background & Locked-Screen Execution**: Uses partial `WakeLock` and battery optimization exemptions so triggers execute reliably even when the phone is locked, asleep, or the app is minimized.
- **Strict State Machine Ledger**: Local Room database ledger tracking transitions:
  `QUEUED` → `FETCHED` → `SUBMITTING` → `SUBMITTED` → `DELIVERED` (or `FAILED` / `SUBMISSION_UNKNOWN`).
- **Anti-Duplicate Dispatch Protection**: Unique WorkManager idempotency keys (`sms_job_<jobId>`) and state-level deduplication prevent duplicate SMS dispatch.
- **Dual-SIM Support**: Inspects device subscriptions via `SubscriptionManager` to allow selecting specific SIM slots.
- **Smart SMS Partitioning**: Automatically calculates and segments GSM 7-bit (160/153 chars) and UCS-2 Unicode (70/67 chars) messages.
- **Privacy-First Design**: Masked phone numbers in UI; no plain-text OTPs or secret credentials logged.

---

## Architecture & Job Lifecycle

### State Machine Flow

```
                      ┌──────────────┐
                      │    QUEUED    │
                      └──────┬───────┘
                             │
                      ┌──────▼───────┐
                      │   FETCHED    │ (If payload retrieved from backend)
                      └──────┬───────┘
                             │
                      ┌──────▼───────┐
                      │  SUBMITTING  │ (Persisted prior to SmsManager call)
                      └──────┬───────┘
                             │
       ┌─────────────────────┼─────────────────────┐
       │ (RESULT_OK)         │ (Modem error)       │ (Timeout / Unknown code)
┌──────▼───────┐      ┌──────▼───────┐      ┌──────▼──────────────┐
│  SUBMITTED   │      │    FAILED    │      │  SUBMISSION_UNKNOWN │
└──────┬───────┘      └──────────────┘      └──────────┬──────────┘
       │                                               │
       │ (Delivery report PDU)                         │ (Manual/Backend reconcile)
┌──────▼───────┐                                ┌──────▼──────┐
│  DELIVERED   │                                │  RECONCILED │
└──────────────┘                                └─────────────┘
```

> **Important**: `SUBMITTED` means the local cellular modem accepted the SMS and handed it to the carrier's SMSC. It is **not** proof of handset delivery. `DELIVERED` is recorded only when the carrier returns an explicit handset delivery report.

---

## Prerequisites & Project Setup

### Requirements
- **JDK 17+** (JDK 20 supported)
- **Android SDK** API 26 (minSdk) to API 35 (compileSdk/targetSdk)
- **Android Device** with an active SIM card and SMS plan
- **Firebase Project** for FCM push triggers

### 1. Clone & Open
Open the repository root in Android Studio or Google Antigravity.

### 2. Configure Firebase
1. Create or open a project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with package name: `com.example.smsgateway`.
3. Download `google-services.json` and place it in the `app/` folder:
   ```text
   sms_gateway_android/app/google-services.json
   ```

### 3. Build & Install
Run the Gradle wrapper:
```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Permissions & Device Configuration

### Required Permissions
The app declares the following permissions in `AndroidManifest.xml`:
- `android.permission.SEND_SMS` (Runtime permission: required to send SMS).
- `android.permission.READ_PHONE_STATE` (Runtime permission: optional, used to detect SIM slots on dual-SIM devices).
- `android.permission.WAKE_LOCK` (Keeps the CPU awake during background push processing).
- `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (Prevents Android Doze mode from sleeping the app).

### Critical: Battery Optimization Exemption
For the gateway to trigger while the phone is locked or minimized:
1. Open the app on the phone.
2. Under **Background & Lock Screen Reliability**, tap **"Allow Unrestricted Background"**.
3. Or run via ADB:
   ```bash
   adb shell dumpsys deviceidle whitelist +com.example.smsgateway
   ```

---

## How to Use the Gateway

### Method 1: Manual Test Screen
1. Launch the app and tap **"Open Manual Test SMS Screen"**.
2. Grant `SEND_SMS` permission when prompted.
3. If on a dual-SIM phone, tap **"Allow Dual-SIM Detection"** to pick SIM 1 or SIM 2.
4. Enter the recipient phone number (e.g. `+919876543210`) and message text.
5. Tap **"Submit Test SMS via SIM"**.
6. View real-time status transitions and audit logs on the screen.

---

### Method 2: Background Trigger via Firebase (FCM Data Message)
This is the recommended method for backend automation. A high-priority **data-only** message wakes up the device silently, acquires a `WakeLock`, and transmits the SMS immediately.

#### Payload Specification
Send a POST request to:
`https://fcm.googleapis.com/v1/projects/<YOUR_PROJECT_ID>/messages:send`

```json
{
  "message": {
    "token": "<DEVICE_FCM_TOKEN>",
    "data": {
      "job_id": "job-unique-12345",
      "recipient": "+919876543210",
      "message_body": "Your verification code is 482910."
    },
    "android": {
      "priority": "high"
    }
  }
}
```

> **Notice**: Do **not** include a `"notification"` key in the payload. Omitting `"notification"` ensures Android delivers the message straight to `GatewayFirebaseMessagingService.onMessageReceived()` in the background without requiring user interaction.

#### Python Trigger Script
Create a script (e.g. `send_fcm.py`) using your Firebase Service Account JSON:

```python
import json
import time
import urllib.request
from google.oauth2 import service_account
import google.auth.transport.requests

SERVICE_ACCOUNT_FILE = "service-account.json"
DEVICE_FCM_TOKEN = "<PASTE_TOKEN_FROM_APP_DASHBOARD>"
RECIPIENT_PHONE = "+919876543210"
MESSAGE = "Automated test SMS triggered via FCM API."

# 1. Get OAuth2 Access Token
scopes = ["https://www.googleapis.com/auth/firebase.messaging"]
creds = service_account.Credentials.from_service_account_file(SERVICE_ACCOUNT_FILE, scopes=scopes)
creds.refresh(google.auth.transport.requests.Request())

with open(SERVICE_ACCOUNT_FILE) as f:
    project_id = json.load(f)["project_id"]

url = f"https://fcm.googleapis.com/v1/projects/{project_id}/messages:send"

# 2. Build High-Priority Data Payload
payload = {
    "message": {
        "token": DEVICE_FCM_TOKEN,
        "data": {
            "job_id": f"job-{int(time.time())}",
            "recipient": RECIPIENT_PHONE,
            "message_body": MESSAGE
        },
        "android": {
            "priority": "high"
        }
    }
}

req = urllib.request.Request(
    url,
    data=json.dumps(payload).encode("utf-8"),
    headers={
        "Authorization": f"Bearer {creds.token}",
        "Content-Type": "application/json"
    }
)

with urllib.request.urlopen(req) as resp:
    print("FCM Response:", resp.read().decode("utf-8"))
```

---

### Method 3: Firebase Console Notification with Custom Data
If triggering from the Firebase Console UI:
1. Go to **Firebase Console → Cloud Messaging → New Campaign → Firebase Notification messages**.
2. Enter Title and Text (e.g. Title: `SMS Job`, Text: `Triggering SMS`).
3. Under **Target**, click **"Send test message"** and enter your Device FCM Token.
4. Under **Additional options → Custom data**, add:
   - `job_id`: `test-fcm-001`
   - `recipient`: `+919876543210`
   - `message_body`: `Test message body`
5. Send the test message. If the app is minimized or the screen is locked, tap the notification in your status bar to trigger the dispatch.

---

## How to Make the API Call After SMS is Delivered

### Understanding Delivery Callbacks
In Android SMS architecture, two separate broadcast callbacks occur:
1. **Sent Callback (`SmsSentReceiver`)**:
   Fired by the local modem when the SMS is accepted by the carrier network (`Activity.RESULT_OK`). The job status transitions to `SUBMITTED`.
2. **Delivered Callback (`SmsDeliveredReceiver`)**:
   Fired when the recipient handset receives the message and the carrier returns a Delivery PDU (`Activity.RESULT_OK`). The job status transitions to `DELIVERED`.

---

### Step-by-Step Callback Implementation

To report the delivery confirmation back to your server:

```
┌────────────────────────┐
│ Carrier Cellular Tower │
└───────────┬────────────┘
            │ Delivery Report PDU
┌───────────▼────────────┐
│   Android SMS Stack    │
└───────────┬────────────┘
            │ PendingIntent Broadcast (SMS_DELIVERED)
┌───────────▼────────────┐
│  SmsDeliveredReceiver  │──► 1. Update Room DB (DELIVERED)
└───────────┬────────────┘
            │
            ▼ 2. Enqueue WorkManager / Network Callback
┌────────────────────────┐
│ Backend Status Reporter│──► 3. HTTPS POST /api/v1/jobs/{jobId}/status
└────────────────────────┘
```

---

### 1. Define the Backend Callback Contract

Your backend should expose a status acknowledgement endpoint. Example:

- **Method**: `POST`
- **URL**: `https://api.yourdomain.com/api/v1/sms/status-callback`
- **Headers**:
  ```http
  Authorization: Bearer <GATEWAY_DEVICE_SECRET>
  Content-Type: application/json
  ```
- **Request Body**:
  ```json
  {
    "job_id": "job-unique-12345",
    "status": "DELIVERED",
    "timestamp": 1730462000000,
    "error_message": null,
    "parts_count": 1,
    "source": "BACKEND_FCM"
  }
  ```
- **Expected Response**: `200 OK` with `{"acknowledged": true}`.

---

### 2. Create the Callback DTO and Retrofit Service

Create `app/src/main/java/com/example/smsgateway/data/remote/StatusCallbackApi.kt`:

```kotlin
package com.example.smsgateway.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class JobStatusReportRequest(
    val jobId: String,
    val status: String,
    val timestamp: Long,
    val errorMessage: String? = null,
)

data class JobStatusReportResponse(
    val acknowledged: Boolean,
)

interface StatusCallbackApi {
    @POST("api/v1/sms/status-callback")
    suspend fun reportJobStatus(
        @Header("Authorization") authToken: String,
        @Body report: JobStatusReportRequest,
    ): Response<JobStatusReportResponse>
}
```

---

### 3. Implement the Status Reporter

Create a repository or service component:

```kotlin
package com.example.smsgateway.data.remote

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeliveryStatusReporter @Inject constructor(
    private val callbackApi: StatusCallbackApi,
) {
    suspend fun reportStatus(
        jobId: String,
        status: String,
        errorMessage: String? = null,
    ): Result<Unit> {
        return try {
            val response = callbackApi.reportJobStatus(
                authToken = "Bearer YOUR_GATEWAY_TOKEN",
                report = JobStatusReportRequest(
                    jobId = jobId,
                    status = status,
                    timestamp = System.currentTimeMillis(),
                    errorMessage = errorMessage,
                ),
            )
            if (response.isSuccessful && response.body()?.acknowledged == true) {
                Timber.i("Successfully acknowledged delivery for job $jobId to backend")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Server returned code ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to send status callback for job $jobId")
            Result.failure(e)
        }
    }
}
```

---

### 4. Hook the Reporter into `SmsDeliveredReceiver`

In [`app/src/main/java/com/example/smsgateway/platform/sms/SmsDeliveredReceiver.kt`](file:///c:/Users/dhana/Desktop/Workspace/Antigravity_Projects/sms_gateway_android/app/src/main/java/com/example/smsgateway/platform/sms/SmsDeliveredReceiver.kt):

```kotlin
@AndroidEntryPoint
class SmsDeliveredReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: SmsJobRepository

    @Inject
    lateinit var statusReporter: DeliveryStatusReporter // <-- Inject your reporter

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SmsResultConstants.ACTION_SMS_DELIVERED) return

        val jobId = intent.getStringExtra(SmsResultConstants.EXTRA_JOB_ID) ?: return
        val code = resultCode
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (code == Activity.RESULT_OK) {
                    // 1. Update local database ledger
                    repository.updateJobStatus(
                        jobId = jobId,
                        newStatus = SmsJobStatus.DELIVERED,
                        detail = "Confirmed delivered by recipient handset",
                    )

                    // 2. Make authenticated API call back to your server
                    val callbackResult = statusReporter.reportStatus(
                        jobId = jobId,
                        status = "DELIVERED",
                    )

                    // 3. Fallback to WorkManager if phone is temporarily offline
                    if (callbackResult.isFailure) {
                        scheduleDurableStatusRetry(context, jobId, "DELIVERED")
                    }
                } else {
                    repository.updateJobStatus(
                        jobId = jobId,
                        newStatus = SmsJobStatus.FAILED,
                        detail = "Carrier reported delivery failure code $code",
                    )
                    statusReporter.reportStatus(
                        jobId = jobId,
                        status = "FAILED",
                        errorMessage = "Delivery failure code $code",
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
```

---

### Handling Offline Scenarios with WorkManager
If the device loses Wi-Fi/mobile data at the exact moment the SMS delivery PDU arrives, direct HTTP calls will fail.

To ensure **guaranteed status delivery** to your server:
1. Enqueue a WorkManager job (`StatusSyncWorker`) with `NetworkType.CONNECTED` constraint.
2. WorkManager will retry with exponential backoff until the server acknowledges the status report.

```kotlin
fun scheduleDurableStatusRetry(context: Context, jobId: String, status: String) {
    val request = OneTimeWorkRequestBuilder<StatusSyncWorker>()
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .setInputData(
            workDataOf(
                "job_id" to jobId,
                "status" to status
            )
        )
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        "sync_status_$jobId",
        ExistingWorkPolicy.REPLACE,
        request
    )
}
```

---

## Testing and Verification

Run the automated test suite locally on JVM (no emulator required):

```bash
# Run all unit tests (Room, state transitions, WorkManager, validators)
./gradlew testDebugUnitTest

# Assemble and verify APK build
./gradlew assembleDebug
```

### Verified Test Cases:
- **`SmsJobStatusTransitionTest`**: Verifies valid paths, terminal state lockouts, and anti-blind-retry rules.
- **`SmsJobDaoTest`**: In-memory SQLite tests for cascade deletion, duplicate constraints, and reactive queries.
- **`SmsJobRepositoryTest`**: Atomic transaction checks for status progression and history audit logs.
- **`SmsSendWorkerTest`**: WorkManager execution, runtime permission verification, and duplicate-dispatch abortion.
- **`SmsMessageValidatorTest`**: Phone formatting, ASCII vs Unicode, and GSM-7 vs UCS-2 length calculations.

---

## Compliance & Legal Notices

1. **Explicit Permission Required**: This application requires runtime `SEND_SMS`. It does not bypass Android platform security, abuse Accessibility services, or use private/hidden APIs.
2. **Indian Regulatory Notice (TRAI / DLT)**: In India, commercial, transactional, or promotional SMS traffic is strictly regulated under TRAI regulations. Commercial bulk messaging or OTP delivery over personal SIM bundles can lead to SIM blocking by telecom operators. Ensure appropriate operator approval and registration for commercial gateway usage.
3. **No Guarantee of Exactly-Once Delivery**: Cellular networks and SMS protocols are inherently store-and-forward. Network timeouts and ambiguous states (`SUBMISSION_UNKNOWN`) must always be reconciled rather than blindly retried to avoid sending duplicate messages to recipients.
