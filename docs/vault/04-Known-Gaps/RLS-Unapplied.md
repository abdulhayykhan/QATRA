---
tags: [gap, risk]
severity: medium
status: resolved
---
## What's wrong / missing
(Historical gap) RLS policies were applied and enabled on the live database, but custom access-token hook wasn't activated, meaning the `user_role` claim the policies read was NULL.

## Why it matters
Without RLS policies fully functional, unauthorized access could happen.

## What "resolved" looks like
Resolved by architecture change.

## Evidence (how this was confirmed, and when)
The project migrated to a FastAPI backend. Rather than relying on database-level row-security policies (the original Supabase-RLS approach), the current backend centralizes every access-control decision in `api/authorization.py`. This was a deliberate design choice so access-control logic lives in one reviewable, testable place.
