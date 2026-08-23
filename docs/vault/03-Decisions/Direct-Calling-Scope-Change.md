---
tags: [decision]
status: needs-signoff
date: 2026-08-23
---
## Decision
Direct `Intent.ACTION_DIAL` instead of masked/proxy calling.

## Context / why
Cost-driven. Masked/proxy phone calling (PRD Section 4.4) was replaced with direct dialing between matched parties, because proxy-calling providers are paid, per-minute services outside the current budget.

## Alternatives considered
Masked/proxy calling using paid SMS/call providers.

## Tradeoffs accepted
The PRD Section 7.4 KPI ("Contact Privacy Breaches: Zero direct phone exposures") is no longer met by design.

## Who needs to sign off
Explicit sign-off required from the Alkhidmat supervisor.
