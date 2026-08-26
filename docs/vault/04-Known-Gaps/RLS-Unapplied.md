---
tags: [gap, risk]
severity: medium
status: partially-resolved
---
## What's wrong / missing
RLS policies are now applied and enabled on the live database. What remains is activating the
custom access-token hook so the `user_role` claim the policies read actually reaches the JWT,
and running the five-role verification pass end to end.

## Why it matters
Without RLS policies applied, the database would rely entirely on client-side logic to prevent
unauthorized access. That half is closed. But every QATRA policy gates on a **top-level**
`auth.jwt() ->> 'user_role'` claim, and Supabase keeps custom data under `app_metadata`, so
until the access-token hook is enabled the claim is NULL for every authenticated user and all
role-gated policies fail closed (authenticated users, admin included, can touch nothing beyond
the anon-readable rows). Enforcement is therefore applied but inert for signed-in roles until
the hook is switched on.

## What "resolved" looks like
`qatra_rls_policies.sql` is applied to the live database (done), the `custom_access_token_hook`
is enabled in GoTrue config, and enforcement is verified via `qatra_rls_verification.sql` under
all five simulated roles.

## Evidence (how this was confirmed, and when)
Verified live on project `glbxtvuanufqjsyjllcq` on 2026-08-26 via Supabase MCP:
- All 16 application tables in `public` have `rowsecurity = true` with policies attached
  (e.g. `blood_requests` 7, `campus_drives`/`donor_locations` 4 each). `firebase_phone_token_ledger`
  has RLS on with 0 policies by design (service-role only). Only `spatial_ref_sys` (PostGIS
  EPSG catalog, public reference data) has RLS off — accepted.
- Every role-gated policy reads `auth.jwt() ->> 'user_role'`, confirming the hook dependency.
- `public.custom_access_token_hook` was created and self-tested: the live admin user resolves to
  claim `admin`, an unknown/role-less user resolves to `guest`. Migration
  `supabase/migrations/20260826140000_add_custom_access_token_hook_user_role.sql`.

## Residual
1. Enable the hook in the dashboard (Authentication -> Hooks -> Customize Access Token (JWT)
   Claims -> `public.custom_access_token_hook`); not settable via SQL, so it is a manual/Management-API
   step. 2. Run the five-role RLS verification pass against the live DB with the hook active.
