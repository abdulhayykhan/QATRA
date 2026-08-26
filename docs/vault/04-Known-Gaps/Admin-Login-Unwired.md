---
tags: [gap, risk]
severity: medium
status: partially-resolved
---
## What's wrong / missing
The admin sign-in now authenticates against the real Supabase auth backend with an
email/password credential, and the signed-in admin identity carries a `user_role=admin`
JWT claim that the row-level-security policies enforce. What remains open is **live TOTP**:
the sign-in screen still shows a 2FA code field, but the code is accepted without being
verified. Multi-factor auth is therefore not yet enforced for the admin terminal.

## Why it matters
Password auth closes the original hole (previously the screen advanced with no credential
check at all, and a later iteration compiled a shared demo password into the APK — both are
now gone). The residual risk is narrower: an attacker who obtains the admin password faces
no second factor. For a closed pilot with a single rotated admin credential this is an
accepted, bounded risk; it must be closed before any wider rollout.

## What "resolved" looks like
Supabase MFA (TOTP enroll + challenge) is wired so the 2FA field is actually verified on
sign-in, and enrolment is mandatory for the admin account.

## Evidence (how this was confirmed, and when)
- Fixed on 2026-08-26: `adminSignIn` in `app/src/main/java/com/example/data/repository/QatraRepository.kt`
  calls `client.auth.signInWith(Email) { ... }`; the compiled-in demo credentials
  (`ADMIN_DEMO_EMAIL/PASSWORD/TOTP`) were deleted from `QatraViewModel.kt`.
- The admin auth user (`admin@qatra.org`, `raw_app_meta_data.user_role = "admin"`,
  `email_confirmed = true`) was provisioned directly in `auth.users`/`auth.identities` on the
  live Supabase project and its password hash was verified via a `crypt()` roundtrip.
- Remaining gap: the TOTP field in `AdminLogin2FAScreen` is accepted but not validated
  (see the `ponytail:` note in `QatraViewModel.adminSignIn`).
