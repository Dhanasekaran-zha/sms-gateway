# Project-wide instructions

You are building a private Android SMS gateway. Use Kotlin, Jetpack Compose, MVVM, repository pattern, Room, WorkManager, Firebase Cloud Messaging and Retrofit where appropriate.

## Non-negotiable rules
- First inspect the existing repository. Do not overwrite working code or invent project state.
- Work in small, compiling phases. Run Gradle build and tests after meaningful changes.
- Keep architecture simple and maintainable. Use clear package boundaries.
- Never invent backend URLs, request/response fields, auth schemes or Firebase configuration. Ask for missing contracts.
- Never hardcode API keys, service-account credentials, tokens or passwords.
- FCM payloads must contain only an opaque job ID and minimal routing metadata. Fetch message content from the backend over authenticated HTTPS.
- Require Android runtime `SEND_SMS` permission and explain why it is needed. Do not bypass permissions, use root, accessibility abuse or hidden APIs.
- SMS submission is not proof of delivery. Track states such as QUEUED, FETCHED, SUBMITTING, SUBMITTED, DELIVERED (only when a delivery report exists), FAILED and SUBMISSION_UNKNOWN.
- Avoid blind retries after an ambiguous submission. Duplicate SMS is a serious failure mode.
- Handle permission denial, no SIM, dual-SIM selection, offline state, FCM delays, app process death and backend timeouts.
- WorkManager is for durable deferred work, not guaranteed instant execution. FCM is best effort. Force-stopped apps generally require the user to launch them again.
- Do not claim exactly-once SMS delivery. Document platform and carrier limitations.
- Keep user-facing status accurate and avoid logging message bodies, OTPs, phone numbers or auth tokens.

## Agent workflow
Before each phase: read `docs/IMPLEMENTATION_PLAN.md`, `docs/DECISIONS.md` and `docs/PROGRESS.md`.
After each phase: build, test, update `docs/PROGRESS.md`, record decisions and list remaining risks.
