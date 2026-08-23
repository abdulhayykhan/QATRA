---
tags: [gap, risk]
severity: high
status: open
---
## What's wrong / missing
Policies authored across all tables in the schema, but zero applied to a live database.

## Why it matters
Without RLS policies applied, the database relies entirely on client-side logic to prevent unauthorized access, meaning any authenticated (or unauthenticated, depending on default access) user might query or mutate any data if they connect directly to the API.

## What "resolved" looks like
`qatra_rls_policies.sql` is applied to a live database and verified via `qatra_rls_verification.sql`.

## Evidence (how this was confirmed, and when)
Confirmed via README and database scripts on 2026-08-23. See [[RLS-Policies]]. *(Note: True state might have changed since last read, flag for fresh check.)*
