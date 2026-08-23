---
tags: [gap, risk]
severity: high
status: open
---
## What's wrong / missing
The admin sign-in screen advances straight to the verification queue on tap with no credential or TOTP check.

## Why it matters
Anyone who can access the admin surface in the app can instantly bypass authentication, gaining full access to sensitive verification queues and fraud audit features.

## What "resolved" looks like
Real authentication gate (credentials + TOTP) is wired and functioning before any non-developer touches the admin surface.

## Evidence (how this was confirmed, and when)
Confirmed on 2026-08-23 in `app/src/main/java/com/example/ui/admin/AdminScreens.kt`:
```kotlin
// // MOCK: 2FA validation pass
viewModel.setAdminStep(AdminScreenStep.VERIFICATION_QUEUE)
```
