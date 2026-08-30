---
tags: [gap, risk]
severity: high
status: open
---
## What's wrong / missing
Full live, on-device end-to-end verification of the complete seeker → donor → admin loop against the real deployed AWS/Supabase/Firebase stack is still pending.

## Why it matters
Every "real" backend integration has been authored and reviewed, and passes local editor diagnostics or local pytest, but has never been fully end-to-end verified by real users in a live environment. There is a high risk of runtime failures, network issues, or configuration mismatches.

## What "resolved" looks like
Manually exercise the full loop on a device connected to the live backend server.

## Evidence (how this was confirmed, and when)
Noted in the current README.md under "Known Gaps".
