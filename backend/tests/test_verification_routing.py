import pytest
import uuid
from unittest.mock import patch
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session
from app.main import app
from app.db import models
from app.api.deps import get_db, get_current_user
from app.db.database import Base
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

# Mock current user for our tests
class MockUser:
    def __init__(self, role="verified_seeker", user_id=None):
        self.id = str(user_id or uuid.uuid4())
        self.role = role
        self.phone_number = "+923001234567"

@pytest.fixture
def test_hospital(db_session):
    # Create test hospital
    hospital = models.Hospital(
        id=uuid.uuid4(),
        name="Test Hospital",
        short_name="TH",
        address="123 Test St",
        district="Karachi South"
    )
    db_session.add(hospital)
    db_session.commit()
    
    yield {"hospital_id": str(hospital.id)}
    
    db_session.delete(hospital)
    db_session.commit()


mock_user = MockUser(role="verified_seeker")
@patch("app.api.endpoints.requests.storage.upload_file")
@patch("app.api.endpoints.requests.trigger_geo_alert")
def test_high_confidence_ocr_routing(mock_trigger, mock_upload, test_hospital, client, db_session):
    client.app.dependency_overrides[get_current_user] = lambda: mock_user
    # Setup mock upload success
    mock_upload.return_value = True

    # 1. Create a request
    create_payload = {
        "hospital_id": test_hospital["hospital_id"],
        "blood_group": "A+",
        "component": "WHOLE_BLOOD",
        "units_required": 1,
        "urgency": "STANDARD",
        "seeker_name": "Test Seeker",
        "seeker_phone": "+923001234567",
        "seeker_cnic": "4210112345671",
        "mrn_number": "MRN-12345"
    }
    
    response = client.post("/api/requests", json=create_payload)
    assert response.status_code == 201
    request_id = response.json()["request_id"]
    
    # 2. Upload hospital slip with HIGH confidence and matching MRN
    files = {'file': ('test.jpg', b'test_image_bytes', 'image/jpeg')}
    data = {
        'ocr_text': 'This is a test slip with MRN-12345 included.',
        'ocr_confidence': '85'
    }
    
    response = client.post(f"/api/requests/{request_id}/hospital-slip", files=files, data=data)
    if response.status_code != 200:
        print("DEBUG 403 (high):", response.json())
    assert response.status_code == 200
    
    # 3. Verify status routed to BROADCASTING
    res_data = response.json()
    assert res_data["status"] == "BROADCASTING"
    assert res_data["is_verified"] is True
    
    # Check that verification_queue has no items for this request
    queue_item = db_session.query(models.VerificationQueue).filter_by(request_id=uuid.UUID(request_id)).first()
    assert queue_item is None

@patch("app.api.endpoints.requests.storage.upload_file")
def test_low_confidence_ocr_routing(mock_upload, test_hospital, client, db_session):
    mock_user = MockUser(role="verified_seeker")
    client.app.dependency_overrides[get_current_user] = lambda: mock_user
    mock_upload.return_value = True

    create_payload = {
        "hospital_id": test_hospital["hospital_id"],
        "blood_group": "B+",
        "component": "PRBC",
        "units_required": 2,
        "urgency": "HIGH_PRIORITY",
        "seeker_name": "Test Seeker 2",
        "seeker_phone": "+923001234567",
        "seeker_cnic": "4210112345671",
        "mrn_number": "MRN-999"
    }
    
    response = client.post("/api/requests", json=create_payload)
    request_id = response.json()["request_id"]
    
    # Upload with LOW confidence and missing MRN
    files = {'file': ('test2.jpg', b'test_image_bytes', 'image/jpeg')}
    data = {
        'ocr_text': 'Blurry text cannot read',
        'ocr_confidence': '40'
    }
    
    response = client.post(f"/api/requests/{request_id}/hospital-slip", files=files, data=data)
    if response.status_code != 200:
        print("DEBUG 403 (low):", response.json())
    assert response.status_code == 200
    
    # Verify status routed to VERIFYING (manual review)
    res_data = response.json()
    assert res_data["status"] == "VERIFYING"
    assert res_data["is_verified"] is False
    
    # Check that verification_queue item was created
    queue_item = db_session.query(models.VerificationQueue).filter_by(request_id=uuid.UUID(request_id)).first()
    assert queue_item is not None
    assert queue_item.ocr_confidence == 40
    assert queue_item.status == "Pending"
    assert queue_item.flag_warning == "Low OCR confidence or MRN mismatch"
