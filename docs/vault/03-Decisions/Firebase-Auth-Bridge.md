---
tags: [decision]
status: decided
date: 2026-08-23
---
## Decision
Firebase Phone Auth bridged to a Supabase session via a custom Edge Function (`verify-firebase-phone`), instead of native Supabase phone OTP.

## Context / why
To avoid a paid SMS provider. Supabase's native phone OTP requires configuring a paid SMS provider per project, while Firebase Phone Auth handles SMS delivery natively.

## Alternatives considered
Native Supabase phone OTP with a paid SMS provider like Twilio.

## Tradeoffs accepted
- Adds complexity (Firebase -> Edge Function -> Supabase).
- Tokens are not currently single-use (replay protection is pending).
- Supabase Auth doesn't expose a clean Admin API for this, so the function rotates users to a random password server-side.

## Who needs to sign off
None.
