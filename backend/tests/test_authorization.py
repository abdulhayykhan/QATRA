import pytest
from sqlalchemy.orm import Session
from sqlalchemy import create_engine
from app.db import models
from app.api.deps import CurrentUser
from app.api import authorization

from sqlalchemy.dialects import postgresql

@pytest.fixture
def mock_session(db_session):
    return db_session

def get_compiled_sql(query):
    return str(query.statement.compile(dialect=postgresql.dialect(), compile_kwargs={"literal_binds": True}))

class TestAuthorizationMatrix:
    ROLES = ["guest", "verified_seeker", "verified_donor", "drive_organizer", "admin"]
    USER_ID = "00000000-0000-0000-0000-000000001234"
    OTHER_ID = "99999999-9999-9999-9999-999999999999"

    def _assert_false(self, sql):
        assert "0 = 1" in sql or "false" in sql.lower(), f"Expected false, got {sql}"
        
    def _assert_true(self, sql):
        assert "WHERE" not in sql, f"Expected true (no where clause), got {sql}"

    def test_read_interceptors(self, mock_session):
        # 1. Hospitals (All roles SELECT, admin ALL)
        for role in self.ROLES:
            q = authorization.apply_hospital_policy(mock_session.query(models.Hospital), CurrentUser(id=self.USER_ID, role=role))
            self._assert_true(get_compiled_sql(q))

        # 2. AwarenessArticles (All roles SELECT, admin ALL)
        for role in self.ROLES:
            q = authorization.apply_awareness_article_policy(mock_session.query(models.AwarenessArticle), CurrentUser(id=self.USER_ID, role=role))
            self._assert_true(get_compiled_sql(q))

        # 3. DonorProfiles (All roles SELECT)
        for role in self.ROLES:
            q = authorization.apply_donor_profile_policy(mock_session.query(models.DonorProfile), CurrentUser(id=self.USER_ID, role=role))
            self._assert_true(get_compiled_sql(q))

        # 4. DonorLocations (Donor own, Admin all)
        for role in self.ROLES:
            q = authorization.apply_donor_location_policy(mock_session.query(models.DonorLocation), CurrentUser(id=self.USER_ID, role=role))
            sql = get_compiled_sql(q)
            if role == "admin":
                self._assert_true(sql)
            elif role == "verified_donor":
                assert "JOIN donor_profiles" in sql and f"donor_profiles.auth_user_id = '{self.USER_ID}'" in sql
            else:
                self._assert_false(sql)

        # 5. DonorDeviceTokens (Donor own, Admin all)
        for role in self.ROLES:
            q = authorization.apply_donor_device_token_policy(mock_session.query(models.DonorDeviceToken), CurrentUser(id=self.USER_ID, role=role))
            sql = get_compiled_sql(q)
            if role == "admin":
                self._assert_true(sql)
            elif role == "verified_donor":
                assert "JOIN donor_profiles" in sql and f"donor_profiles.auth_user_id = '{self.USER_ID}'" in sql
            else:
                self._assert_false(sql)

        # 6. DonorPrivateContacts (Donor own, Admin all)
        for role in self.ROLES:
            q = authorization.apply_donor_private_contact_policy(mock_session.query(models.DonorPrivateContact), CurrentUser(id=self.USER_ID, role=role))
            sql = get_compiled_sql(q)
            if role == "admin":
                self._assert_true(sql)
            elif role == "verified_donor":
                assert "JOIN donor_profiles" in sql and f"donor_profiles.auth_user_id = '{self.USER_ID}'" in sql
            else:
                self._assert_false(sql)

        # 7. HospitalSlipDocuments (Seeker own, Admin all)
        for role in self.ROLES:
            q = authorization.apply_hospital_slip_document_policy(mock_session.query(models.HospitalSlipDocument), CurrentUser(id=self.USER_ID, role=role))
            sql = get_compiled_sql(q)
            if role == "admin":
                self._assert_true(sql)
            elif role == "verified_seeker":
                assert "JOIN blood_requests" in sql and f"blood_requests.seeker_auth_user_id = '{self.USER_ID}'" in sql
            else:
                self._assert_false(sql)

        # 8. BloodRequests (Seeker own, Guest/Donor/Org BROADCASTING, Admin all)
        for role in self.ROLES:
            q = authorization.apply_blood_request_policy(mock_session.query(models.BloodRequest), CurrentUser(id=self.USER_ID, role=role))
            sql = get_compiled_sql(q)
            if role == "admin":
                self._assert_true(sql)
            elif role == "verified_seeker":
                assert f"blood_requests.seeker_auth_user_id = '{self.USER_ID}'" in sql
            else:
                assert "blood_requests.status = 'BROADCASTING'" in sql

        # 9. RequestSensitiveData (Seeker own, Admin all)
        for role in self.ROLES:
            q = authorization.apply_request_sensitive_data_policy(mock_session.query(models.RequestSensitiveData), CurrentUser(id=self.USER_ID, role=role))
            sql = get_compiled_sql(q)
            if role == "admin":
                self._assert_true(sql)
            elif role == "verified_seeker":
                assert "JOIN blood_requests" in sql and f"blood_requests.seeker_auth_user_id = '{self.USER_ID}'" in sql
            else:
                self._assert_false(sql)

        # 10. MatchedDonorRequests (Seeker own, Donor own, Org all, Admin all)
        for role in self.ROLES:
            q = authorization.apply_matched_donor_request_policy(mock_session.query(models.MatchedDonorRequest), CurrentUser(id=self.USER_ID, role=role))
            sql = get_compiled_sql(q)
            if role in ["admin", "drive_organizer"]:
                self._assert_true(sql)
            elif role == "verified_seeker":
                assert "JOIN blood_requests" in sql and f"blood_requests.seeker_auth_user_id = '{self.USER_ID}'" in sql
            elif role == "verified_donor":
                assert "JOIN donor_profiles" in sql and f"donor_profiles.auth_user_id = '{self.USER_ID}'" in sql
            else:
                self._assert_false(sql)

        # 11. VerificationQueue (Admin only)
        for role in self.ROLES:
            q = authorization.apply_verification_queue_policy(mock_session.query(models.VerificationQueue), CurrentUser(id=self.USER_ID, role=role))
            sql = get_compiled_sql(q)
            if role == "admin":
                self._assert_true(sql)
            else:
                self._assert_false(sql)

        # 12. FraudAuditItems (Admin only)
        for role in self.ROLES:
            q = authorization.apply_fraud_audit_item_policy(mock_session.query(models.FraudAuditItem), CurrentUser(id=self.USER_ID, role=role))
            sql = get_compiled_sql(q)
            if role == "admin":
                self._assert_true(sql)
            else:
                self._assert_false(sql)

        # 13. CampusDrives (All roles SELECT)
        for role in self.ROLES:
            q = authorization.apply_campus_drive_policy(mock_session.query(models.CampusDrive), CurrentUser(id=self.USER_ID, role=role))
            self._assert_true(get_compiled_sql(q))

        # 14. DriveAttendees (Donor own, Admin all)
        for role in self.ROLES:
            q = authorization.apply_drive_attendee_policy(mock_session.query(models.DriveAttendee), CurrentUser(id=self.USER_ID, role=role))
            sql = get_compiled_sql(q)
            if role == "admin":
                self._assert_true(sql)
            elif role == "verified_donor":
                assert "JOIN donor_profiles" in sql and f"donor_profiles.auth_user_id = '{self.USER_ID}'" in sql
            else:
                self._assert_false(sql)

        # 15. PreScreeningAnswers (Donor own, Admin all)
        for role in self.ROLES:
            q = authorization.apply_pre_screening_answer_policy(mock_session.query(models.PreScreeningAnswer), CurrentUser(id=self.USER_ID, role=role))
            sql = get_compiled_sql(q)
            if role == "admin":
                self._assert_true(sql)
            elif role == "verified_donor":
                assert "JOIN donor_profiles" in sql and f"donor_profiles.auth_user_id = '{self.USER_ID}'" in sql
            else:
                self._assert_false(sql)

    def test_write_validators(self):
        # Construct mock instances
        dp_own = models.DonorProfile(auth_user_id=self.USER_ID)
        dp_other = models.DonorProfile(auth_user_id=self.OTHER_ID)
        br_own = models.BloodRequest(seeker_auth_user_id=self.USER_ID)
        br_other = models.BloodRequest(seeker_auth_user_id=self.OTHER_ID)
        
        # 1. Hospital (Admin only)
        for role in self.ROLES:
            res = authorization.verify_hospital_write(models.Hospital(), CurrentUser(id=self.USER_ID, role=role))
            assert res is (role == "admin")

        # 2. AwarenessArticle (Admin only)
        for role in self.ROLES:
            res = authorization.verify_awareness_article_write(models.AwarenessArticle(), CurrentUser(id=self.USER_ID, role=role))
            assert res is (role == "admin")

        # 3. DonorProfile (Donor own, Admin all)
        for role in self.ROLES:
            assert authorization.verify_donor_profile_write(dp_own, CurrentUser(id=self.USER_ID, role=role)) is (role in ["admin", "verified_donor"])
            assert authorization.verify_donor_profile_write(dp_other, CurrentUser(id=self.USER_ID, role=role)) is (role == "admin")

        # 4-6. DonorLocation, DeviceToken, PrivateContact (Donor own, Admin all)
        for model_cls, verify_func in [
            (models.DonorLocation, authorization.verify_donor_location_write),
            (models.DonorDeviceToken, authorization.verify_donor_device_token_write),
            (models.DonorPrivateContact, authorization.verify_donor_private_contact_write)
        ]:
            for role in self.ROLES:
                assert verify_func(model_cls(donor=dp_own), CurrentUser(id=self.USER_ID, role=role)) is (role in ["admin", "verified_donor"])
                assert verify_func(model_cls(donor=dp_other), CurrentUser(id=self.USER_ID, role=role)) is (role == "admin")
                assert verify_func(model_cls(donor=None), CurrentUser(id=self.USER_ID, role=role)) is (role == "admin")

        # 7. HospitalSlipDocument (Seeker own, Admin all)
        for role in self.ROLES:
            assert authorization.verify_hospital_slip_document_write(models.HospitalSlipDocument(request=br_own), CurrentUser(id=self.USER_ID, role=role)) is (role in ["admin", "verified_seeker"])
            assert authorization.verify_hospital_slip_document_write(models.HospitalSlipDocument(request=br_other), CurrentUser(id=self.USER_ID, role=role)) is (role == "admin")

        # 8. BloodRequest (Seeker own, Admin all)
        for role in self.ROLES:
            assert authorization.verify_blood_request_write(br_own, CurrentUser(id=self.USER_ID, role=role)) is (role in ["admin", "verified_seeker"])
            assert authorization.verify_blood_request_write(br_other, CurrentUser(id=self.USER_ID, role=role)) is (role == "admin")

        # 9. RequestSensitiveData (Seeker own, Admin all)
        for role in self.ROLES:
            assert authorization.verify_request_sensitive_data_write(models.RequestSensitiveData(request=br_own), CurrentUser(id=self.USER_ID, role=role)) is (role in ["admin", "verified_seeker"])
            assert authorization.verify_request_sensitive_data_write(models.RequestSensitiveData(request=br_other), CurrentUser(id=self.USER_ID, role=role)) is (role == "admin")

        # 10. MatchedDonorRequest (Admin only)
        for role in self.ROLES:
            res = authorization.verify_matched_donor_request_write(models.MatchedDonorRequest(), CurrentUser(id=self.USER_ID, role=role))
            assert res is (role == "admin")

        # 11-14. VerificationQueue, FraudAuditItem, CampusDrive, DriveAttendee (Admin only)
        for model_cls, verify_func in [
            (models.VerificationQueue, authorization.verify_verification_queue_write),
            (models.FraudAuditItem, authorization.verify_fraud_audit_item_write),
            (models.CampusDrive, authorization.verify_campus_drive_write),
            (models.DriveAttendee, authorization.verify_drive_attendee_write)
        ]:
            for role in self.ROLES:
                assert verify_func(model_cls(), CurrentUser(id=self.USER_ID, role=role)) is (role == "admin")

        # 15. PreScreeningAnswer (Donor own, Admin all)
        for role in self.ROLES:
            assert authorization.verify_pre_screening_answer_write(models.PreScreeningAnswer(donor=dp_own), CurrentUser(id=self.USER_ID, role=role)) is (role in ["admin", "verified_donor"])
            assert authorization.verify_pre_screening_answer_write(models.PreScreeningAnswer(donor=dp_other), CurrentUser(id=self.USER_ID, role=role)) is (role == "admin")

