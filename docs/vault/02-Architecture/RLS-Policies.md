---
tags: [architecture, rls]
status: active
last-updated: 2026-08-23
---
# Row-Level Security Policies

Role-by-table matrix from [qatra_rls_verification.sql](file:///c:/Users/USER/OneDrive%20-%20Dawood%20University%20of%20Engineering%20Technology/Desktop/AKK-SSIP/QATRA/database/qatra_rls_verification.sql):

| Table | Status | Note |
|---|---|---|
| hospitals | authored, not applied, not verified | |
| awareness_articles | authored, not applied, not verified | |
| donor_profiles | authored, not applied, not verified | |
| donor_locations | authored, not applied, not verified | |
| donor_device_tokens | authored, not applied, not verified | |
| donor_private_contacts | authored, not applied, not verified | |
| hospital_slip_documents | authored, not applied, not verified | |
| blood_requests | authored, not applied, not verified | |
| request_sensitive_data | authored, not applied, not verified | |
| matched_donor_requests | authored, not applied, not verified | |
| verification_queue | authored, not applied, not verified | |
| fraud_audit_items | authored, not applied, not verified | |
| campus_drives | authored, not applied, not verified | |
| drive_attendees | authored, not applied, not verified | |
| pre_screening_answers | authored, not applied, not verified | |

*(Note: Requires fresh check against repo as true state might have changed since last read. As of this writing, zero policies are applied to a live database.)*
