---
tags: [gap, risk]
severity: medium
status: open
---
## What's wrong / missing
Firebase ID tokens are not single-use and the `verify-firebase-phone` Edge Function does not implement replay protection.

## Why it matters
A valid token could be intercepted and replayed until its expiry to mint new Supabase sessions.

## What "resolved" looks like
A hardening step persists Firebase `uid` plus `iat`/`jti` and rejects repeats, enforcing strict single-use semantics.

## Evidence (how this was confirmed, and when)
Confirmed via `supabase/functions/verify-firebase-phone/README.md` on 2026-08-23:
> "Firebase ID tokens are not single-use. This function does not store tokens or add a replay ledger, so a valid token could be replayed until expiry."
