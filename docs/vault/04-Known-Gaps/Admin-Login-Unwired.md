---
tags: [gap, risk]
severity: medium
status: resolved
---
## What's wrong / missing
(Historical gap) The admin sign-in previously went unauthenticated or used a compiled-in demo credential, and later lacked TOTP validation even when password auth was added.

## Why it matters
MFA is required for admin access.

## What "resolved" looks like
Admin login uses real TOTP verification with a passing regression test proving bad credentials are rejected.

## Evidence (how this was confirmed, and when)
Resolved in the FastAPI backend migration. The README states: "Admin login uses real TOTP verification with a passing regression test proving bad credentials are rejected."
