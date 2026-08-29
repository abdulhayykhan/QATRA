import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session
from app.db import models
from app.main import app

def test_register_device_token_success(client: TestClient, db: Session, verified_donor_token_headers: dict, verified_donor: models.User):
    # Ensure donor profile exists
    donor = db.query(models.DonorProfile).filter_by(auth_user_id=verified_donor.id).first()
    assert donor is not None

    payload = {
        "token": "fcm_token_123_abc",
        "platform": "android"
    }

    response = client.post(
        "/api/donors/me/device-token",
        json=payload,
        headers=verified_donor_token_headers,
    )
    assert response.status_code == 200
    assert response.json() == {"detail": "Token registered"}

    # Verify token is in DB
    token_record = db.query(models.DonorDeviceToken).filter_by(token="fcm_token_123_abc").first()
    assert token_record is not None
    assert token_record.donor_id == donor.id
    assert token_record.platform == "android"

def test_register_device_token_unauthorized(client: TestClient):
    payload = {
        "token": "fcm_token_123_abc",
        "platform": "android"
    }

    response = client.post(
        "/api/donors/me/device-token",
        json=payload,
    )
    assert response.status_code == 401
