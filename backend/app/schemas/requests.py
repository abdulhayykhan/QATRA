import uuid
from typing import Optional
from datetime import datetime
from pydantic import BaseModel, Field

class BloodRequestCreate(BaseModel):
    hospital_id: uuid.UUID
    blood_group: str = Field(..., pattern=r'^(A|B|AB|O)[+-]$')
    component: str
    units_required: int = Field(..., gt=0)
    urgency: str
    seeker_name: str
    seeker_phone: str
    seeker_cnic: str
    mrn_number: str

class BloodRequestResponse(BaseModel):
    request_id: uuid.UUID
    status: str
    is_verified: bool
    ocr_confidence: int

    class Config:
        from_attributes = True

class BloodRequestFeedItem(BaseModel):
    id: uuid.UUID
    hospital_id: uuid.UUID
    blood_group: str
    component: str
    units_required: int
    urgency: str
    status: str
    active_donors_in_radius: int
    responded_donors_count: int
    is_verified: bool
    created_at: datetime | None = None

    class Config:
        from_attributes = True
