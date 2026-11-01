# Progress log

Update this file after every phase.

## Current phase
Phase 5 — FCM and durable processing ✅ COMPLETE (Hardened for background & locked-screen reliability)

## Completed
- Starter kit created.
- Phase 0 — Repository audit: inspected environment, confirmed greenfield, documented risks.
- Phase 1 — Foundation: created full Android project scaffold with Compose, Navigation, Hilt, build verification.
- Phase 2 — Local job model: Room entities, DAO, database, repository, transaction validation, unit tests.
- Phase 3 — Manual SMS vertical slice: SmsManager, broadcast callbacks, SimSubscriptionHelper, ManualSmsScreen.
- Phase 4 — Backend integration: Deferred / stubbed per user request ("leave the api call trigger empty, i need to check the sms triggering only after the notification from firebase"). Created `BackendJobFetcher` interface and `StubBackendJobFetcher`.
- Phase 5 — FCM and durable processing:
  - Integrated `com.google.gms.google-services` plugin and real `google-services.json` from user's Firebase project (`sms-gateway-597c5`).
  - Added Firebase Cloud Messaging BOM (`33.7.0`) and WorkManager (`2.10.0`) with Hilt Worker integration (`1.2.0`).
  - Implemented `FcmTokenManager` for token retrieval and local persistence.
  - Implemented `GatewayFirebaseMessagingService` with partial `WakeLock` and immediate cellular modem submission on receipt.
  - Implemented `SmsSendWorker` (`@HiltWorker`) checking runtime permissions, anti-duplicate protection, and dispatching via `SmsSender`.
  - Added notification intent extra interception in `MainActivity` for when notifications are tapped from the system tray while minimized.
  - Added `WAKE_LOCK` and `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` in `AndroidManifest.xml` and battery optimization whitelist on device.
  - Verified background SMS submission on physical Motorola Moto G30 (`ZF6526ZJKN`) with `resultCode=-1` (`RESULT_OK`).

## Build/test status
✅ `testDebugUnitTest`: BUILD SUCCESSFUL (all unit tests passed)
✅ `assembleDebug`: BUILD SUCCESSFUL (debug APK generated)
✅ Real device deployment: Installed and active on Motorola Moto G30 (`ZF6526ZJKN`)

## Known risks
- Force-stopped apps do not receive FCM messages until manually opened by the user (standard Android platform behavior).
- Pure background execution without user tap requires sending FCM messages as **high-priority data messages** (or tapping notification if sent via Firebase Console).
- Real backend API endpoints remain to be hooked up when the user provides the contract.

## Next action
Phase 6 — Controlled end-to-end testing, allowlist verification, and duplicate push mitigation.
