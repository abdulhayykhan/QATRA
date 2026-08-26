-- QATRA baseline schema (reconstructed 2026-08-26 from the live Supabase catalog).
--
-- Captures the full public schema — extensions, tables, constraints, indexes,
-- RLS, policies, masked views, and the donor-matching function — exactly as it
-- existed on live project glbxtvuanufqjsyjllcq before any tracked migration. It
-- is the reproducible starting point for a fresh environment.
--
-- On the EXISTING live project these objects already exist, so DO NOT execute
-- this file there. Mark it applied without running it:
--     supabase migration repair --status applied 20260826000000
-- On a fresh project, `supabase db push` runs it normally.
--
-- The two public_* views are intentionally SECURITY DEFINER (owner-rights, the
-- Postgres default for a view): they expose only a masked column projection
-- while bypassing base-table RLS. Keep them that way — flipping to
-- security_invoker would leak or block, not fix. EXECUTE hardening on the
-- donor-matching function is applied by the next migration, not here, so on a
-- fresh env the function is briefly PUBLIC-executable until that migration runs.

create schema if not exists extensions;
create schema if not exists vault;

create extension if not exists "pg_stat_statements" with schema extensions;
create extension if not exists "pgcrypto" with schema extensions;
create extension if not exists "postgis" with schema public;
create extension if not exists "supabase_vault" with schema vault;
create extension if not exists "uuid-ossp" with schema extensions;

-- ---------------------------------------------------------------------------
-- Tables
-- ---------------------------------------------------------------------------

create table if not exists awareness_articles (
  id uuid not null default gen_random_uuid(),
  title text not null,
  category text not null,
  read_time text not null,
  summary text not null,
  full_content text not null,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now()
);

create table if not exists blood_requests (
  id uuid not null default gen_random_uuid(),
  hospital_id uuid not null,
  blood_group text not null,
  component text not null,
  units_required integer not null,
  urgency text not null,
  seeker_name text not null,
  seeker_phone_masked text not null default '0300-XXXXXXX'::text,
  seeker_cnic_masked text not null default '42101-XXXXXXX-1'::text,
  status text not null,
  created_at timestamp with time zone not null default now(),
  active_donors_in_radius integer not null default 0,
  responded_donors_count integer not null default 0,
  mrn_number text not null,
  ocr_confidence integer not null default 0,
  is_verified boolean not null default true,
  doctor_stamp_verified boolean not null default true,
  updated_at timestamp with time zone not null default now(),
  seeker_auth_user_id uuid
);

create table if not exists campus_drives (
  id uuid not null default gen_random_uuid(),
  title text not null,
  university_venue text not null,
  target_quota_units integer not null,
  registered_donors integer not null default 0,
  date_str text not null,
  time_str text not null,
  status text not null default 'Scheduled'::text,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now()
);

create table if not exists donor_device_tokens (
  id uuid not null default gen_random_uuid(),
  donor_id uuid not null,
  token text not null,
  platform text not null default 'android'::text,
  updated_at timestamp with time zone not null default now(),
  created_at timestamp with time zone not null default now()
);

create table if not exists donor_locations (
  donor_id uuid not null,
  latitude double precision not null,
  longitude double precision not null,
  location geography(Point,4326) not null,
  source text not null default 'device'::text,
  updated_at timestamp with time zone not null default now()
);

create table if not exists donor_private_contacts (
  donor_id uuid not null,
  phone_e164 text not null,
  cnic text not null,
  cnic_hash text not null,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now()
);

create table if not exists donor_profiles (
  id uuid not null default gen_random_uuid(),
  display_name text not null,
  blood_group text not null,
  phone_masked text not null default '0300-XXXXXXX'::text,
  cnic_masked text not null default '42101-XXXXXXX-7'::text,
  is_available_to_donate boolean not null default true,
  is_eligible boolean not null default true,
  cooldown_days_remaining smallint not null default 0,
  lifetime_donations integer not null default 0,
  tier text not null default 'Silver Tier'::text,
  district text not null default 'Karachi South'::text,
  is_cnic_verified boolean not null default true,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  auth_user_id uuid
);

create table if not exists drive_attendees (
  id uuid not null default gen_random_uuid(),
  drive_id uuid not null,
  donor_id uuid not null,
  name text not null,
  dept_year text,
  cnic_status text not null default 'Verified'::text,
  pre_screening_status text not null default 'Passed'::text,
  check_in_status text not null default 'Checked In 10:42 AM'::text,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now()
);

create table if not exists fraud_audit_items (
  id uuid not null default gen_random_uuid(),
  request_id uuid not null,
  seeker_cnic_masked text not null,
  phone_masked text not null,
  hospital_mrn text not null,
  ocr_confidence integer not null,
  flag_reason text not null,
  action_status text not null default 'Flagged'::text,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now()
);

create table if not exists hospital_slip_documents (
  id uuid not null default gen_random_uuid(),
  request_id uuid not null,
  document_kind text not null,
  storage_path text not null,
  sha256_digest text not null,
  mrn text,
  doctor_stamp_detected boolean not null default false,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now()
);

create table if not exists hospitals (
  id uuid not null default gen_random_uuid(),
  name text not null,
  short_name text not null,
  address text not null,
  district text not null,
  is_trauma_center boolean not null default false,
  location geography(Point,4326),
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now()
);

create table if not exists matched_donor_requests (
  id uuid not null default gen_random_uuid(),
  request_id uuid not null,
  donor_id uuid not null,
  blood_group text not null,
  distance_km double precision not null default 0,
  eta_minutes integer not null default 0,
  status_text text not null,
  phone_masked text not null default '0300-XXXXXXX'::text,
  is_verified boolean not null default true,
  lifetime_donations integer not null default 0,
  created_at timestamp with time zone not null default now()
);

create table if not exists pre_screening_answers (
  id uuid not null default gen_random_uuid(),
  donor_id uuid not null,
  age_valid boolean not null default true,
  weight_valid boolean not null default true,
  no_recent_illness boolean not null default true,
  no_recent_donation boolean not null default true,
  no_recent_tattoo_or_surgery boolean not null default true,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now()
);

create table if not exists request_sensitive_data (
  request_id uuid not null,
  seeker_phone_hash text not null,
  seeker_cnic text not null,
  seeker_cnic_hash text not null,
  raw_ocr_text text,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now(),
  seeker_phone_e164 text
);

create table if not exists verification_queue (
  id uuid not null default gen_random_uuid(),
  request_id uuid not null,
  hospital_name text not null,
  doctor_stamp_detected boolean not null default false,
  mrn text not null,
  blood_group text not null,
  units integer not null,
  ocr_confidence integer not null,
  blood_group_confidence integer not null,
  flag_warning text,
  status text not null default 'Pending'::text,
  created_at timestamp with time zone not null default now(),
  updated_at timestamp with time zone not null default now()
);

-- ---------------------------------------------------------------------------
-- Constraints (primary keys, unique keys, checks, foreign keys)
-- ---------------------------------------------------------------------------

alter table awareness_articles add constraint awareness_articles_pkey PRIMARY KEY (id);
alter table blood_requests add constraint blood_requests_blood_group_check CHECK ((blood_group = ANY (ARRAY['A+'::text, 'A-'::text, 'B+'::text, 'B-'::text, 'O+'::text, 'O-'::text, 'AB+'::text, 'AB-'::text])));
alter table blood_requests add constraint blood_requests_component_check CHECK ((component = ANY (ARRAY['WHOLE_BLOOD'::text, 'PRBC'::text, 'PLATELETS'::text, 'PLASMA'::text])));
alter table blood_requests add constraint blood_requests_ocr_confidence_check CHECK (((ocr_confidence >= 0) AND (ocr_confidence <= 100)));
alter table blood_requests add constraint blood_requests_status_check CHECK ((status = ANY (ARRAY['VERIFYING'::text, 'BROADCASTING'::text, 'DONOR_MATCHED'::text, 'EN_ROUTE'::text, 'FULFILLED'::text, 'CLOSED'::text])));
alter table blood_requests add constraint blood_requests_units_required_check CHECK ((units_required > 0));
alter table blood_requests add constraint blood_requests_urgency_check CHECK ((urgency = ANY (ARRAY['HIGH_PRIORITY'::text, 'STANDARD'::text])));
alter table blood_requests add constraint blood_requests_hospital_id_fkey FOREIGN KEY (hospital_id) REFERENCES hospitals(id);
alter table blood_requests add constraint blood_requests_seeker_auth_user_id_fkey FOREIGN KEY (seeker_auth_user_id) REFERENCES auth.users(id);
alter table blood_requests add constraint blood_requests_pkey PRIMARY KEY (id);
alter table campus_drives add constraint campus_drives_status_check CHECK ((status = ANY (ARRAY['Scheduled'::text, 'Open'::text, 'Closed'::text, 'Cancelled'::text])));
alter table campus_drives add constraint campus_drives_target_quota_units_check CHECK ((target_quota_units > 0));
alter table campus_drives add constraint campus_drives_pkey PRIMARY KEY (id);
alter table donor_device_tokens add constraint donor_device_tokens_platform_check CHECK ((platform = 'android'::text));
alter table donor_device_tokens add constraint donor_device_tokens_donor_id_fkey FOREIGN KEY (donor_id) REFERENCES donor_profiles(id) ON DELETE CASCADE;
alter table donor_device_tokens add constraint donor_device_tokens_pkey PRIMARY KEY (id);
alter table donor_device_tokens add constraint donor_device_tokens_token_key UNIQUE (token);
alter table donor_locations add constraint donor_locations_latitude_check CHECK (((latitude >= ('-90'::integer)::double precision) AND (latitude <= (90)::double precision)));
alter table donor_locations add constraint donor_locations_longitude_check CHECK (((longitude >= ('-180'::integer)::double precision) AND (longitude <= (180)::double precision)));
alter table donor_locations add constraint donor_locations_donor_id_fkey FOREIGN KEY (donor_id) REFERENCES donor_profiles(id) ON DELETE CASCADE;
alter table donor_locations add constraint donor_locations_pkey PRIMARY KEY (donor_id);
alter table donor_private_contacts add constraint donor_private_contacts_donor_id_fkey FOREIGN KEY (donor_id) REFERENCES donor_profiles(id) ON DELETE CASCADE;
alter table donor_private_contacts add constraint donor_private_contacts_pkey PRIMARY KEY (donor_id);
alter table donor_profiles add constraint donor_profiles_blood_group_check CHECK ((blood_group = ANY (ARRAY['A+'::text, 'A-'::text, 'B+'::text, 'B-'::text, 'O+'::text, 'O-'::text, 'AB+'::text, 'AB-'::text])));
alter table donor_profiles add constraint donor_profiles_auth_user_id_fkey FOREIGN KEY (auth_user_id) REFERENCES auth.users(id);
alter table donor_profiles add constraint donor_profiles_pkey PRIMARY KEY (id);
alter table drive_attendees add constraint drive_attendees_cnic_status_check CHECK ((cnic_status = ANY (ARRAY['Verified'::text, 'Pending'::text, 'Failed'::text])));
alter table drive_attendees add constraint drive_attendees_pre_screening_status_check CHECK ((pre_screening_status = ANY (ARRAY['Passed'::text, 'Pending'::text, 'Failed'::text])));
alter table drive_attendees add constraint drive_attendees_donor_id_fkey FOREIGN KEY (donor_id) REFERENCES donor_profiles(id) ON DELETE CASCADE;
alter table drive_attendees add constraint drive_attendees_drive_id_fkey FOREIGN KEY (drive_id) REFERENCES campus_drives(id) ON DELETE CASCADE;
alter table drive_attendees add constraint drive_attendees_pkey PRIMARY KEY (id);
alter table drive_attendees add constraint drive_attendees_drive_id_donor_id_key UNIQUE (drive_id, donor_id);
alter table fraud_audit_items add constraint fraud_audit_items_action_status_check CHECK ((action_status = ANY (ARRAY['Flagged'::text, 'Blacklisted'::text, 'Whitelisted'::text])));
alter table fraud_audit_items add constraint fraud_audit_items_ocr_confidence_check CHECK (((ocr_confidence >= 0) AND (ocr_confidence <= 100)));
alter table fraud_audit_items add constraint fraud_audit_items_request_id_fkey FOREIGN KEY (request_id) REFERENCES blood_requests(id) ON DELETE CASCADE;
alter table fraud_audit_items add constraint fraud_audit_items_pkey PRIMARY KEY (id);
alter table hospital_slip_documents add constraint hospital_slip_documents_document_kind_check CHECK ((document_kind = ANY (ARRAY['FRONT'::text, 'BACK'::text])));
alter table hospital_slip_documents add constraint hospital_slip_documents_pkey PRIMARY KEY (id);
alter table hospitals add constraint hospitals_pkey PRIMARY KEY (id);
alter table matched_donor_requests add constraint matched_donor_requests_blood_group_check CHECK ((blood_group = ANY (ARRAY['A+'::text, 'A-'::text, 'B+'::text, 'B-'::text, 'O+'::text, 'O-'::text, 'AB+'::text, 'AB-'::text])));
alter table matched_donor_requests add constraint matched_donor_requests_donor_id_fkey FOREIGN KEY (donor_id) REFERENCES donor_profiles(id) ON DELETE CASCADE;
alter table matched_donor_requests add constraint matched_donor_requests_request_id_fkey FOREIGN KEY (request_id) REFERENCES blood_requests(id) ON DELETE CASCADE;
alter table matched_donor_requests add constraint matched_donor_requests_pkey PRIMARY KEY (id);
alter table matched_donor_requests add constraint matched_donor_requests_request_id_donor_id_key UNIQUE (request_id, donor_id);
alter table pre_screening_answers add constraint pre_screening_answers_donor_id_fkey FOREIGN KEY (donor_id) REFERENCES donor_profiles(id) ON DELETE CASCADE;
alter table pre_screening_answers add constraint pre_screening_answers_pkey PRIMARY KEY (id);
alter table pre_screening_answers add constraint pre_screening_answers_donor_id_key UNIQUE (donor_id);
alter table request_sensitive_data add constraint request_sensitive_data_request_id_fkey FOREIGN KEY (request_id) REFERENCES blood_requests(id) ON DELETE CASCADE;
alter table request_sensitive_data add constraint request_sensitive_data_pkey PRIMARY KEY (request_id);
alter table verification_queue add constraint verification_queue_blood_group_check CHECK ((blood_group = ANY (ARRAY['A+'::text, 'A-'::text, 'B+'::text, 'B-'::text, 'O+'::text, 'O-'::text, 'AB+'::text, 'AB-'::text])));
alter table verification_queue add constraint verification_queue_blood_group_confidence_check CHECK (((blood_group_confidence >= 0) AND (blood_group_confidence <= 100)));
alter table verification_queue add constraint verification_queue_ocr_confidence_check CHECK (((ocr_confidence >= 0) AND (ocr_confidence <= 100)));
alter table verification_queue add constraint verification_queue_status_check CHECK ((status = ANY (ARRAY['Pending'::text, 'Approved'::text, 'Rejected'::text])));
alter table verification_queue add constraint verification_queue_units_check CHECK ((units > 0));
alter table verification_queue add constraint verification_queue_request_id_fkey FOREIGN KEY (request_id) REFERENCES blood_requests(id) ON DELETE CASCADE;
alter table verification_queue add constraint verification_queue_pkey PRIMARY KEY (id);

-- ---------------------------------------------------------------------------
-- Indexes (non-constraint)
-- ---------------------------------------------------------------------------

CREATE INDEX idx_blood_requests_seeker_auth_user_id ON public.blood_requests USING btree (seeker_auth_user_id);
CREATE INDEX idx_donor_device_tokens_donor_id ON public.donor_device_tokens USING btree (donor_id);
CREATE INDEX idx_donor_locations_geography ON public.donor_locations USING gist (location);
CREATE INDEX idx_hospitals_geography ON public.hospitals USING gist (location);

-- ---------------------------------------------------------------------------
-- Row Level Security
-- ---------------------------------------------------------------------------

alter table awareness_articles enable row level security;
alter table blood_requests enable row level security;
alter table campus_drives enable row level security;
alter table donor_device_tokens enable row level security;
alter table donor_locations enable row level security;
alter table donor_private_contacts enable row level security;
alter table donor_profiles enable row level security;
alter table drive_attendees enable row level security;
alter table fraud_audit_items enable row level security;
alter table hospital_slip_documents enable row level security;
alter table hospitals enable row level security;
alter table matched_donor_requests enable row level security;
alter table pre_screening_answers enable row level security;
alter table request_sensitive_data enable row level security;
alter table verification_queue enable row level security;

-- ---------------------------------------------------------------------------
-- Policies
-- Role model: auth.jwt() ->> 'user_role' in
--   (guest, verified_seeker, verified_donor, drive_organizer, admin);
-- anonymous public reads via auth.role() = 'anon'; ownership via auth.uid().
-- Several drive_organizer policies are gated `AND false` (feature not yet live).
-- ---------------------------------------------------------------------------

create policy "awareness_admin_write" on awareness_articles as permissive for all to public
  using (((auth.jwt() ->> 'user_role'::text) = 'admin'::text))
  with check (((auth.jwt() ->> 'user_role'::text) = 'admin'::text));

create policy "awareness_guest_select" on awareness_articles as permissive for select to public
  using (((auth.role() = 'anon'::text) OR ((auth.jwt() ->> 'user_role'::text) = ANY (ARRAY['guest'::text, 'verified_seeker'::text, 'verified_donor'::text, 'drive_organizer'::text, 'admin'::text]))));

create policy "blood_requests_admin_all" on blood_requests as permissive for all to public
  using (((auth.jwt() ->> 'user_role'::text) = 'admin'::text))
  with check (((auth.jwt() ->> 'user_role'::text) = 'admin'::text));

create policy "blood_requests_donor_select" on blood_requests as permissive for select to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'verified_donor'::text) AND (status = 'BROADCASTING'::text)));

create policy "blood_requests_guest_select" on blood_requests as permissive for select to public
  using ((((auth.role() = 'anon'::text) OR ((auth.jwt() ->> 'user_role'::text) = 'guest'::text)) AND (status = 'BROADCASTING'::text)));

create policy "blood_requests_organizer_select" on blood_requests as permissive for select to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'drive_organizer'::text) AND (status = 'BROADCASTING'::text)));

create policy "blood_requests_seeker_insert" on blood_requests as permissive for insert to public
  with check ((((auth.jwt() ->> 'user_role'::text) = 'verified_seeker'::text) AND (seeker_auth_user_id = auth.uid())));

create policy "blood_requests_seeker_select" on blood_requests as permissive for select to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'verified_seeker'::text) AND (seeker_auth_user_id = auth.uid())));

create policy "blood_requests_seeker_update" on blood_requests as permissive for update to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'verified_seeker'::text) AND (seeker_auth_user_id = auth.uid())))
  with check ((seeker_auth_user_id = auth.uid()));

create policy "campus_drives_admin_all" on campus_drives as permissive for all to public
  using (((auth.jwt() ->> 'user_role'::text) = 'admin'::text))
  with check (((auth.jwt() ->> 'user_role'::text) = 'admin'::text));

create policy "campus_drives_organizer_insert" on campus_drives as permissive for insert to public
  with check ((((auth.jwt() ->> 'user_role'::text) = 'drive_organizer'::text) AND false));

create policy "campus_drives_organizer_update" on campus_drives as permissive for update to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'drive_organizer'::text) AND false))
  with check (false);

create policy "campus_drives_public_select" on campus_drives as permissive for select to public
  using (((auth.role() = 'anon'::text) OR ((auth.jwt() ->> 'user_role'::text) = ANY (ARRAY['guest'::text, 'verified_seeker'::text, 'verified_donor'::text, 'drive_organizer'::text, 'admin'::text]))));

create policy "donor_device_tokens_admin_all" on donor_device_tokens as permissive for all to public
  using (((auth.jwt() ->> 'user_role'::text) = 'admin'::text))
  with check (((auth.jwt() ->> 'user_role'::text) = 'admin'::text));

create policy "donor_device_tokens_owner_insert" on donor_device_tokens as permissive for insert to public
  with check ((((auth.jwt() ->> 'user_role'::text) = 'verified_donor'::text) AND (EXISTS ( SELECT 1
   FROM donor_profiles dp
  WHERE ((dp.id = donor_device_tokens.donor_id) AND (dp.auth_user_id = auth.uid()))))));

create policy "donor_device_tokens_owner_select" on donor_device_tokens as permissive for select to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'verified_donor'::text) AND (EXISTS ( SELECT 1
   FROM donor_profiles dp
  WHERE ((dp.id = donor_device_tokens.donor_id) AND (dp.auth_user_id = auth.uid()))))));

create policy "donor_device_tokens_owner_update" on donor_device_tokens as permissive for update to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'verified_donor'::text) AND (EXISTS ( SELECT 1
   FROM donor_profiles dp
  WHERE ((dp.id = donor_device_tokens.donor_id) AND (dp.auth_user_id = auth.uid()))))))
  with check ((EXISTS ( SELECT 1
   FROM donor_profiles dp
  WHERE ((dp.id = donor_device_tokens.donor_id) AND (dp.auth_user_id = auth.uid())))));

create policy "donor_locations_admin_all" on donor_locations as permissive for all to public
  using (((auth.jwt() ->> 'user_role'::text) = 'admin'::text))
  with check (((auth.jwt() ->> 'user_role'::text) = 'admin'::text));

create policy "donor_locations_organizer_select" on donor_locations as permissive for select to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'drive_organizer'::text) AND false));

create policy "donor_locations_owner_select" on donor_locations as permissive for select to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'verified_donor'::text) AND (EXISTS ( SELECT 1
   FROM donor_profiles dp
  WHERE ((dp.id = donor_locations.donor_id) AND (dp.auth_user_id = auth.uid()))))));

create policy "donor_locations_owner_update" on donor_locations as permissive for update to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'verified_donor'::text) AND (EXISTS ( SELECT 1
   FROM donor_profiles dp
  WHERE ((dp.id = donor_locations.donor_id) AND (dp.auth_user_id = auth.uid()))))))
  with check ((EXISTS ( SELECT 1
   FROM donor_profiles dp
  WHERE ((dp.id = donor_locations.donor_id) AND (dp.auth_user_id = auth.uid())))));

create policy "donor_private_contacts_admin_all" on donor_private_contacts as permissive for all to public
  using (((auth.jwt() ->> 'user_role'::text) = 'admin'::text))
  with check (((auth.jwt() ->> 'user_role'::text) = 'admin'::text));

create policy "donor_private_contacts_owner_select" on donor_private_contacts as permissive for select to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'verified_donor'::text) AND (EXISTS ( SELECT 1
   FROM donor_profiles dp
  WHERE ((dp.id = donor_private_contacts.donor_id) AND (dp.auth_user_id = auth.uid()))))));

create policy "donor_private_contacts_owner_update" on donor_private_contacts as permissive for update to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'verified_donor'::text) AND (EXISTS ( SELECT 1
   FROM donor_profiles dp
  WHERE ((dp.id = donor_private_contacts.donor_id) AND (dp.auth_user_id = auth.uid()))))))
  with check ((EXISTS ( SELECT 1
   FROM donor_profiles dp
  WHERE ((dp.id = donor_private_contacts.donor_id) AND (dp.auth_user_id = auth.uid())))));

create policy "donor_profiles_admin_all" on donor_profiles as permissive for all to public
  using (((auth.jwt() ->> 'user_role'::text) = 'admin'::text))
  with check (((auth.jwt() ->> 'user_role'::text) = 'admin'::text));

create policy "donor_profiles_owner_update" on donor_profiles as permissive for update to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'verified_donor'::text) AND (auth_user_id = auth.uid())))
  with check ((auth_user_id = auth.uid()));

create policy "donor_profiles_public_select" on donor_profiles as permissive for select to public
  using (((auth.role() = 'anon'::text) OR ((auth.jwt() ->> 'user_role'::text) = ANY (ARRAY['guest'::text, 'verified_seeker'::text, 'verified_donor'::text, 'drive_organizer'::text, 'admin'::text]))));

create policy "drive_attendees_admin_all" on drive_attendees as permissive for all to public
  using (((auth.jwt() ->> 'user_role'::text) = 'admin'::text))
  with check (((auth.jwt() ->> 'user_role'::text) = 'admin'::text));

create policy "drive_attendees_donor_select" on drive_attendees as permissive for select to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'verified_donor'::text) AND (EXISTS ( SELECT 1
   FROM donor_profiles dp
  WHERE ((dp.id = drive_attendees.donor_id) AND (dp.auth_user_id = auth.uid()))))));

create policy "drive_attendees_organizer_select" on drive_attendees as permissive for select to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'drive_organizer'::text) AND false));

create policy "drive_attendees_organizer_update" on drive_attendees as permissive for update to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'drive_organizer'::text) AND false))
  with check (false);

create policy "fraud_audit_admin_all" on fraud_audit_items as permissive for all to public
  using (((auth.jwt() ->> 'user_role'::text) = 'admin'::text))
  with check (((auth.jwt() ->> 'user_role'::text) = 'admin'::text));

create policy "fraud_audit_organizer_select" on fraud_audit_items as permissive for select to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'drive_organizer'::text) AND false));

create policy "hospital_slips_admin_all" on hospital_slip_documents as permissive for all to public
  using (((auth.jwt() ->> 'user_role'::text) = 'admin'::text))
  with check (((auth.jwt() ->> 'user_role'::text) = 'admin'::text));

create policy "hospital_slips_owner_select" on hospital_slip_documents as permissive for select to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'verified_seeker'::text) AND (EXISTS ( SELECT 1
   FROM blood_requests br
  WHERE ((br.id = hospital_slip_documents.request_id) AND (br.seeker_auth_user_id = auth.uid()))))));

create policy "hospitals_admin_write" on hospitals as permissive for all to public
  using (((auth.jwt() ->> 'user_role'::text) = 'admin'::text))
  with check (((auth.jwt() ->> 'user_role'::text) = 'admin'::text));

create policy "hospitals_guest_select" on hospitals as permissive for select to public
  using (((auth.role() = 'anon'::text) OR ((auth.jwt() ->> 'user_role'::text) = ANY (ARRAY['guest'::text, 'verified_seeker'::text, 'verified_donor'::text, 'drive_organizer'::text, 'admin'::text]))));

create policy "matched_requests_admin_all" on matched_donor_requests as permissive for all to public
  using (((auth.jwt() ->> 'user_role'::text) = 'admin'::text))
  with check (((auth.jwt() ->> 'user_role'::text) = 'admin'::text));

create policy "matched_requests_donor_select" on matched_donor_requests as permissive for select to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'verified_donor'::text) AND (EXISTS ( SELECT 1
   FROM donor_profiles dp
  WHERE ((dp.id = matched_donor_requests.donor_id) AND (dp.auth_user_id = auth.uid()))))));

create policy "matched_requests_organizer_select" on matched_donor_requests as permissive for select to public
  using (((auth.jwt() ->> 'user_role'::text) = 'drive_organizer'::text));

create policy "matched_requests_seeker_select" on matched_donor_requests as permissive for select to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'verified_seeker'::text) AND (EXISTS ( SELECT 1
   FROM blood_requests br
  WHERE ((br.id = matched_donor_requests.request_id) AND (br.seeker_auth_user_id = auth.uid()))))));

create policy "pre_screening_admin_all" on pre_screening_answers as permissive for all to public
  using (((auth.jwt() ->> 'user_role'::text) = 'admin'::text))
  with check (((auth.jwt() ->> 'user_role'::text) = 'admin'::text));

create policy "pre_screening_donor_select" on pre_screening_answers as permissive for select to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'verified_donor'::text) AND (EXISTS ( SELECT 1
   FROM donor_profiles dp
  WHERE ((dp.id = pre_screening_answers.donor_id) AND (dp.auth_user_id = auth.uid()))))));

create policy "pre_screening_donor_update" on pre_screening_answers as permissive for update to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'verified_donor'::text) AND (EXISTS ( SELECT 1
   FROM donor_profiles dp
  WHERE ((dp.id = pre_screening_answers.donor_id) AND (dp.auth_user_id = auth.uid()))))))
  with check ((EXISTS ( SELECT 1
   FROM donor_profiles dp
  WHERE ((dp.id = pre_screening_answers.donor_id) AND (dp.auth_user_id = auth.uid())))));

create policy "pre_screening_organizer_select" on pre_screening_answers as permissive for select to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'drive_organizer'::text) AND false));

create policy "request_sensitive_admin_all" on request_sensitive_data as permissive for all to public
  using (((auth.jwt() ->> 'user_role'::text) = 'admin'::text))
  with check (((auth.jwt() ->> 'user_role'::text) = 'admin'::text));

create policy "request_sensitive_seeker_select" on request_sensitive_data as permissive for select to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'verified_seeker'::text) AND (EXISTS ( SELECT 1
   FROM blood_requests br
  WHERE ((br.id = request_sensitive_data.request_id) AND (br.seeker_auth_user_id = auth.uid()))))));

create policy "verification_queue_admin_all" on verification_queue as permissive for all to public
  using (((auth.jwt() ->> 'user_role'::text) = 'admin'::text))
  with check (((auth.jwt() ->> 'user_role'::text) = 'admin'::text));

create policy "verification_queue_organizer_select" on verification_queue as permissive for select to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'drive_organizer'::text) AND false));

create policy "verification_queue_organizer_update" on verification_queue as permissive for update to public
  using ((((auth.jwt() ->> 'user_role'::text) = 'drive_organizer'::text) AND false))
  with check (false);

-- ---------------------------------------------------------------------------
-- Masked public views (intentionally SECURITY DEFINER — see header note)
-- ---------------------------------------------------------------------------

create or replace view public_donor_directory as
 SELECT id,
    display_name,
    blood_group,
    phone_masked,
    cnic_masked,
    is_available_to_donate,
    is_eligible,
    cooldown_days_remaining,
    lifetime_donations,
    tier,
    district,
    is_cnic_verified
   FROM donor_profiles dp;

create or replace view public_request_feed as
 SELECT id,
    hospital_id,
    blood_group,
    component,
    units_required,
    urgency,
    seeker_name,
    seeker_phone_masked,
    seeker_cnic_masked,
    status,
    created_at,
    active_donors_in_radius,
    responded_donors_count,
    mrn_number,
    ocr_confidence,
    is_verified,
    doctor_stamp_verified
   FROM blood_requests br;

-- ---------------------------------------------------------------------------
-- Donor-matching function (SECURITY DEFINER; EXECUTE hardened by next migration)
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.find_eligible_donors_for_request(p_request_id uuid, p_radius_km double precision)
 RETURNS TABLE(donor_id uuid, display_name text, blood_group text, distance_km double precision, eta_minutes integer, status_text text, phone_masked text, is_verified boolean, lifetime_donations integer)
 LANGUAGE sql
 STABLE SECURITY DEFINER
 SET search_path TO 'public', 'extensions'
AS $function$
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
$function$;
