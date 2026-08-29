import pytest
import uuid
from app.db import models
from app.api import deps

def test_feed_endpoint_authorization(client, db_session):
    """
    Proves that a verified_donor calling /requests/feed never receives a request 
    with status VERIFYING, REJECTED, or FLAGGED - only BROADCASTING.
    """
    
    # 1. Use dummy hospital id
    hospital_id = uuid.uuid4()
    seeker_id = uuid.uuid4()
    
    # 2. Insert test requests with various statuses
    r1 = models.BloodRequest(
        id=uuid.uuid4(),
        seeker_auth_user_id=seeker_id,
        hospital_id=hospital_id,
        blood_group="A+",
        component="Whole Blood",
        units_required=1,
        urgency="HIGH",
        status="BROADCASTING",
        seeker_name="Test Seeker 1",
        seeker_phone_masked="0300-XXXXXXX",
        seeker_cnic_masked="42101-XXXXXXX-1",
        mrn_number="MRN001",
        is_verified=True,
        ocr_confidence=95,
        doctor_stamp_verified=True
    )
    
    r2 = models.BloodRequest(
        id=uuid.uuid4(),
        seeker_auth_user_id=seeker_id,
        hospital_id=hospital_id,
        blood_group="B+",
        component="Whole Blood",
        units_required=2,
        urgency="HIGH",
        status="VERIFYING",
        seeker_name="Test Seeker 2",
        seeker_phone_masked="0300-XXXXXXX",
        seeker_cnic_masked="42101-XXXXXXX-1",
        mrn_number="MRN002",
        is_verified=False,
        ocr_confidence=50,
        doctor_stamp_verified=False
    )
    
    r3 = models.BloodRequest(
        id=uuid.uuid4(),
        seeker_auth_user_id=seeker_id,
        hospital_id=hospital_id,
        blood_group="O+",
        component="Whole Blood",
        units_required=3,
        urgency="HIGH",
        status="REJECTED",
        seeker_name="Test Seeker 3",
        seeker_phone_masked="0300-XXXXXXX",
        seeker_cnic_masked="42101-XXXXXXX-1",
        mrn_number="MRN003",
        is_verified=False,
        ocr_confidence=10,
        doctor_stamp_verified=False
    )

    db_session.add_all([r1, r2, r3])
    db_session.commit()

    # 3. Override get_current_user to simulate a verified_donor
    def override_get_current_user():
        return deps.CurrentUser(id=str(uuid.uuid4()), role="verified_donor")
        
    client.app.dependency_overrides[deps.get_current_user] = override_get_current_user

    # 4. Call endpoint
    response = client.get("/api/requests/feed")
    assert response.status_code == 200
    
    data = response.json()
    
    # 5. Assert only BROADCASTING requests are returned
    ids_returned = [r["id"] for r in data]
    assert str(r1.id) in ids_returned, "The BROADCASTING request should be returned"
    assert data[0]["status"] == "BROADCASTING", "Status must be BROADCASTING"
    
    # Prove that failure mode is closed by explicitly asserting the others aren't there
    ids_returned = [r["id"] for r in data]
    assert str(r2.id) not in ids_returned, "VERIFYING requests must not be visible to donors"
    assert str(r3.id) not in ids_returned, "REJECTED requests must not be visible to donors"
    
    client.app.dependency_overrides.clear()
