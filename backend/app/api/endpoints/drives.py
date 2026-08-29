from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
import uuid

from app.api import deps
from app.db import models
from app.schemas import drives as schemas
from app.api.authorization import (
    apply_campus_drive_policy, verify_campus_drive_write,
    apply_drive_attendee_policy, verify_drive_attendee_write
)

router = APIRouter()

@router.get("/", response_model=List[schemas.CampusDriveResponse])
def list_drives(
    db: Session = Depends(deps.get_db),
    current_user: deps.CurrentUser = Depends(deps.get_current_user)
):
    query = db.query(models.CampusDrive)
    query = apply_campus_drive_policy(query, current_user)
    return query.all()

@router.post("/", response_model=schemas.CampusDriveResponse)
def create_drive(
    drive_in: schemas.CampusDriveCreate,
    db: Session = Depends(deps.get_db),
    current_user: deps.CurrentUser = Depends(deps.get_current_user)
):
    drive = models.CampusDrive(**drive_in.model_dump())
    if not verify_campus_drive_write(drive, current_user):
        raise HTTPException(status_code=403, detail="Only admins can schedule drives")
    
    db.add(drive)
    db.commit()
    db.refresh(drive)
    return drive

@router.post("/{drive_id}/attendees", response_model=schemas.DriveAttendeeResponse)
def register_attendee(
    drive_id: uuid.UUID,
    attendee_in: schemas.DriveAttendeeCreate,
    db: Session = Depends(deps.get_db),
    current_user: deps.CurrentUser = Depends(deps.get_current_user)
):
    dummy_attendee = models.DriveAttendee(donor_id=None)
    # We assign a dummy donor to let the verify function check auth_user_id
    dummy_donor = models.DonorProfile(auth_user_id=current_user.id)
    dummy_attendee.donor = dummy_donor
    if not verify_drive_attendee_write(dummy_attendee, current_user):
        raise HTTPException(status_code=403, detail="Not authorized to register for drives")

    donor = db.query(models.DonorProfile).filter(models.DonorProfile.auth_user_id == current_user.id).first()
    if not donor:
        raise HTTPException(status_code=404, detail="Donor profile not found")

    attendee = models.DriveAttendee(**attendee_in.model_dump(), drive_id=drive_id, donor_id=donor.id)
    
    db.add(attendee)
    db.commit()
    db.refresh(attendee)
    return attendee

@router.post("/check-in/{attendee_id}", response_model=schemas.DriveAttendeeResponse)
def check_in_attendee(
    attendee_id: uuid.UUID,
    db: Session = Depends(deps.get_db),
    current_user: deps.CurrentUser = Depends(deps.get_current_user)
):
    attendee = db.query(models.DriveAttendee).filter(models.DriveAttendee.id == attendee_id).first()
    if not attendee:
        raise HTTPException(status_code=404, detail="Attendee not found")

    if not verify_drive_attendee_write(attendee, current_user):
        raise HTTPException(status_code=403, detail="Only admins can check-in attendees")

    attendee.check_in_status = "Checked In"
    db.commit()
    db.refresh(attendee)
    return attendee
