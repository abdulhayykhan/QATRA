import uuid
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.api.deps import get_db
from app.db import models
from app.api.deps import get_current_user, CurrentUser
from app.api.authorization import verify_contact_visibility
from pydantic import BaseModel

router = APIRouter()

class ContactResponse(BaseModel):
    phone_number: str

@router.get("/{request_id}/contact/{target_user_id}", response_model=ContactResponse)
async def get_contact_number(
    request_id: uuid.UUID,
    target_user_id: uuid.UUID,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user)
):
    """
    Retrieves the exact, unmasked phone number of the target user if and only if
    there is an active, accepted match between the requester and target.
    This enforces strict point-in-time RLS-equivalent privacy.
    """
    # 1. Authorize access
    if not verify_contact_visibility(request_id, target_user_id, current_user, db):
        raise HTTPException(status_code=403, detail="Not authorized to view this contact number")

    # 2. Fetch the target user's contact information
    target_user = db.query(models.User).filter(models.User.id == target_user_id).first()
    if not target_user or not target_user.phone_number:
        # Fallback to private contact table if user phone_number is null
        # Note: If target is donor, check private contact table for the E164 number.
        target_donor = db.query(models.DonorProfile).filter(models.DonorProfile.auth_user_id == target_user_id).first()
        if target_donor:
            private_contact = db.query(models.DonorPrivateContact).filter(models.DonorPrivateContact.donor_id == target_donor.id).first()
            if private_contact and private_contact.phone_e164:
                return {"phone_number": private_contact.phone_e164}
        
        # If target is seeker or fallback failed, and user phone_number exists
        if target_user and target_user.phone_number:
            return {"phone_number": target_user.phone_number}
            
        # Check request sensitive data for seeker
        request_sensitive = db.query(models.RequestSensitiveData).filter(models.RequestSensitiveData.request_id == request_id).first()
        if request_sensitive and request_sensitive.seeker_phone_e164:
            return {"phone_number": request_sensitive.seeker_phone_e164}

        raise HTTPException(status_code=404, detail="Phone number not found for this user")

    return {"phone_number": target_user.phone_number}
