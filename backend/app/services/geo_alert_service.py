import uuid
from sqlalchemy.orm import Session
from app.db import models
from app.db.database import SessionLocal
from app.api.endpoints.matching import find_eligible_donors_for_request
from app.services.fcm_service import send_push_notification

def trigger_geo_alert(request_id: uuid.UUID):
    """
    Background task to find eligible donors within the radius and send an FCM push notification.
    """
    db = SessionLocal()
    try:
        request = db.query(models.BloodRequest).filter(models.BloodRequest.id == request_id).first()
        if not request or request.status != "BROADCASTING":
            return

        # Call the matching logic (from Prompt 4)
        # This evaluates eligibility, radius, blood group compatibility, etc.
        eligible_donors = find_eligible_donors_for_request(request_id, radius_km=10.0, db=db, current_user=None)
        
        if not eligible_donors:
            print(f"No eligible donors found for request {request_id}")
            return

        # Extract donor IDs
        donor_ids = [donor.donor_id for donor in eligible_donors]
        
        # Update matched donors count
        request.active_donors_in_radius = len(donor_ids)
        db.commit()

        # Fetch their active device tokens
        tokens = db.query(models.DonorDeviceToken.token).filter(
            models.DonorDeviceToken.donor_id.in_(donor_ids)
        ).all()
        
        token_list = [t[0] for t in tokens]
        
        if not token_list:
            print(f"No registered FCM tokens for matched donors on request {request_id}")
            return

        # Construct the push payload
        hospital = db.query(models.Hospital).filter(models.Hospital.id == request.hospital_id).first()
        hospital_name = hospital.name if hospital else "Unknown Hospital"
        
        title = f"Emergency {request.blood_group} Blood Required"
        body = f"{request.units_required} unit(s) needed at {hospital_name}."
        
        data = {
            "request_id": str(request.id),
            "blood_group": request.blood_group,
            "type": "geo_alert"
        }

        # Dispatch via FCM
        send_push_notification(token_list, title, body, data)
        print(f"Dispatched geo-alert to {len(token_list)} devices for request {request_id}")
    finally:
        db.close()
