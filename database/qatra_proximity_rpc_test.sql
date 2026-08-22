-- Integration test for find_eligible_donors_for_request.
-- Run against a disposable Supabase/Postgres database with qatra_postgres_schema.sql applied.
-- The transaction is rolled back and leaves no test records behind.

BEGIN;

DO $$
DECLARE
    hospital_id UUID := gen_random_uuid();
    request_id UUID := gen_random_uuid();
    eligible_donor_id UUID := gen_random_uuid();
    outside_donor_id UUID := gen_random_uuid();
    cooldown_donor_id UUID := gen_random_uuid();
    failed_screening_donor_id UUID := gen_random_uuid();
    match_count INTEGER;
BEGIN
    INSERT INTO hospitals (id, name, short_name, address, district, location)
    VALUES (
        hospital_id,
        'RPC Test Hospital',
        'RPC Hospital',
        'Test Address',
        'Test District',
        ST_SetSRID(ST_MakePoint(67.001, 24.861), 4326)::geography
    );

    INSERT INTO donor_profiles (id, display_name, blood_group, is_available_to_donate, is_eligible, cooldown_days_remaining, is_cnic_verified)
    VALUES
        (eligible_donor_id, 'Eligible', 'O+', true, true, 0, true),
        (outside_donor_id, 'Outside Radius', 'O+', true, true, 0, true),
        (cooldown_donor_id, 'Cooldown', 'O+', true, true, 45, true),
        (failed_screening_donor_id, 'Failed Screening', 'O+', true, true, 0, true);

    INSERT INTO donor_locations (donor_id, latitude, longitude, location)
    VALUES
        (eligible_donor_id, 24.8615, 67.0015, ST_SetSRID(ST_MakePoint(67.0015, 24.8615), 4326)::geography),
        (outside_donor_id, 25.0500, 67.2500, ST_SetSRID(ST_MakePoint(67.2500, 25.0500), 4326)::geography),
        (cooldown_donor_id, 24.8615, 67.0015, ST_SetSRID(ST_MakePoint(67.0015, 24.8615), 4326)::geography),
        (failed_screening_donor_id, 24.8615, 67.0015, ST_SetSRID(ST_MakePoint(67.0015, 24.8615), 4326)::geography);

    INSERT INTO pre_screening_answers (
        donor_id, age_valid, weight_valid, no_recent_illness,
        no_recent_donation, no_recent_tattoo_or_surgery
    )
    VALUES
        (eligible_donor_id, true, true, true, true, true),
        (outside_donor_id, true, true, true, true, true),
        (cooldown_donor_id, true, true, true, true, true),
        (failed_screening_donor_id, true, true, false, true, true);

    INSERT INTO blood_requests (
        id, hospital_id, blood_group, component, units_required, urgency,
        seeker_name, status, mrn_number, is_verified
    )
    VALUES (
        request_id, hospital_id, 'O+', 'PRBC', 1, 'HIGH_PRIORITY',
        'RPC Test Seeker', 'BROADCASTING', 'MRN-RPC-TEST', true
    );

    SELECT count(*) INTO match_count
    FROM find_eligible_donors_for_request(request_id, 10)
    WHERE donor_id = eligible_donor_id;

    IF match_count <> 1 THEN
        RAISE EXCEPTION 'Eligible donor was not returned';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM find_eligible_donors_for_request(request_id, 10)
        WHERE donor_id IN (outside_donor_id, cooldown_donor_id, failed_screening_donor_id)
    ) THEN
        RAISE EXCEPTION 'Radius, cooldown, or failed-screening donor was returned';
    END IF;
END $$;

ROLLBACK;