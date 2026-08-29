import uuid
from typing import Any
from sqlalchemy.orm import Query
from sqlalchemy import false
from app.db import models
from app.api.deps import CurrentUser

# --- Read Interceptors ---

def apply_hospital_policy(query: Query, user: CurrentUser) -> Query:
    if user.role in ["guest", "verified_seeker", "verified_donor", "drive_organizer", "admin"]:
        return query
    return query.filter(false())

def apply_awareness_article_policy(query: Query, user: CurrentUser) -> Query:
    if user.role in ["guest", "verified_seeker", "verified_donor", "drive_organizer", "admin"]:
        return query
    return query.filter(false())

def apply_donor_profile_policy(query: Query, user: CurrentUser) -> Query:
    if user.role in ["guest", "verified_seeker", "verified_donor", "drive_organizer", "admin"]:
        return query
    return query.filter(false())

def apply_donor_location_policy(query: Query, user: CurrentUser) -> Query:
    if user.role == "admin":
        return query
    if user.role == "verified_donor":
        return query.join(models.DonorProfile, models.DonorLocation.donor_id == models.DonorProfile.id).filter(models.DonorProfile.auth_user_id == user.id)
    return query.filter(false())

def apply_donor_device_token_policy(query: Query, user: CurrentUser) -> Query:
    if user.role == "admin":
        return query
    if user.role == "verified_donor":
        return query.join(models.DonorProfile, models.DonorDeviceToken.donor_id == models.DonorProfile.id).filter(models.DonorProfile.auth_user_id == user.id)
    return query.filter(false())

def apply_donor_private_contact_policy(query: Query, user: CurrentUser) -> Query:
    if user.role == "admin":
        return query
    if user.role == "verified_donor":
        return query.join(models.DonorProfile, models.DonorPrivateContact.donor_id == models.DonorProfile.id).filter(models.DonorProfile.auth_user_id == user.id)
    return query.filter(false())

def apply_hospital_slip_document_policy(query: Query, user: CurrentUser) -> Query:
    if user.role == "admin":
        return query
    if user.role == "verified_seeker":
        return query.join(models.BloodRequest, models.HospitalSlipDocument.request_id == models.BloodRequest.id).filter(models.BloodRequest.seeker_auth_user_id == user.id)
    return query.filter(false())

def apply_blood_request_policy(query: Query, user: CurrentUser) -> Query:
    if user.role == "admin":
        return query
    if user.role == "verified_seeker":
        return query.filter(models.BloodRequest.seeker_auth_user_id == user.id)
    if user.role in ["guest", "verified_donor", "drive_organizer"]:
        return query.filter(models.BloodRequest.status == "BROADCASTING")
    return query.filter(false())

def apply_request_sensitive_data_policy(query: Query, user: CurrentUser) -> Query:
    if user.role == "admin":
        return query
    if user.role == "verified_seeker":
        return query.join(models.BloodRequest, models.RequestSensitiveData.request_id == models.BloodRequest.id).filter(models.BloodRequest.seeker_auth_user_id == user.id)
    return query.filter(false())

def apply_matched_donor_request_policy(query: Query, user: CurrentUser) -> Query:
    if user.role == "admin" or user.role == "drive_organizer":
        return query
    if user.role == "verified_seeker":
        return query.join(models.BloodRequest, models.MatchedDonorRequest.request_id == models.BloodRequest.id).filter(models.BloodRequest.seeker_auth_user_id == user.id)
    if user.role == "verified_donor":
        return query.join(models.DonorProfile, models.MatchedDonorRequest.donor_id == models.DonorProfile.id).filter(models.DonorProfile.auth_user_id == user.id)
    return query.filter(false())

def apply_verification_queue_policy(query: Query, user: CurrentUser) -> Query:
    if user.role == "admin":
        return query
    return query.filter(false())

def apply_fraud_audit_item_policy(query: Query, user: CurrentUser) -> Query:
    if user.role == "admin":
        return query
    return query.filter(false())

def apply_campus_drive_policy(query: Query, user: CurrentUser) -> Query:
    if user.role in ["guest", "verified_seeker", "verified_donor", "drive_organizer", "admin"]:
        return query
    return query.filter(false())

def apply_drive_attendee_policy(query: Query, user: CurrentUser) -> Query:
    if user.role == "admin":
        return query
    if user.role == "verified_donor":
        return query.join(models.DonorProfile, models.DriveAttendee.donor_id == models.DonorProfile.id).filter(models.DonorProfile.auth_user_id == user.id)
    return query.filter(false())

def apply_pre_screening_answer_policy(query: Query, user: CurrentUser) -> Query:
    if user.role == "admin":
        return query
    if user.role == "verified_donor":
        return query.join(models.DonorProfile, models.PreScreeningAnswer.donor_id == models.DonorProfile.id).filter(models.DonorProfile.auth_user_id == user.id)
    return query.filter(false())

def apply_request_feedback_policy(query: Query, user: CurrentUser) -> Query:
    if user.role == "admin":
        return query
    if user.role == "verified_seeker":
        return query.join(models.BloodRequest, models.RequestFeedback.request_id == models.BloodRequest.id).filter(models.BloodRequest.seeker_auth_user_id == user.id)
    if user.role == "verified_donor":
        return query.join(models.MatchedDonorRequest, models.RequestFeedback.request_id == models.MatchedDonorRequest.request_id).join(models.DonorProfile, models.MatchedDonorRequest.donor_id == models.DonorProfile.id).filter(models.DonorProfile.auth_user_id == user.id)
    return query.filter(false())

# --- Write Validators ---

def verify_hospital_write(record: models.Hospital, user: CurrentUser) -> bool:
    return user.role == "admin"

def verify_awareness_article_write(record: models.AwarenessArticle, user: CurrentUser) -> bool:
    return user.role == "admin"

def verify_donor_profile_write(record: models.DonorProfile, user: CurrentUser) -> bool:
    if user.role == "admin":
        return True
    if user.role == "verified_donor":
        return str(record.auth_user_id) == user.id
    return False

def verify_donor_location_write(record: models.DonorLocation, user: CurrentUser) -> bool:
    if user.role == "admin":
        return True
    if user.role == "verified_donor" and record.donor and str(record.donor.auth_user_id) == user.id:
        return True
    return False

def verify_donor_device_token_write(record: models.DonorDeviceToken, user: CurrentUser) -> bool:
    if user.role == "admin":
        return True
    if user.role == "verified_donor" and record.donor and str(record.donor.auth_user_id) == user.id:
        return True
    return False

def verify_donor_private_contact_write(record: models.DonorPrivateContact, user: CurrentUser) -> bool:
    if user.role == "admin":
        return True
    if user.role == "verified_donor" and record.donor and str(record.donor.auth_user_id) == user.id:
        return True
    return False

def verify_hospital_slip_document_write(record: models.HospitalSlipDocument, user: CurrentUser) -> bool:
    if user.role == "admin":
        return True
    if user.role == "verified_seeker" and record.request and str(record.request.seeker_auth_user_id) == user.id:
        return True
    return False

def verify_blood_request_write(record: models.BloodRequest, user: CurrentUser) -> bool:
    if user.role == "admin":
        return True
    if user.role == "verified_seeker":
        return str(record.seeker_auth_user_id) == user.id
    return False

def verify_request_sensitive_data_write(record: models.RequestSensitiveData, user: CurrentUser) -> bool:
    if user.role == "admin":
        return True
    if user.role == "verified_seeker" and record.request and str(record.request.seeker_auth_user_id) == user.id:
        return True
    return False

def verify_matched_donor_request_write(record: models.MatchedDonorRequest, user: CurrentUser) -> bool:
    return user.role == "admin"

def verify_verification_queue_write(record: models.VerificationQueue, user: CurrentUser) -> bool:
    return user.role == "admin"

def verify_fraud_audit_item_write(record: models.FraudAuditItem, user: CurrentUser) -> bool:
    return user.role == "admin"

def verify_campus_drive_write(record: models.CampusDrive, user: CurrentUser) -> bool:
    return user.role == "admin"

def verify_drive_attendee_write(record: models.DriveAttendee, user: CurrentUser) -> bool:
    if user.role == "admin":
        return True
    if user.role == "verified_donor" and record.donor and str(record.donor.auth_user_id) == user.id:
        return True
    return False

def verify_pre_screening_answer_write(record: models.PreScreeningAnswer, user: CurrentUser) -> bool:
    if user.role == "admin":
        return True
    if user.role == "verified_donor" and record.donor and str(record.donor.auth_user_id) == user.id:
        return True
    return False

def verify_request_feedback_write(record: models.RequestFeedback, user: CurrentUser) -> bool:
    if user.role == "admin":
        return True
    if user.role == "verified_seeker" and record.request and str(record.request.seeker_auth_user_id) == user.id:
        return True
    return False

def verify_admin_only_write(record: Any, user: CurrentUser) -> bool:
    return user.role == "admin"

def verify_donor_cnic_document_write(record: models.DonorCnicDocument, user: CurrentUser) -> bool:
    if user.role == "admin":
        return True
    if user.role == "verified_donor" and record.donor and str(record.donor.auth_user_id) == user.id:
        return True
    return False

def verify_matched_donors_read(request: models.BloodRequest, user: CurrentUser) -> bool:
    if user.role == "admin":
        return True
    if user.role == "verified_seeker" and str(request.seeker_auth_user_id) == user.id:
        return True
    return False

def verify_seeker_or_admin(user: CurrentUser) -> bool:
    return user.role in ["verified_seeker", "admin"]

def verify_contact_visibility(request_id: uuid.UUID, target_user_id: uuid.UUID, user: CurrentUser, db) -> bool:
    """
    Strict RLS-equivalent policy for fetching phone numbers:
    - Admin can always view.
    - If user is Seeker, target must be an active Donor matched on this request.
    - If user is Donor, target must be the Seeker who owns this request.
    - Overall BloodRequest status must NOT be FULFILLED or CANCELLED.
    """
    if user.role == "admin":
        return True
        
    request = db.query(models.BloodRequest).filter(models.BloodRequest.id == request_id).first()
    if not request or request.status in ["FULFILLED", "CANCELLED"]:
        return False

    if user.role == "verified_seeker":
        # Current user must be the seeker
        if str(request.seeker_auth_user_id) != user.id:
            return False
            
        # Target must be a matched donor
        target_donor = db.query(models.DonorProfile).filter(models.DonorProfile.auth_user_id == target_user_id).first()
        if not target_donor:
            return False
            
        match = db.query(models.MatchedDonorRequest).filter(
            models.MatchedDonorRequest.request_id == request_id,
            models.MatchedDonorRequest.donor_id == target_donor.id
        ).first()
        
        return match is not None

    if user.role == "verified_donor":
        # Target must be the seeker
        if str(request.seeker_auth_user_id) != str(target_user_id):
            return False
            
        # Current user must be a matched donor on this request
        current_donor = db.query(models.DonorProfile).filter(models.DonorProfile.auth_user_id == user.id).first()
        if not current_donor:
            return False
            
        match = db.query(models.MatchedDonorRequest).filter(
            models.MatchedDonorRequest.request_id == request_id,
            models.MatchedDonorRequest.donor_id == current_donor.id
        ).first()
        
        return match is not None

    return False

