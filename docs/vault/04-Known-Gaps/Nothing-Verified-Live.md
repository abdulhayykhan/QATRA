---
tags: [gap, risk]
severity: high
status: open
---
## What's wrong / missing
No backend integration has been run against a live Supabase project or a real device with network access.

## Why it matters
Every "real" backend integration has been authored and reviewed, and passes local editor diagnostics, but has never run against an actual Supabase project. There is a high risk of runtime failures, network issues, or configuration mismatches.

## What "resolved" looks like
Apply the migrations to a live project in order, run the RLS verification queries under all five simulated roles, and manually exercise the full loop on a device.

## Evidence (how this was confirmed, and when)
Confirmed via README on 2026-08-23. The development environment lacked Gradle wrapper, Gradle CLI, psql, and Supabase CLI at various points, so verification to date has largely been static review.
