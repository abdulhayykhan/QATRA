-- QATRA PostgreSQL / PostGIS schema review draft
-- Design goal: separate public feed/map data from sensitive identity and OCR records,
-- and keep phone/CNIC data behind restricted access paths.

CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- Core lookup / infrastructure tables
-- ============================================================

CREATE TABLE hospitals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    short_name TEXT NOT NULL,
    address TEXT NOT NULL,
    district TEXT NOT NULL,
    is_trauma_center BOOLEAN NOT NULL DEFAULT false,
    location GEOGRAPHY(Point, 4326),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_hospitals_geography
    ON hospitals USING GIST (location);

-- RLS stub:
-- guest: SELECT on public hospital metadata only
-- verified_seeker: SELECT
-- verified_donor: SELECT
-- drive_organizer: SELECT
-- admin: SELECT/INSERT/UPDATE/DELETE

CREATE TABLE awareness_articles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    category TEXT NOT NULL,
    read_time TEXT NOT NULL,
    summary TEXT NOT NULL,
    full_content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- RLS stub:
-- guest: SELECT
-- verified_seeker: SELECT
-- verified_donor: SELECT
-- drive_organizer: SELECT
-- admin: SELECT/INSERT/UPDATE/DELETE

-- ============================================================
-- Public donor identity/profile data
-- ============================================================

CREATE TABLE donor_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_name TEXT NOT NULL,
    blood_group TEXT NOT NULL CHECK (blood_group IN ('A+','A-','B+','B-','O+','O-','AB+','AB-')),
    phone_masked TEXT NOT NULL DEFAULT '0300-XXXXXXX',
    cnic_masked TEXT NOT NULL DEFAULT '42101-XXXXXXX-7',
    is_available_to_donate BOOLEAN NOT NULL DEFAULT true,
    is_eligible BOOLEAN NOT NULL DEFAULT true,
    cooldown_days_remaining SMALLINT NOT NULL DEFAULT 0,
    lifetime_donations INTEGER NOT NULL DEFAULT 0,
    tier TEXT NOT NULL DEFAULT 'Silver Tier',
    district TEXT NOT NULL DEFAULT 'Karachi South',
    is_cnic_verified BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- RLS stub:
-- guest: SELECT only masked donor metadata required for public matches; no direct phone/CNIC
-- verified_seeker: SELECT on public donor profile metadata only
-- verified_donor: SELECT/UPDATE own row
-- drive_organizer: SELECT on donor list needed for event sign-in / roster management
-- admin: full access

CREATE TABLE donor_locations (
    donor_id UUID PRIMARY KEY REFERENCES donor_profiles(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    location GEOGRAPHY(Point, 4326) NOT NULL,
    source TEXT NOT NULL DEFAULT 'device',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (latitude BETWEEN -90 AND 90),
    CHECK (longitude BETWEEN -180 AND 180)
);

CREATE INDEX idx_donor_locations_geography
    ON donor_locations USING GIST (location);

-- RLS stub:
-- guest: no access
-- verified_seeker: no access
-- verified_donor: SELECT/UPDATE own row
-- drive_organizer: SELECT for location-aware scheduling only if authorized by event scope
-- admin: full access

-- NOTE:
-- The donor location table is intentionally separated from donor identity/profile data to
-- prevent map query patterns from dragging in personal or sensitive identity columns.

CREATE TABLE donor_device_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    donor_id UUID NOT NULL REFERENCES donor_profiles(id) ON DELETE CASCADE,
    token TEXT NOT NULL UNIQUE,
    platform TEXT NOT NULL DEFAULT 'android' CHECK (platform IN ('android')),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_donor_device_tokens_donor_id
    ON donor_device_tokens (donor_id);

-- RLS stub:
-- guest: no access
-- verified_seeker: no access
-- verified_donor: SELECT/INSERT/UPDATE own token rows only
-- admin: full access

-- ============================================================
-- Restricted-sensitive identity/contact data
-- ============================================================

CREATE TABLE donor_private_contacts (
    donor_id UUID PRIMARY KEY REFERENCES donor_profiles(id) ON DELETE CASCADE,
    phone_e164 TEXT NOT NULL,
    cnic TEXT NOT NULL,
    cnic_hash TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- RLS stub:
-- guest: no access
-- verified_seeker: no access
-- verified_donor: SELECT/UPDATE own row only
-- drive_organizer: no access
-- admin: full access

-- NOTE:
-- This table stores the actual phone number and CNIC in a restricted, admin-only access path.
-- Public queries use donor_profiles.phone_masked and donor_profiles.cnic_masked instead.

CREATE TABLE hospital_slip_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL,
    document_kind TEXT NOT NULL CHECK (document_kind IN ('FRONT','BACK')),
    storage_path TEXT NOT NULL,
    sha256_digest TEXT NOT NULL,
    mrn TEXT,
    doctor_stamp_detected BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- RLS stub:
-- guest: no access
-- verified_seeker: SELECT own slip metadata only when needed for verification workflow
-- verified_donor: no access
-- drive_organizer: no access
-- admin: full access

-- NOTE:
-- Slip references live outside the public request rows so the feed/map can query request metadata
-- without exposing document storage references or raw identity fields.

-- ============================================================
-- Request lifecycle / seeker-facing records
-- ============================================================

CREATE TABLE blood_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hospital_id UUID NOT NULL REFERENCES hospitals(id),
    blood_group TEXT NOT NULL CHECK (blood_group IN ('A+','A-','B+','B-','O+','O-','AB+','AB-')),
    component TEXT NOT NULL CHECK (component IN ('WHOLE_BLOOD','PRBC','PLATELETS','PLASMA')),
    units_required INTEGER NOT NULL CHECK (units_required > 0),
    urgency TEXT NOT NULL CHECK (urgency IN ('HIGH_PRIORITY','STANDARD')),
    seeker_name TEXT NOT NULL,
    seeker_phone_masked TEXT NOT NULL DEFAULT '0300-XXXXXXX',
    seeker_cnic_masked TEXT NOT NULL DEFAULT '42101-XXXXXXX-1',
    status TEXT NOT NULL CHECK (status IN ('VERIFYING','BROADCASTING','DONOR_MATCHED','EN_ROUTE','FULFILLED','CLOSED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    active_donors_in_radius INTEGER NOT NULL DEFAULT 0,
    responded_donors_count INTEGER NOT NULL DEFAULT 0,
    mrn_number TEXT NOT NULL,
    ocr_confidence INTEGER NOT NULL DEFAULT 0 CHECK (ocr_confidence BETWEEN 0 AND 100),
    is_verified BOOLEAN NOT NULL DEFAULT true,
    doctor_stamp_verified BOOLEAN NOT NULL DEFAULT true,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- RLS stub:
-- guest: SELECT only public request records (masked seeker info) for feed/listing
-- verified_seeker: SELECT own request rows; INSERT/UPDATE own requests
-- verified_donor: SELECT active/broadcasting request rows necessary for donation matching
-- drive_organizer: SELECT for coordination tasks; no direct edits unless assigned
-- admin: full access

CREATE TABLE request_sensitive_data (
    request_id UUID PRIMARY KEY REFERENCES blood_requests(id) ON DELETE CASCADE,
    seeker_phone_hash TEXT NOT NULL,
    seeker_cnic TEXT NOT NULL,
    seeker_cnic_hash TEXT NOT NULL,
    raw_ocr_text TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- RLS stub:
-- guest: no access
-- verified_seeker: SELECT own row only
-- verified_donor: no access
-- drive_organizer: no access
-- admin: full access

-- NOTE:
-- Request-level sensitive data is separated from request feed objects. This keeps map/feed queries
-- fast and avoids exposing CNIC or phone data in the same rows that are publicly visible.

CREATE TABLE matched_donor_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL REFERENCES blood_requests(id) ON DELETE CASCADE,
    donor_id UUID NOT NULL REFERENCES donor_profiles(id) ON DELETE CASCADE,
    blood_group TEXT NOT NULL CHECK (blood_group IN ('A+','A-','B+','B-','O+','O-','AB+','AB-')),
    distance_km DOUBLE PRECISION NOT NULL DEFAULT 0,
    eta_minutes INTEGER NOT NULL DEFAULT 0,
    status_text TEXT NOT NULL,
    phone_masked TEXT NOT NULL DEFAULT '0300-XXXXXXX',
    is_verified BOOLEAN NOT NULL DEFAULT true,
    lifetime_donations INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (request_id, donor_id)
);

-- RLS stub:
-- guest: no access
-- verified_seeker: SELECT own request matches only
-- verified_donor: SELECT own match rows only
-- drive_organizer: SELECT for operational coordination
-- admin: full access

CREATE TABLE verification_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL REFERENCES blood_requests(id) ON DELETE CASCADE,
    hospital_name TEXT NOT NULL,
    doctor_stamp_detected BOOLEAN NOT NULL DEFAULT false,
    mrn TEXT NOT NULL,
    blood_group TEXT NOT NULL CHECK (blood_group IN ('A+','A-','B+','B-','O+','O-','AB+','AB-')),
    units INTEGER NOT NULL CHECK (units > 0),
    ocr_confidence INTEGER NOT NULL CHECK (ocr_confidence BETWEEN 0 AND 100),
    blood_group_confidence INTEGER NOT NULL CHECK (blood_group_confidence BETWEEN 0 AND 100),
    flag_warning TEXT,
    status TEXT NOT NULL DEFAULT 'Pending' CHECK (status IN ('Pending','Approved','Rejected')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- RLS stub:
-- guest: no access
-- verified_seeker: no access
-- verified_donor: no access
-- drive_organizer: SELECT/UPDATE assigned queue items
-- admin: full access

CREATE TABLE fraud_audit_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL REFERENCES blood_requests(id) ON DELETE CASCADE,
    seeker_cnic_masked TEXT NOT NULL,
    phone_masked TEXT NOT NULL,
    hospital_mrn TEXT NOT NULL,
    ocr_confidence INTEGER NOT NULL CHECK (ocr_confidence BETWEEN 0 AND 100),
    flag_reason TEXT NOT NULL,
    action_status TEXT NOT NULL DEFAULT 'Flagged' CHECK (action_status IN ('Flagged','Blacklisted','Whitelisted')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- RLS stub:
-- guest: no access
-- verified_seeker: no access
-- verified_donor: no access
-- drive_organizer: SELECT for disputed registration cases if scoped to their events
-- admin: full access

-- ============================================================
-- Campus drive / attendee / screening data
-- ============================================================

CREATE TABLE campus_drives (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    university_venue TEXT NOT NULL,
    target_quota_units INTEGER NOT NULL CHECK (target_quota_units > 0),
    registered_donors INTEGER NOT NULL DEFAULT 0,
    date_str TEXT NOT NULL,
    time_str TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'Scheduled' CHECK (status IN ('Scheduled','Open','Closed','Cancelled')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- RLS stub:
-- guest: SELECT public drive listings
-- verified_seeker: SELECT
-- verified_donor: SELECT
-- drive_organizer: SELECT/INSERT/UPDATE their drives
-- admin: full access

CREATE TABLE drive_attendees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    drive_id UUID NOT NULL REFERENCES campus_drives(id) ON DELETE CASCADE,
    donor_id UUID NOT NULL REFERENCES donor_profiles(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    dept_year TEXT,
    cnic_status TEXT NOT NULL DEFAULT 'Verified' CHECK (cnic_status IN ('Verified','Pending','Failed')),
    pre_screening_status TEXT NOT NULL DEFAULT 'Passed' CHECK (pre_screening_status IN ('Passed','Pending','Failed')),
    check_in_status TEXT NOT NULL DEFAULT 'Checked In 10:42 AM',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (drive_id, donor_id)
);

-- RLS stub:
-- guest: no access
-- verified_seeker: no access
-- verified_donor: SELECT own attendance/registration rows only
-- drive_organizer: SELECT/UPDATE for their own drive roster
-- admin: full access

CREATE TABLE pre_screening_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    donor_id UUID NOT NULL REFERENCES donor_profiles(id) ON DELETE CASCADE,
    age_valid BOOLEAN NOT NULL DEFAULT true,
    weight_valid BOOLEAN NOT NULL DEFAULT true,
    no_recent_illness BOOLEAN NOT NULL DEFAULT true,
    no_recent_donation BOOLEAN NOT NULL DEFAULT true,
    no_recent_tattoo_or_surgery BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (donor_id)
);

-- RLS stub:
-- guest: no access
-- verified_seeker: no access
-- verified_donor: SELECT/UPDATE own pre-screening result only
-- drive_organizer: SELECT on roster outcomes for their drives
-- admin: full access

-- ============================================================
-- Proximity matching RPC
-- ============================================================

CREATE OR REPLACE FUNCTION find_eligible_donors_for_request(
    p_request_id UUID,
    p_radius_km DOUBLE PRECISION
)
RETURNS TABLE (
    donor_id UUID,
    display_name TEXT,
    blood_group TEXT,
    distance_km DOUBLE PRECISION,
    eta_minutes INTEGER,
    status_text TEXT,
    phone_masked TEXT,
    is_verified BOOLEAN,
    lifetime_donations INTEGER
)
LANGUAGE SQL
STABLE
SECURITY DEFINER
SET search_path = public, extensions
AS $$
    SELECT
        dp.id AS donor_id,
        dp.display_name,
        dp.blood_group,
        ST_Distance(dl.location, h.location) / 1000.0 AS distance_km,
        GREATEST(1, CEIL((ST_Distance(dl.location, h.location) / 1000.0) * 4)::INTEGER) AS eta_minutes,
        'Notified'::TEXT AS status_text,
        dp.phone_masked,
        (dp.is_cnic_verified AND dp.is_eligible) AS is_verified,
        dp.lifetime_donations
    FROM blood_requests br
    JOIN hospitals h ON h.id = br.hospital_id
    JOIN donor_locations dl ON true
    JOIN donor_profiles dp ON dp.id = dl.donor_id
    JOIN pre_screening_answers psa ON psa.donor_id = dp.id
    WHERE br.id = p_request_id
      AND br.status = 'BROADCASTING'
      AND h.location IS NOT NULL
      AND ST_DWithin(h.location, dl.location, p_radius_km * 1000.0)
      AND dp.is_available_to_donate
      AND dp.is_eligible
      AND dp.cooldown_days_remaining = 0
      AND dp.is_cnic_verified
      AND psa.age_valid
      AND psa.weight_valid
      AND psa.no_recent_illness
      AND psa.no_recent_donation
      AND psa.no_recent_tattoo_or_surgery
      AND (
          dp.blood_group = br.blood_group
          OR (br.blood_group = 'A+' AND dp.blood_group IN ('A+','A-','O+','O-'))
          OR (br.blood_group = 'A-' AND dp.blood_group IN ('A-','O-'))
          OR (br.blood_group = 'B+' AND dp.blood_group IN ('B+','B-','O+','O-'))
          OR (br.blood_group = 'B-' AND dp.blood_group IN ('B-','O-'))
          OR (br.blood_group = 'AB+' AND dp.blood_group IN ('A+','A-','B+','B-','AB+','AB-','O+','O-'))
          OR (br.blood_group = 'AB-' AND dp.blood_group IN ('A-','B-','AB-','O-'))
          OR (br.blood_group = 'O-' AND dp.blood_group = 'O-')
          OR (br.blood_group = 'O+' AND dp.blood_group IN ('O+','O-'))
      )
    ORDER BY ST_Distance(dl.location, h.location) ASC;
$$;

-- TODO: Add radius auto-expansion and rare-blood-group bypass logic in a later task.

-- ============================================================
-- Public-facing data access views
-- ============================================================

CREATE VIEW public_donor_directory AS
SELECT
    dp.id,
    dp.display_name,
    dp.blood_group,
    dp.phone_masked,
    dp.cnic_masked,
    dp.is_available_to_donate,
    dp.is_eligible,
    dp.cooldown_days_remaining,
    dp.lifetime_donations,
    dp.tier,
    dp.district,
    dp.is_cnic_verified
FROM donor_profiles dp;

-- RLS stub for view:
-- guest: SELECT on public_donor_directory only
-- verified_seeker: SELECT
-- verified_donor: SELECT own row
-- drive_organizer: SELECT roster data
-- admin: full access

CREATE VIEW public_request_feed AS
SELECT
    br.id,
    br.hospital_id,
    br.blood_group,
    br.component,
    br.units_required,
    br.urgency,
    br.seeker_name,
    br.seeker_phone_masked,
    br.seeker_cnic_masked,
    br.status,
    br.created_at,
    br.active_donors_in_radius,
    br.responded_donors_count,
    br.mrn_number,
    br.ocr_confidence,
    br.is_verified,
    br.doctor_stamp_verified
FROM blood_requests br;

-- RLS stub for view:
-- guest: SELECT only public request rows and masked seeker info
-- verified_seeker: SELECT own requests and public feed
-- verified_donor: SELECT active public requests
-- drive_organizer: SELECT for operational visibility
-- admin: full access

-- ============================================================
-- Design notes / security posture summary
-- ============================================================
-- 1) donor location data is isolated in donor_locations because it is a geospatial, map-driven concern
--    and should never be joined with personally identifying or OCR-sensitive tables in routine map queries.
-- 2) CNIC values and slip document references are held in separate tables to keep the public feed and map
--    tables clean, faster, and safer while preserving a secure audit trail for verification workflows.
-- 3) Phone and CNIC values are never placed in public-facing tables. The masked fields in donor_profiles and
--    blood_requests are the only values exposed in normal queries, while the actual sensitive data sits in
--    donor_private_contacts and request_sensitive_data behind restricted access.
-- 4) The schema intentionally leaves permissions and policy bodies as comments for later implementation,
--    because the architecture review step is meant to define the correct access boundaries before writing RLS.
