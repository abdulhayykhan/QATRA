---
tags: [gap, risk]
severity: low
status: resolved
---
## What's wrong / missing
Previously, Firebase ID tokens were not single-use and the `verify-firebase-phone` Edge
Function had no replay protection, so a valid token could be re-exchanged until its Firebase
expiry. This is now closed: the function records the SHA-256 of every accepted token in a
single-use ledger and rejects any repeat, and it now verifies tokens with revocation checking
enabled.

## Why it matters
A valid token could be intercepted and replayed until its expiry to mint new Supabase sessions.
The single-use ledger reduces the exploit window from ~1 hour to zero replays, and revocation
checking rejects tokens for sessions that Firebase has already invalidated.

## What "resolved" looks like
A hardening step persists a per-token identifier and rejects repeats, enforcing strict
single-use semantics. — Done.

## Evidence (how this was confirmed, and when)
Fixed on 2026-08-26:
- Migration `supabase/migrations/20260826133000_add_firebase_phone_token_replay_ledger.sql`
  creates `public.firebase_phone_token_ledger` (`token_hash` primary key, RLS enabled, no
  policies so only the service-role function can write). Applied to live project
  `glbxtvuanufqjsyjllcq` via Supabase MCP; a double-insert of the same `token_hash` was
  verified to raise `unique_violation` and leave no rows behind.
- `verify-firebase-phone/index.ts` hashes the raw token, prunes expired rows, and inserts the
  hash **before** minting a session (fail-closed); a duplicate insert (`23505`) returns HTTP
  `409`. Token verification now passes `checkRevoked = true`.
- Deployed as version 4 (ACTIVE, `verify_jwt = true`). Live smoke test: bogus token → `401`,
  missing token → `400`, `GET` → `405`.

## Residual note
Full end-to-end replay (re-POSTing a genuine, freshly issued Firebase ID token) was not
exercised because a real token cannot be minted from this environment; the dedup mechanism was
proven at the database layer instead. `checkRevoked = true` adds one Firebase lookup per login,
which is acceptable for the pilot's login volume.

