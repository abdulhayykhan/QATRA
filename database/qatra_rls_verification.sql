-- Run after qatra_ownership_columns.sql and qatra_rls_policies.sql in a disposable Supabase test project.
-- Set a JWT with the role claim and matching auth.uid() for each session before each query.

-- Example role setup (use the Supabase SQL editor's authenticated session, not service_role):
-- select set_config('request.jwt.claims', '{"sub":"<USER_UUID>","role":"authenticated","user_role":"guest"}', true);

SELECT tablename, rowsecurity
FROM pg_tables
WHERE schemaname = 'public'
  AND tablename IN (
    'hospitals', 'awareness_articles', 'donor_profiles', 'donor_locations',
    'donor_device_tokens', 'donor_private_contacts', 'hospital_slip_documents',
    'blood_requests', 'request_sensitive_data', 'matched_donor_requests',
    'verification_queue', 'fraud_audit_items', 'campus_drives',
    'drive_attendees', 'pre_screening_answers'
  )
ORDER BY tablename;

-- Sensitive-table checks: execute each SELECT in a session configured as the listed role.
-- A zero-row result is expected for all roles except admin, and verified_donor for its own row.
SELECT 'guest' AS simulated_role, count(*) AS visible_rows FROM public.donor_private_contacts;
SELECT 'verified_seeker' AS simulated_role, count(*) AS visible_rows FROM public.donor_private_contacts;
SELECT 'verified_donor' AS simulated_role, count(*) AS visible_rows FROM public.donor_private_contacts;
SELECT 'drive_organizer' AS simulated_role, count(*) AS visible_rows FROM public.donor_private_contacts;
SELECT 'admin' AS simulated_role, count(*) AS visible_rows FROM public.donor_private_contacts;

-- Guest/anon blood-request assertion. Run this section in an anon session.
-- It must return only BROADCASTING rows and zero rows in every other status.
SELECT status, count(*) AS visible_rows
FROM public.blood_requests
GROUP BY status
ORDER BY status;

SELECT
  count(*) FILTER (WHERE status = 'BROADCASTING') AS broadcasting_rows,
  count(*) FILTER (WHERE status <> 'BROADCASTING' OR status IS NULL) AS non_broadcasting_rows,
  CASE
    WHEN count(*) FILTER (WHERE status <> 'BROADCASTING' OR status IS NULL) = 0
    THEN 'PASS: anon sees no non-BROADCASTING rows'
    ELSE 'FAIL: anon can see a non-BROADCASTING row'
  END AS status_policy_assertion
FROM public.blood_requests;

-- Full role-by-table matrix (run each statement under each role session).
SELECT 'hospitals' AS table_name, count(*) AS visible_rows FROM public.hospitals;
SELECT 'awareness_articles', count(*) FROM public.awareness_articles;
SELECT 'donor_profiles', count(*) FROM public.donor_profiles;
SELECT 'donor_locations', count(*) FROM public.donor_locations;
SELECT 'donor_device_tokens', count(*) FROM public.donor_device_tokens;
SELECT 'donor_private_contacts', count(*) FROM public.donor_private_contacts;
SELECT 'hospital_slip_documents', count(*) FROM public.hospital_slip_documents;
SELECT 'blood_requests', count(*) FROM public.blood_requests;
SELECT 'request_sensitive_data', count(*) FROM public.request_sensitive_data;
SELECT 'matched_donor_requests', count(*) FROM public.matched_donor_requests;
SELECT 'verification_queue', count(*) FROM public.verification_queue;
SELECT 'fraud_audit_items', count(*) FROM public.fraud_audit_items;
SELECT 'campus_drives', count(*) FROM public.campus_drives;
SELECT 'drive_attendees', count(*) FROM public.drive_attendees;
SELECT 'pre_screening_answers', count(*) FROM public.pre_screening_answers;