---
tags: [decision]
status: decided
date: 2026-08-23
---
## Decision
Use `auth.jwt()` custom claims for Role-Based Access Control (RBAC) over a joined roles table for all RLS policies.

## Context / why
Allows RLS policies to execute extremely fast by avoiding expensive joins to external permission tables on every row access.

## Alternatives considered
Joining a roles table (e.g., `user_roles`) within every RLS policy.

## Tradeoffs accepted
~1-hour role-change propagation delay at Supabase's default token expiry. If a donor is suspended, their JWT token will still have access until it expires up to an hour later. High-stakes checks must be paired with a database-backed status column rather than relying solely on the JWT claim.

## Who needs to sign off
None.
