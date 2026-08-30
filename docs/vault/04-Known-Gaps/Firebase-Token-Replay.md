---
tags: [gap, risk]
severity: low
status: open
---
## What's wrong / missing
Firebase ID token replay protection is not yet implemented in the auth bridge for FastAPI.

## Why it matters
A valid token could be intercepted and replayed until its expiry to mint new FastAPI sessions. 
While this was previously resolved in the Supabase Edge Function implementation, the migration to FastAPI re-opened this gap.

## What "resolved" looks like
A hardening step in the FastAPI `verify-firebase-phone` endpoint persists a per-token identifier (like a hash) in a ledger and rejects repeats, enforcing strict single-use semantics.

## Evidence (how this was confirmed, and when)
Noted in the current README under "Known Gaps": "Firebase ID token replay protection is not yet implemented in the auth bridge — acceptable for a small closed pilot, worth hardening before wider rollout."
