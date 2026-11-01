# Architecture rules
- UI: Jetpack Compose, state-driven screens, ViewModels.
- Domain: job state and use cases independent of Android UI.
- Data: Room local ledger, Retrofit backend client, FCM receiver, SMS platform adapter.
- Keep Android framework calls behind interfaces where practical.
- Persist job state transitions before and after side effects.
- Use unique work names / idempotency keys to prevent duplicate local scheduling.
- Do not place secrets or message content in notification extras or logs.
