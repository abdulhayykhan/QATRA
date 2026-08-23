---
tags: [status]
status: active
last-updated: 2026-08-23
---
# Build Status

```dataview
TABLE severity, status
FROM "04-Known-Gaps"
WHERE status != "resolved"
SORT severity DESC
```
*(Note: If the Dataview plugin is not installed, the query above will just display as a code block. Please install Dataview to render it.)*

## Current Status (Verified 2026-08-23)

- **Live Map & Proximity Matching**: Implemented (RPC `find_eligible_donors_for_request`), not yet verified live.
- **Auth, Verification & Eligibility**: Implemented (Firebase phone auth bridge, OCR), not yet verified live.
- **Social Feed**: Implemented (mocked data). *(Note: Requires fresh check against repo as true state might have changed since last read)*
- **Awareness & Eligibility Module**: Implemented (mocked data). *(Note: Requires fresh check against repo as true state might have changed since last read)*
- **Push Notifications**: Implemented (Edge function `send-geo-alert`), not yet verified live.
- **Row-Level Security**: Authored and reviewed (50+ policies), never applied to live database. See [[RLS-Policies]]. *(Note: Requires fresh check against repo as true state might have changed since last read)*
- **Admin / Verification Desk**: UI implemented; admin login is mocked. See [[Admin-Login-Unwired]].
- **Direct Calling**: Implemented (`Intent.ACTION_DIAL`), not yet verified live. See [[Direct-Calling-Scope-Change]].
