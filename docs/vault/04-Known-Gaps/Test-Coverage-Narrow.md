---
tags: [gap, risk]
severity: medium
status: open
---
## What's wrong / missing
Test coverage is not comprehensive.

## Why it matters
There is no automated protection against regressions in OCR/upload, push notifications, or calling.

## What "resolved" looks like
Expanded Pytest test coverage exercising OCR/upload flow, push notifications, and calling.

## Evidence (how this was confirmed, and when)
Confirmed via README. Coverage exists for authorization rules, OTP flow, and geo-matching, but some gaps remain.
