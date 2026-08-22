-- QATRA Prompt 9 RLS migration.
-- Role source: auth.jwt() ->> 'user_role'. Ownership uses auth.uid().
-- Apply docs/qatra_ownership_columns.sql before this file.
-- Calling-specific policies are intentionally excluded from this migration.

-- Enable RLS without dropping or altering any policies already present in the project.
ALTER TABLE public.hospitals ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.awareness_articles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.donor_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.donor_locations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.donor_device_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.donor_private_contacts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.hospital_slip_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.blood_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.request_sensitive_data ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.matched_donor_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.verification_queue ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.fraud_audit_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.campus_drives ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.drive_attendees ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pre_screening_answers ENABLE ROW LEVEL SECURITY;

-- Guest/read-only public lookup tables.
CREATE POLICY hospitals_guest_select ON public.hospitals FOR SELECT USING (auth.role() = 'anon' OR (auth.jwt() ->> 'user_role') IN ('guest', 'verified_seeker', 'verified_donor', 'drive_organizer', 'admin'));
CREATE POLICY hospitals_admin_write ON public.hospitals FOR ALL USING ((auth.jwt() ->> 'user_role') = 'admin') WITH CHECK ((auth.jwt() ->> 'user_role') = 'admin');

CREATE POLICY awareness_guest_select ON public.awareness_articles FOR SELECT USING (auth.role() = 'anon' OR (auth.jwt() ->> 'user_role') IN ('guest', 'verified_seeker', 'verified_donor', 'drive_organizer', 'admin'));
CREATE POLICY awareness_admin_write ON public.awareness_articles FOR ALL USING ((auth.jwt() ->> 'user_role') = 'admin') WITH CHECK ((auth.jwt() ->> 'user_role') = 'admin');

-- Donor public profile and own profile maintenance.
CREATE POLICY donor_profiles_public_select ON public.donor_profiles FOR SELECT USING (auth.role() = 'anon' OR (auth.jwt() ->> 'user_role') IN ('guest', 'verified_seeker', 'verified_donor', 'drive_organizer', 'admin'));
CREATE POLICY donor_profiles_owner_update ON public.donor_profiles FOR UPDATE USING ((auth.jwt() ->> 'user_role') = 'verified_donor' AND auth_user_id = auth.uid()) WITH CHECK (auth_user_id = auth.uid());
CREATE POLICY donor_profiles_admin_all ON public.donor_profiles FOR ALL USING ((auth.jwt() ->> 'user_role') = 'admin') WITH CHECK ((auth.jwt() ->> 'user_role') = 'admin');

CREATE POLICY donor_locations_owner_select ON public.donor_locations FOR SELECT USING ((auth.jwt() ->> 'user_role') = 'verified_donor' AND EXISTS (SELECT 1 FROM public.donor_profiles dp WHERE dp.id = donor_id AND dp.auth_user_id = auth.uid()));
CREATE POLICY donor_locations_owner_update ON public.donor_locations FOR UPDATE USING ((auth.jwt() ->> 'user_role') = 'verified_donor' AND EXISTS (SELECT 1 FROM public.donor_profiles dp WHERE dp.id = donor_id AND dp.auth_user_id = auth.uid())) WITH CHECK (EXISTS (SELECT 1 FROM public.donor_profiles dp WHERE dp.id = donor_id AND dp.auth_user_id = auth.uid()));
CREATE POLICY donor_locations_organizer_select ON public.donor_locations FOR SELECT USING ((auth.jwt() ->> 'user_role') = 'drive_organizer' AND false);
CREATE POLICY donor_locations_admin_all ON public.donor_locations FOR ALL USING ((auth.jwt() ->> 'user_role') = 'admin') WITH CHECK ((auth.jwt() ->> 'user_role') = 'admin');

CREATE POLICY donor_device_tokens_owner_select ON public.donor_device_tokens FOR SELECT USING ((auth.jwt() ->> 'user_role') = 'verified_donor' AND EXISTS (SELECT 1 FROM public.donor_profiles dp WHERE dp.id = donor_id AND dp.auth_user_id = auth.uid()));
CREATE POLICY donor_device_tokens_owner_insert ON public.donor_device_tokens FOR INSERT WITH CHECK ((auth.jwt() ->> 'user_role') = 'verified_donor' AND EXISTS (SELECT 1 FROM public.donor_profiles dp WHERE dp.id = donor_id AND dp.auth_user_id = auth.uid()));
CREATE POLICY donor_device_tokens_owner_update ON public.donor_device_tokens FOR UPDATE USING ((auth.jwt() ->> 'user_role') = 'verified_donor' AND EXISTS (SELECT 1 FROM public.donor_profiles dp WHERE dp.id = donor_id AND dp.auth_user_id = auth.uid())) WITH CHECK (EXISTS (SELECT 1 FROM public.donor_profiles dp WHERE dp.id = donor_id AND dp.auth_user_id = auth.uid()));
CREATE POLICY donor_device_tokens_admin_all ON public.donor_device_tokens FOR ALL USING ((auth.jwt() ->> 'user_role') = 'admin') WITH CHECK ((auth.jwt() ->> 'user_role') = 'admin');

-- Sensitive donor contacts: no guest/seeker/organizer policy exists by design.
CREATE POLICY donor_private_contacts_owner_select ON public.donor_private_contacts FOR SELECT USING ((auth.jwt() ->> 'user_role') = 'verified_donor' AND EXISTS (SELECT 1 FROM public.donor_profiles dp WHERE dp.id = donor_id AND dp.auth_user_id = auth.uid()));
CREATE POLICY donor_private_contacts_owner_update ON public.donor_private_contacts FOR UPDATE USING ((auth.jwt() ->> 'user_role') = 'verified_donor' AND EXISTS (SELECT 1 FROM public.donor_profiles dp WHERE dp.id = donor_id AND dp.auth_user_id = auth.uid())) WITH CHECK (EXISTS (SELECT 1 FROM public.donor_profiles dp WHERE dp.id = donor_id AND dp.auth_user_id = auth.uid()));
CREATE POLICY donor_private_contacts_admin_all ON public.donor_private_contacts FOR ALL USING ((auth.jwt() ->> 'user_role') = 'admin') WITH CHECK ((auth.jwt() ->> 'user_role') = 'admin');

-- Slip metadata: seeker ownership is represented by blood_requests.seeker_auth_user_id.
CREATE POLICY hospital_slips_owner_select ON public.hospital_slip_documents FOR SELECT USING ((auth.jwt() ->> 'user_role') = 'verified_seeker' AND EXISTS (SELECT 1 FROM public.blood_requests br WHERE br.id = request_id AND br.seeker_auth_user_id = auth.uid()));
CREATE POLICY hospital_slips_admin_all ON public.hospital_slip_documents FOR ALL USING ((auth.jwt() ->> 'user_role') = 'admin') WITH CHECK ((auth.jwt() ->> 'user_role') = 'admin');

-- Blood requests.
CREATE POLICY blood_requests_guest_select ON public.blood_requests FOR SELECT USING ((auth.role() = 'anon' OR (auth.jwt() ->> 'user_role') = 'guest') AND status = 'BROADCASTING');
CREATE POLICY blood_requests_seeker_select ON public.blood_requests FOR SELECT USING ((auth.jwt() ->> 'user_role') = 'verified_seeker' AND seeker_auth_user_id = auth.uid());
CREATE POLICY blood_requests_seeker_insert ON public.blood_requests FOR INSERT WITH CHECK ((auth.jwt() ->> 'user_role') = 'verified_seeker' AND seeker_auth_user_id = auth.uid());
CREATE POLICY blood_requests_seeker_update ON public.blood_requests FOR UPDATE USING ((auth.jwt() ->> 'user_role') = 'verified_seeker' AND seeker_auth_user_id = auth.uid()) WITH CHECK (seeker_auth_user_id = auth.uid());
CREATE POLICY blood_requests_donor_select ON public.blood_requests FOR SELECT USING ((auth.jwt() ->> 'user_role') = 'verified_donor' AND status = 'BROADCASTING');
-- Organizer access is not intended to expose every request. The reviewed stub says
-- coordination visibility only, and no organizer ownership/scope column exists yet.
-- Until that scope exists, expose only live BROADCASTING requests and keep writes absent.
CREATE POLICY blood_requests_organizer_select ON public.blood_requests FOR SELECT USING ((auth.jwt() ->> 'user_role') = 'drive_organizer' AND status = 'BROADCASTING');
CREATE POLICY blood_requests_admin_all ON public.blood_requests FOR ALL USING ((auth.jwt() ->> 'user_role') = 'admin') WITH CHECK ((auth.jwt() ->> 'user_role') = 'admin');

-- Sensitive request data: seeker ownership only; no donor/organizer access.
CREATE POLICY request_sensitive_seeker_select ON public.request_sensitive_data FOR SELECT USING ((auth.jwt() ->> 'user_role') = 'verified_seeker' AND EXISTS (SELECT 1 FROM public.blood_requests br WHERE br.id = request_id AND br.seeker_auth_user_id = auth.uid()));
CREATE POLICY request_sensitive_admin_all ON public.request_sensitive_data FOR ALL USING ((auth.jwt() ->> 'user_role') = 'admin') WITH CHECK ((auth.jwt() ->> 'user_role') = 'admin');

CREATE POLICY matched_requests_seeker_select ON public.matched_donor_requests FOR SELECT USING ((auth.jwt() ->> 'user_role') = 'verified_seeker' AND EXISTS (SELECT 1 FROM public.blood_requests br WHERE br.id = request_id AND br.seeker_auth_user_id = auth.uid()));
CREATE POLICY matched_requests_donor_select ON public.matched_donor_requests FOR SELECT USING ((auth.jwt() ->> 'user_role') = 'verified_donor' AND EXISTS (SELECT 1 FROM public.donor_profiles dp WHERE dp.id = donor_id AND dp.auth_user_id = auth.uid()));
CREATE POLICY matched_requests_organizer_select ON public.matched_donor_requests FOR SELECT USING ((auth.jwt() ->> 'user_role') = 'drive_organizer');
CREATE POLICY matched_requests_admin_all ON public.matched_donor_requests FOR ALL USING ((auth.jwt() ->> 'user_role') = 'admin') WITH CHECK ((auth.jwt() ->> 'user_role') = 'admin');

-- Operational/admin tables. Assignment/drive ownership columns are not present in the reviewed schema,
-- so organizer policies are deny-by-default rather than accidentally granting all organizer rows.
CREATE POLICY verification_queue_organizer_select ON public.verification_queue FOR SELECT USING ((auth.jwt() ->> 'user_role') = 'drive_organizer' AND false);
CREATE POLICY verification_queue_organizer_update ON public.verification_queue FOR UPDATE USING ((auth.jwt() ->> 'user_role') = 'drive_organizer' AND false) WITH CHECK (false);
CREATE POLICY verification_queue_admin_all ON public.verification_queue FOR ALL USING ((auth.jwt() ->> 'user_role') = 'admin') WITH CHECK ((auth.jwt() ->> 'user_role') = 'admin');
CREATE POLICY fraud_audit_organizer_select ON public.fraud_audit_items FOR SELECT USING ((auth.jwt() ->> 'user_role') = 'drive_organizer' AND false);
CREATE POLICY fraud_audit_admin_all ON public.fraud_audit_items FOR ALL USING ((auth.jwt() ->> 'user_role') = 'admin') WITH CHECK ((auth.jwt() ->> 'user_role') = 'admin');

CREATE POLICY campus_drives_public_select ON public.campus_drives FOR SELECT USING (auth.role() = 'anon' OR (auth.jwt() ->> 'user_role') IN ('guest', 'verified_seeker', 'verified_donor', 'drive_organizer', 'admin'));
CREATE POLICY campus_drives_organizer_insert ON public.campus_drives FOR INSERT WITH CHECK ((auth.jwt() ->> 'user_role') = 'drive_organizer' AND false);
CREATE POLICY campus_drives_organizer_update ON public.campus_drives FOR UPDATE USING ((auth.jwt() ->> 'user_role') = 'drive_organizer' AND false) WITH CHECK (false);
CREATE POLICY campus_drives_admin_all ON public.campus_drives FOR ALL USING ((auth.jwt() ->> 'user_role') = 'admin') WITH CHECK ((auth.jwt() ->> 'user_role') = 'admin');

CREATE POLICY drive_attendees_donor_select ON public.drive_attendees FOR SELECT USING ((auth.jwt() ->> 'user_role') = 'verified_donor' AND EXISTS (SELECT 1 FROM public.donor_profiles dp WHERE dp.id = donor_id AND dp.auth_user_id = auth.uid()));
CREATE POLICY drive_attendees_organizer_select ON public.drive_attendees FOR SELECT USING ((auth.jwt() ->> 'user_role') = 'drive_organizer' AND false);
CREATE POLICY drive_attendees_organizer_update ON public.drive_attendees FOR UPDATE USING ((auth.jwt() ->> 'user_role') = 'drive_organizer' AND false) WITH CHECK (false);
CREATE POLICY drive_attendees_admin_all ON public.drive_attendees FOR ALL USING ((auth.jwt() ->> 'user_role') = 'admin') WITH CHECK ((auth.jwt() ->> 'user_role') = 'admin');

CREATE POLICY pre_screening_donor_select ON public.pre_screening_answers FOR SELECT USING ((auth.jwt() ->> 'user_role') = 'verified_donor' AND EXISTS (SELECT 1 FROM public.donor_profiles dp WHERE dp.id = donor_id AND dp.auth_user_id = auth.uid()));
CREATE POLICY pre_screening_donor_update ON public.pre_screening_answers FOR UPDATE USING ((auth.jwt() ->> 'user_role') = 'verified_donor' AND EXISTS (SELECT 1 FROM public.donor_profiles dp WHERE dp.id = donor_id AND dp.auth_user_id = auth.uid())) WITH CHECK (EXISTS (SELECT 1 FROM public.donor_profiles dp WHERE dp.id = donor_id AND dp.auth_user_id = auth.uid()));
CREATE POLICY pre_screening_organizer_select ON public.pre_screening_answers FOR SELECT USING ((auth.jwt() ->> 'user_role') = 'drive_organizer' AND false);
CREATE POLICY pre_screening_admin_all ON public.pre_screening_answers FOR ALL USING ((auth.jwt() ->> 'user_role') = 'admin') WITH CHECK ((auth.jwt() ->> 'user_role') = 'admin');