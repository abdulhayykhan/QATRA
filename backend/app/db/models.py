from sqlalchemy import Column, String, Boolean, Integer, Float, ForeignKey, DateTime, SmallInteger
from sqlalchemy.orm import relationship
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.sql import func
from geoalchemy2 import Geography
from app.db.database import Base
import uuid

class User(Base):
    """
    Replaces Supabase auth.users table.
    """
    __tablename__ = "users"
    
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    phone_number = Column(String, unique=True, index=True, nullable=True)
    email = Column(String, unique=True, index=True, nullable=True)
    hashed_password = Column(String, nullable=True)
    totp_secret = Column(String, nullable=True)
    role = Column(String, default="guest") # guest, verified_seeker, verified_donor, drive_organizer, admin
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

class Hospital(Base):
    __tablename__ = "hospitals"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    name = Column(String, nullable=False)
    short_name = Column(String, nullable=False)
    address = Column(String, nullable=False)
    district = Column(String, nullable=False)
    is_trauma_center = Column(Boolean, nullable=False, default=False)
    location = Column(Geography(geometry_type='POINT', srid=4326), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

class AwarenessArticle(Base):
    __tablename__ = "awareness_articles"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    title = Column(String, nullable=False)
    category = Column(String, nullable=False)
    read_time = Column(String, nullable=False)
    summary = Column(String, nullable=False)
    full_content = Column(String, nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

class DonorProfile(Base):
    __tablename__ = "donor_profiles"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    auth_user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), unique=True, nullable=True)
    display_name = Column(String, nullable=False)
    blood_group = Column(String, nullable=False)
    phone_masked = Column(String, nullable=False, default='0300-XXXXXXX')
    cnic_masked = Column(String, nullable=False, default='42101-XXXXXXX-7')
    is_available_to_donate = Column(Boolean, nullable=False, default=True)
    is_eligible = Column(Boolean, nullable=False, default=True)
    cooldown_days_remaining = Column(SmallInteger, nullable=False, default=0)
    lifetime_donations = Column(Integer, nullable=False, default=0)
    tier = Column(String, nullable=False, default='Silver Tier')
    district = Column(String, nullable=False, default='Karachi South')
    is_cnic_verified = Column(Boolean, nullable=False, default=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    user = relationship("User")
    location = relationship("DonorLocation", back_populates="donor", uselist=False, cascade="all, delete-orphan")
    private_contact = relationship("DonorPrivateContact", back_populates="donor", uselist=False, cascade="all, delete-orphan")
    device_tokens = relationship("DonorDeviceToken", back_populates="donor", cascade="all, delete-orphan")

class DonorLocation(Base):
    __tablename__ = "donor_locations"

    donor_id = Column(UUID(as_uuid=True), ForeignKey("donor_profiles.id", ondelete="CASCADE"), primary_key=True)
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    location = Column(Geography(geometry_type='POINT', srid=4326), nullable=False)
    source = Column(String, nullable=False, default='device')
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    donor = relationship("DonorProfile", back_populates="location")

class DonorDeviceToken(Base):
    __tablename__ = "donor_device_tokens"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    donor_id = Column(UUID(as_uuid=True), ForeignKey("donor_profiles.id", ondelete="CASCADE"), nullable=False, index=True)
    token = Column(String, nullable=False, unique=True)
    platform = Column(String, nullable=False, default='android')
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    donor = relationship("DonorProfile", back_populates="device_tokens")

class DonorPrivateContact(Base):
    __tablename__ = "donor_private_contacts"

    donor_id = Column(UUID(as_uuid=True), ForeignKey("donor_profiles.id", ondelete="CASCADE"), primary_key=True)
    phone_e164 = Column(String, nullable=False)
    cnic = Column(String, nullable=False)
    cnic_hash = Column(String, nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    donor = relationship("DonorProfile", back_populates="private_contact")

class DonorCnicDocument(Base):
    __tablename__ = "donor_cnic_documents"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    donor_id = Column(UUID(as_uuid=True), ForeignKey("donor_profiles.id", ondelete="CASCADE"), nullable=False)
    document_kind = Column(String, nullable=False) # FRONT or BACK
    storage_path = Column(String, nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    donor = relationship("DonorProfile")

class BloodRequest(Base):
    __tablename__ = "blood_requests"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    seeker_auth_user_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), index=True, nullable=True)
    hospital_id = Column(UUID(as_uuid=True), ForeignKey("hospitals.id"), nullable=False)
    blood_group = Column(String, nullable=False)
    component = Column(String, nullable=False)
    units_required = Column(Integer, nullable=False)
    urgency = Column(String, nullable=False)
    seeker_name = Column(String, nullable=False)
    seeker_phone_masked = Column(String, nullable=False, default='0300-XXXXXXX')
    seeker_cnic_masked = Column(String, nullable=False, default='42101-XXXXXXX-1')
    status = Column(String, nullable=False)
    active_donors_in_radius = Column(Integer, nullable=False, default=0)
    responded_donors_count = Column(Integer, nullable=False, default=0)
    mrn_number = Column(String, nullable=False)
    ocr_confidence = Column(Integer, nullable=False, default=0)
    is_verified = Column(Boolean, nullable=False, default=True)
    doctor_stamp_verified = Column(Boolean, nullable=False, default=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    hospital = relationship("Hospital")
    user = relationship("User")
    sensitive_data = relationship("RequestSensitiveData", back_populates="request", uselist=False, cascade="all, delete-orphan")
    slip_documents = relationship("HospitalSlipDocument", back_populates="request", cascade="all, delete-orphan")

class RequestSensitiveData(Base):
    __tablename__ = "request_sensitive_data"

    request_id = Column(UUID(as_uuid=True), ForeignKey("blood_requests.id", ondelete="CASCADE"), primary_key=True)
    seeker_phone_e164 = Column(String, nullable=True)
    seeker_phone_hash = Column(String, nullable=False)
    seeker_cnic = Column(String, nullable=False)
    seeker_cnic_hash = Column(String, nullable=False)
    raw_ocr_text = Column(String, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    request = relationship("BloodRequest", back_populates="sensitive_data")

class HospitalSlipDocument(Base):
    __tablename__ = "hospital_slip_documents"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    request_id = Column(UUID(as_uuid=True), ForeignKey("blood_requests.id", ondelete="CASCADE"), nullable=False)
    document_kind = Column(String, nullable=False)
    storage_path = Column(String, nullable=False)
    sha256_digest = Column(String, nullable=False)
    mrn = Column(String, nullable=True)
    doctor_stamp_detected = Column(Boolean, nullable=False, default=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    request = relationship("BloodRequest", back_populates="slip_documents")

class MatchedDonorRequest(Base):
    __tablename__ = "matched_donor_requests"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    request_id = Column(UUID(as_uuid=True), ForeignKey("blood_requests.id", ondelete="CASCADE"), nullable=False)
    donor_id = Column(UUID(as_uuid=True), ForeignKey("donor_profiles.id", ondelete="CASCADE"), nullable=False)
    blood_group = Column(String, nullable=False)
    distance_km = Column(Float, nullable=False, default=0.0)
    eta_minutes = Column(Integer, nullable=False, default=0)
    status_text = Column(String, nullable=False)
    phone_masked = Column(String, nullable=False, default='0300-XXXXXXX')
    is_verified = Column(Boolean, nullable=False, default=True)
    lifetime_donations = Column(Integer, nullable=False, default=0)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    request = relationship("BloodRequest")
    donor = relationship("DonorProfile")

class VerificationQueue(Base):
    __tablename__ = "verification_queue"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    request_id = Column(UUID(as_uuid=True), ForeignKey("blood_requests.id", ondelete="CASCADE"), nullable=False)
    hospital_name = Column(String, nullable=False)
    doctor_stamp_detected = Column(Boolean, nullable=False, default=False)
    mrn = Column(String, nullable=False)
    blood_group = Column(String, nullable=False)
    units = Column(Integer, nullable=False)
    ocr_confidence = Column(Integer, nullable=False)
    blood_group_confidence = Column(Integer, nullable=False)
    flag_warning = Column(String, nullable=True)
    status = Column(String, nullable=False, default='Pending')
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    request = relationship("BloodRequest")

class FraudAuditItem(Base):
    __tablename__ = "fraud_audit_items"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    request_id = Column(UUID(as_uuid=True), ForeignKey("blood_requests.id", ondelete="CASCADE"), nullable=False)
    seeker_cnic_masked = Column(String, nullable=False)
    phone_masked = Column(String, nullable=False)
    hospital_mrn = Column(String, nullable=False)
    ocr_confidence = Column(Integer, nullable=False)
    flag_reason = Column(String, nullable=False)
    action_status = Column(String, nullable=False, default='Flagged')
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    request = relationship("BloodRequest")

class CampusDrive(Base):
    __tablename__ = "campus_drives"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    title = Column(String, nullable=False)
    university_venue = Column(String, nullable=False)
    target_quota_units = Column(Integer, nullable=False)
    registered_donors = Column(Integer, nullable=False, default=0)
    date_str = Column(String, nullable=False)
    time_str = Column(String, nullable=False)
    status = Column(String, nullable=False, default='Scheduled')
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

class DriveAttendee(Base):
    __tablename__ = "drive_attendees"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    drive_id = Column(UUID(as_uuid=True), ForeignKey("campus_drives.id", ondelete="CASCADE"), nullable=False)
    donor_id = Column(UUID(as_uuid=True), ForeignKey("donor_profiles.id", ondelete="CASCADE"), nullable=False)
    name = Column(String, nullable=False)
    dept_year = Column(String, nullable=True)
    cnic_status = Column(String, nullable=False, default='Verified')
    pre_screening_status = Column(String, nullable=False, default='Passed')
    check_in_status = Column(String, nullable=False, default='Checked In 10:42 AM')
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    drive = relationship("CampusDrive")
    donor = relationship("DonorProfile")

class PreScreeningAnswer(Base):
    __tablename__ = "pre_screening_answers"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    donor_id = Column(UUID(as_uuid=True), ForeignKey("donor_profiles.id", ondelete="CASCADE"), nullable=False, unique=True)
    age_valid = Column(Boolean, nullable=False, default=True)
    weight_valid = Column(Boolean, nullable=False, default=True)
    no_recent_illness = Column(Boolean, nullable=False, default=True)
    no_recent_donation = Column(Boolean, nullable=False, default=True)
    no_recent_tattoo_or_surgery = Column(Boolean, nullable=False, default=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    donor = relationship("DonorProfile")

# Missing Tables identified in gap analysis

class FirebasePhoneTokenLedger(Base):
    """
    Fail-closed token ledger to prevent replay attacks on the firebase auth endpoint.
    """
    __tablename__ = "firebase_phone_token_ledger"

    token_hash = Column(String, primary_key=True)
    firebase_uid = Column(String, nullable=False)
    phone = Column(String, nullable=False)
    expires_at = Column(DateTime(timezone=True), nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

class RequestFeedback(Base):
    """
    Missing from original schema, but used by SeekerRepository.submitFeedback
    """
    __tablename__ = "request_feedback"
    
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    request_id = Column(UUID(as_uuid=True), ForeignKey("blood_requests.id", ondelete="CASCADE"), nullable=False)
    rating = Column(Integer, nullable=False)
    note = Column(String, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    request = relationship("BloodRequest")
