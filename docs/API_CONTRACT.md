# Backend API contract — fill in before Phase 4

Do not let the coding agent invent this contract.

Provide:
- Base URL (non-secret)
- Authentication method and token lifecycle
- Fetch pending job endpoint and HTTP method
- Request/response JSON examples
- Job ID and idempotency semantics
- Status acknowledgement endpoint and allowed status values
- Retry/backoff rules
- Whether the backend can reconcile ambiguous SMS submissions
- Test environment and test-recipient allowlist
