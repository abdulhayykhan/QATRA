from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.api.deps import get_db
from app.db import models
from app.schemas.donor import LocationUpdate, LocationUpdateResponse, DeviceTokenCreate
from fastapi import UploadFile, File, Form
from app.api.authorization import (
    verify_donor_location_write,
    verify_donor_cnic_document_write,
    verify_donor_device_token_write
)
import hashlib
from app.core import storage
from app.api.deps import get_current_user, CurrentUser
from geoalchemy2.elements import WKTElement

router = APIRouter()

@router.put("/me/location", response_model=LocationUpdateResponse)
def update_location(
    location: LocationUpdate,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user)
):
    dummy_location = models.DonorLocation()
    dummy_donor = models.DonorProfile(auth_user_id=current_user.id)
    dummy_location.donor = dummy_donor
    if not verify_donor_location_write(dummy_location, current_user):
        raise HTTPException(status_code=403, detail="Not authorized to update location")

    # Find the donor profile
    donor = db.query(models.DonorProfile).filter(
        models.DonorProfile.auth_user_id == current_user.id
    ).first()

    if not donor:
        raise HTTPException(status_code=404, detail="Donor profile not found")

    # The mobile client handles pausing the stream if 'is_available_to_donate' is off, 
    # but we can enforce server-side rejection too just in case.
    if not donor.is_available_to_donate:
        raise HTTPException(status_code=400, detail="Cannot update location while unavailable")

    # Convert lat/lon to PostGIS Geography WKT Point
    # WKT format is POINT(lon lat)
    point = WKTElement(f"POINT({location.longitude} {location.latitude})", srid=4326)

    # Upsert location
    donor_location = db.query(models.DonorLocation).filter(
        models.DonorLocation.donor_id == donor.id
    ).first()

    if donor_location:
        donor_location.latitude = location.latitude
        donor_location.longitude = location.longitude
        donor_location.location = point
    else:
        donor_location = models.DonorLocation(
            donor_id=donor.id,
            latitude=location.latitude,
            longitude=location.longitude,
            location=point,
            source="device"
        )
        db.add(donor_location)

    db.commit()

    return LocationUpdateResponse(status="success", message="Location updated successfully")

@router.post("/me/cnic-document")
async def upload_cnic_document(
    file: UploadFile = File(...),
    document_kind: str = Form(...), # FRONT or BACK
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user)
):
    dummy_doc = models.DonorCnicDocument()
    dummy_donor = models.DonorProfile(auth_user_id=current_user.id)
    dummy_doc.donor = dummy_donor
    if not verify_donor_cnic_document_write(dummy_doc, current_user):
        raise HTTPException(status_code=403, detail="Not authorized")
        
    donor = db.query(models.DonorProfile).filter_by(auth_user_id=current_user.id).first()
    if not donor:
        raise HTTPException(status_code=404, detail="Donor profile not found")

    if document_kind not in ["FRONT", "BACK"]:
        raise HTTPException(status_code=400, detail="Invalid document_kind")

    file_content = await file.read()
    file_hash = hashlib.sha256(file_content).hexdigest()
    
    object_name = f"{donor.id}/{document_kind}_{file_hash}_{file.filename}"
    upload_success = storage.upload_file(
        file_content=file_content,
        bucket_name=storage.S3_CNIC_DOCUMENTS_BUCKET,
        object_name=object_name,
        content_type=file.content_type or "image/jpeg"
    )

    if not upload_success:
        raise HTTPException(status_code=500, detail="Failed to upload CNIC document to storage backend")

    cnic_doc = models.DonorCnicDocument(
        donor_id=donor.id,
        document_kind=document_kind,
        storage_path=object_name
    )
    db.add(cnic_doc)
    db.commit()

    return {"detail": f"{document_kind} uploaded successfully", "storage_path": object_name}

@router.post("/me/device-token")
def register_device_token(
    payload: DeviceTokenCreate,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user)
):
    dummy_token = models.DonorDeviceToken()
    dummy_donor = models.DonorProfile(auth_user_id=current_user.id)
    dummy_token.donor = dummy_donor
    if not verify_donor_device_token_write(dummy_token, current_user):
        raise HTTPException(status_code=403, detail="Not authorized")
        
    import uuid
    donor = db.query(models.DonorProfile).filter_by(auth_user_id=uuid.UUID(current_user.id)).first()
    if not donor:
        raise HTTPException(status_code=404, detail="Donor profile not found")

    # Check if token already exists
    existing_token = db.query(models.DonorDeviceToken).filter_by(token=payload.token).first()
    if existing_token:
        # Re-assign to current donor if it was mapped to someone else
        if existing_token.donor_id != donor.id:
            existing_token.donor_id = donor.id
            db.commit()
        return {"detail": "Token registered"}

    # Add new token
    new_token = models.DonorDeviceToken(
        donor_id=donor.id,
        token=payload.token,
        platform=payload.platform
    )
    db.add(new_token)
    db.commit()

    return {"detail": "Token registered"}
