---
tags: [decision]
status: decided
date: 2026-08-23
---
## Decision
CNIC validation includes format (13 digits) and district-range check only. No checksum validation, no NADRA integration.

## Context / why
Pakistani CNICs have no published checksum (the 13th digit indicates gender only). Real identity confirmation requires NADRA Verisys integration, which is out of scope due to lack of API access.

## Alternatives considered
Earlier builds fabricated a checksum algorithm on the 13th digit. This was later found to be invalid and removed. This serves as a documented lesson against fabricating validation constraints that don't match reality.

## Tradeoffs accepted
We cannot cryptographically or algorithmically verify a CNIC's absolute authenticity, only its format plausibility. UI copy says "CNIC Format Check," not "Identity Verified."

## Who needs to sign off
None.
