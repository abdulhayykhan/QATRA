---
tags: [gap, risk]
severity: medium
status: open
---
## What's wrong / missing
Test coverage is targeted at known past bugs, not comprehensive.

## Why it matters
There is no automated protection against regressions in RLS behavior, OCR/upload, push notifications, or calling.

## What "resolved" looks like
Expanded test coverage exercising RLS behavior automatically, OCR/upload flow, push notifications, and calling.

## Evidence (how this was confirmed, and when)
Confirmed via README on 2026-08-23. Coverage exists for the CNIC validator and an OTP-flow regression test, plus an unexecuted SQL fixture for the geo RPC. No coverage for RLS behavior, OCR/upload, push, or calling.
