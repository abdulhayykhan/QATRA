from pydantic import BaseModel
from typing import List
from uuid import UUID

class MatchedDonorResponse(BaseModel):
    donor_id: UUID
    display_name: str
    blood_group: str
    distance_km: float
    eta_minutes: int
    status_text: str
    phone_masked: str
    is_verified: bool
    lifetime_donations: int

class RequestCreate(BaseModel):
    hospital_id: UUID
    blood_group: str
    component: str
    units_required: int
    urgency: str
    seeker_name: str
    seeker_phone: str
    seeker_cnic: str
    mrn_number: str

class RequestCreateResponse(BaseModel):
    request_id: UUID
    status: str
