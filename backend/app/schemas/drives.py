from pydantic import BaseModel
import uuid
from typing import Optional

class CampusDriveBase(BaseModel):
    title: str
    university_venue: str
    target_quota_units: int
    date_str: str
    time_str: str
    status: str = "Scheduled"

class CampusDriveCreate(CampusDriveBase):
    pass

class CampusDriveResponse(CampusDriveBase):
    id: uuid.UUID
    registered_donors: int

    class Config:
        from_attributes = True

class DriveAttendeeBase(BaseModel):
    name: str
    dept_year: Optional[str] = None

class DriveAttendeeCreate(DriveAttendeeBase):
    pass

class DriveAttendeeResponse(DriveAttendeeBase):
    id: uuid.UUID
    drive_id: uuid.UUID
    donor_id: uuid.UUID
    cnic_status: str
    pre_screening_status: str
    check_in_status: str

    class Config:
        from_attributes = True
