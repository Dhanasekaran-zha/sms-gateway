# Architecture decisions

1. Kotlin Android app.
2. Jetpack Compose UI.
3. MVVM with repository boundaries.
4. Room as local job ledger.
5. WorkManager for durable deferred work, not real-time guarantees.
6. FCM as a wake-up signal containing only opaque job ID.
7. Backend remains authoritative for job details and final reconciliation.
8. SMS submission is performed only after explicit runtime permission.
9. Never promise exactly-once delivery; ambiguous submission must be reconciled.
10. Strict SMS job state machine: QUEUED -> FETCHED/SUBMITTING -> SUBMITTED/SUBMISSION_UNKNOWN/FAILED -> DELIVERED.
11. Prevent blind retries: SUBMISSION_UNKNOWN cannot transition back to SUBMITTING automatically; it must be reconciled to SUBMITTED, DELIVERED, or FAILED.
12. Every state change is recorded atomically in `status_history` via Room database transactions.
13. Duplicate job ID insertion is rejected (`DuplicateJobException`) to enforce local idempotency.
14. Explicit runtime request for `SEND_SMS` with clear user rationale card explaining that direct modem access is required.
15. Optional `READ_PHONE_STATE` for dual-SIM subscription queries via `SubscriptionManager`, with graceful fallback to default SIM.
16. Strict distinction between submission (`SUBMITTED`, accepted by cellular modem) and delivery (`DELIVERED`, handset delivery confirmation).
17. Dynamic calculation of SMS segments (GSM 7-bit vs UCS-2 Unicode) with intermediate part logging in `status_history`.
18. Backend API call trigger stubbed (`StubBackendJobFetcher`) per user request to enable immediate testing of the Firebase push trigger.
19. FCM data message handler (`GatewayFirebaseMessagingService`) supports both opaque `job_id` and direct data payload fields (`recipient`, `message_body`) for testing.
20. Idempotent WorkManager dispatch using `ExistingWorkPolicy.KEEP` and unique task name `sms_job_$jobId`.
21. `SmsSendWorker` guards against duplicate dispatches: skips jobs that are already in `SUBMITTED`, `DELIVERED`, `FAILED`, or `SUBMISSION_UNKNOWN` states.
22. Hilt `HiltWorkerFactory` configured via `Configuration.Provider` in `SmsGatewayApp` with default initializer disabled in `AndroidManifest.xml`.
