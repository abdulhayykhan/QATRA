import pytest
import uuid
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session
from app.db import models
from app.main import app

def test_contact_auth_unauthorized(client: TestClient, db: Session, verified_seeker_token_headers: dict, verified_seeker: models.User, verified_donor: models.User):
    # Create request
    hospital = db.query(models.Hospital).first()
    if not hospital:
        hospital = models.Hospital(name="Test Hosp", short_name="Test", address="Test", district="Test")
        db.add(hospital)
        db.commit()

    request_id = uuid.uuid4()
    req = models.BloodRequest(
        id=request_id,
        seeker_auth_user_id=verified_seeker.id,
        hospital_id=hospital.id,
        blood_group="O+",
        component="Whole Blood",
        units_required=1,
        urgency="High",
        seeker_name="Seeker",
        status="BROADCASTING",
        mrn_number="123"
    )
    db.add(req)
    db.commit()

    # Missing MatchedDonorRequest! Seeker shouldn't be able to view donor phone
    response = client.get(
        f"/api/contact/{request_id}/contact/{verified_donor.id}",
        headers=verified_seeker_token_headers
    )
    assert response.status_code == 403

def test_contact_auth_authorized(client: TestClient, db: Session, verified_seeker_token_headers: dict, verified_seeker: models.User, verified_donor: models.User):
    hospital = db.query(models.Hospital).first()
    request_id = uuid.uuid4()
    req = models.BloodRequest(
        id=request_id,
        seeker_auth_user_id=verified_seeker.id,
        hospital_id=hospital.id,
        blood_group="O+",
        component="Whole Blood",
        units_required=1,
        urgency="High",
        seeker_name="Seeker",
        status="BROADCASTING",
        mrn_number="123"
    )
    db.add(req)
    db.commit()

    donor_profile = db.query(models.DonorProfile).filter_by(auth_user_id=verified_donor.id).first()
    
    # Add match
    match = models.MatchedDonorRequest(
        request_id=request_id,
        donor_id=donor_profile.id,
        blood_group="O+",
        status_text="ACCEPTED"
    )
    db.add(match)
    db.commit()

    # Now authorized
    response = client.get(
        f"/api/contact/{request_id}/contact/{verified_donor.id}",
        headers=verified_seeker_token_headers
    )
    assert response.status_code == 200
    assert response.json()["phone_number"] == verified_donor.phone_number
