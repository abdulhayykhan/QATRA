---
tags: [gap, risk]
severity: unconfirmed
status: resolved
---
## What's wrong / missing
(Historical gap) Observed build/dependency problems related to Maven dependency-resolution errors (e.g., involving a supabase artifact path).

## Why it matters
Could prevent building the app successfully.

## What "resolved" looks like
Resolved by architecture change.

## Evidence (how this was confirmed, and when)
The project was migrated from Kotlin/Jetpack Compose to Flutter + FastAPI. The original Supabase Android SDK dependencies are gone.
