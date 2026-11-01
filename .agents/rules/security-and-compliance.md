# Security and compliance
- Use HTTPS and authenticated backend calls.
- Store tokens using Android Keystore-backed storage where feasible.
- FCM message: opaque job ID only; fetch actual recipient/body from backend after auth.
- Minimize local retention; protect database and avoid sensitive logs.
- Ask for SEND_SMS at runtime with clear disclosure.
- Private sideloading does not remove Android permission, carrier terms or legal obligations.
- In India, commercial/OTP messaging may require TRAI/DLT-compliant routes and registered providers. Do not assume a personal SIM/free SMS bundle is suitable for commercial traffic.
