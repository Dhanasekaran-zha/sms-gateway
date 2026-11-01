# Implementation plan

## Phase 0 — Repository audit
Inspect Android Studio project, Gradle, package name, min/target SDK and current build. Report findings; do not make broad changes.

## Phase 1 — Foundation
Set up Kotlin/Compose structure, navigation, theme, dependency injection if justified, build configuration and a basic dashboard. Must compile.

## Phase 2 — Local job model
Define SMS job/status models, Room entities/DAO/database, repository and status history. Add unit tests for legal state transitions.

## Phase 3 — Manual SMS vertical slice
Build a screen to enter a test recipient and message, request SEND_SMS permission, select SIM where supported, submit through SmsManager, and display submission result. Add sent callback and optional delivery callback. Test on a real device. Do not connect backend yet.

## Phase 4 — Backend integration
Wait for the user to provide the real API contract and auth design. Implement typed DTOs, Retrofit service, auth handling, timeouts, error mapping and idempotent job acknowledgement. Never guess endpoints.

## Phase 5 — FCM and durable processing
Receive an opaque job ID, authenticate and fetch job details, persist locally, enqueue unique work, check permission/SIM/network, submit SMS and report status. Respect ambiguous submission states; no blind retry.

## Phase 6 — Controlled end-to-end
Add a backend test job flow and a safe allowlist/test mode. Verify duplicate push handling and backend reconciliation.

## Phase 7 — Reliability UX
Show connection, permission, SIM, queue and last-sync states. Add retry only where safe, manual reconciliation for unknown submission, and clear diagnostics without sensitive content.

## Phase 8 — Hardening and release
Run unit/instrumentation tests, inspect manifest permissions, review logs/secrets, test real devices and document installation, limitations and recovery.
