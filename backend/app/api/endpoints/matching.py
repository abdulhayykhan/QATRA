from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from sqlalchemy import or_, and_, func, desc
from typing import List
from uuid import UUID
import math

from app.api.deps import get_db
from app.db import models
from app.schemas.matching import MatchedDonorResponse
from app.api.deps import get_current_user, CurrentUser

router = APIRouter()

def get_compatible_blood_groups(request_bg: str) -> List[str]:
    # Equivalent to the Postgres OR logic
    mapping = {
        'A+': ['A+', 'A-', 'O+', 'O-'],
        'A-': ['A-', 'O-'],
        'B+': ['B+', 'B-', 'O+', 'O-'],
        'B-': ['B-', 'O-'],
        'AB+': ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'],
        'AB-': ['A-', 'B-', 'AB-', 'O-'],
        'O+': ['O+', 'O-'],
        'O-': ['O-']
    }
    return mapping.get(request_bg, [request_bg])

@router.get("/requests/{request_id}/eligible-donors", response_model=List[MatchedDonorResponse])
def find_eligible_donors_for_request(
    request_id: UUID,
    radius_km: float = Query(10.0, description="Search radius in kilometers"),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user)
):
    # Retrieve the request and hospital
    request = db.query(models.BloodRequest).filter(models.BloodRequest.id == request_id).first()
    if not request:
        raise HTTPException(status_code=404, detail="Request not found")

    from app.api import authorization
    if not authorization.verify_matched_donors_read(request, current_user):
        raise HTTPException(status_code=403, detail="Not authorized to view eligible donors for this request")

    if request.status != 'BROADCASTING':
        return []

    hospital = db.query(models.Hospital).filter(models.Hospital.id == request.hospital_id).first()
    if not hospital or not hospital.location:
        return []

    # Using GeoAlchemy2 to build the query exactly mirroring the PostGIS RPC
    compatible_groups = get_compatible_blood_groups(request.blood_group)

    # 1. Start query against DonorProfile
    query = db.query(
        models.DonorProfile.id.label("donor_id"),
        models.DonorProfile.display_name,
        models.DonorProfile.blood_group,
        (func.ST_Distance(models.DonorLocation.location, hospital.location) / 1000.0).label("distance_km"),
        models.DonorProfile.phone_masked,
        models.DonorProfile.is_cnic_verified,
        models.DonorProfile.is_eligible,
        models.DonorProfile.lifetime_donations
    ).join(
        models.DonorLocation, models.DonorProfile.id == models.DonorLocation.donor_id
    ).join(
        models.PreScreeningAnswer, models.DonorProfile.id == models.PreScreeningAnswer.donor_id
    )

    # 2. Apply all eligibility filters
    query = query.filter(
        # Geo distance filter
        func.ST_DWithin(hospital.location, models.DonorLocation.location, radius_km * 1000.0),
        
        # Profile flags
        models.DonorProfile.is_available_to_donate == True,
        models.DonorProfile.is_eligible == True,
        models.DonorProfile.cooldown_days_remaining == 0,
        models.DonorProfile.is_cnic_verified == True,
        
        # Pre-screening flags
        models.PreScreeningAnswer.age_valid == True,
        models.PreScreeningAnswer.weight_valid == True,
        models.PreScreeningAnswer.no_recent_illness == True,
        models.PreScreeningAnswer.no_recent_donation == True,
        models.PreScreeningAnswer.no_recent_tattoo_or_surgery == True,
        
        # Blood compatibility
        models.DonorProfile.blood_group.in_(compatible_groups)
    )

    # 3. Order by distance
    query = query.order_by(func.ST_Distance(models.DonorLocation.location, hospital.location).asc())

    results = query.all()

    # 4. Map to response model
    response = []
    for row in results:
        eta = max(1, math.ceil(row.distance_km * 4)) # rough 4 mins per km
        response.append(
            MatchedDonorResponse(
                donor_id=row.donor_id,
                display_name=row.display_name,
                blood_group=row.blood_group,
                distance_km=round(row.distance_km, 2),
                eta_minutes=eta,
                status_text="Notified",
                phone_masked=row.phone_masked,
                is_verified=(row.is_cnic_verified and row.is_eligible),
                lifetime_donations=row.lifetime_donations
            )
        )

    return response
