---
tags: [decision]
status: decided
date: 2026-08-23
---
## Decision
Supabase (Postgres + PostGIS) chosen for backend database and geo-queries.

## Context / why
Chosen over a custom backend to rapidly implement geospatial queries (`ST_DWithin`, `ST_Distance`) and backend auth/storage without managing custom server infrastructure for a closed pilot.

## Alternatives considered
Custom backend (Node.js/Python) with a managed database.

## Tradeoffs accepted
Vendor lock-in with Supabase and heavy reliance on Row-Level Security (RLS) for data privacy instead of backend application logic.

## Who needs to sign off
None.
