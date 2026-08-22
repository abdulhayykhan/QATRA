-- QATRA ownership/contact prerequisites for RLS.
-- Apply after docs/qatra_postgres_schema.sql and before docs/qatra_rls_policies.sql.

ALTER TABLE donor_profiles
    ADD COLUMN IF NOT EXISTS auth_user_id UUID REFERENCES auth.users(id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_donor_profiles_auth_user_id
    ON donor_profiles (auth_user_id)
    WHERE auth_user_id IS NOT NULL;

ALTER TABLE blood_requests
    ADD COLUMN IF NOT EXISTS seeker_auth_user_id UUID REFERENCES auth.users(id);

CREATE INDEX IF NOT EXISTS idx_blood_requests_seeker_auth_user_id
    ON blood_requests (seeker_auth_user_id);

ALTER TABLE request_sensitive_data
    ADD COLUMN IF NOT EXISTS seeker_phone_e164 TEXT;

COMMENT ON COLUMN donor_profiles.auth_user_id IS
    'Supabase Auth user who owns this donor profile.';

COMMENT ON COLUMN blood_requests.seeker_auth_user_id IS
    'Supabase Auth user who owns this blood request.';

COMMENT ON COLUMN request_sensitive_data.seeker_phone_e164 IS
    'Unmasked seeker phone, readable only through explicitly authorized policies.';