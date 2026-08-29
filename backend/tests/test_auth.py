import pytest
from unittest.mock import MagicMock
from sqlalchemy.exc import IntegrityError
from app.db import models

def test_verify_firebase_phone_missing_token(client):
    """
    Regression Test: Ensure that empty or missing tokens return 400 or 422.
    """
    response = client.post("/auth/verify-firebase-phone", json={})
    assert response.status_code == 422 # Pydantic validation error for missing field

    response = client.post("/auth/verify-firebase-phone", json={"firebase_id_token": ""})
    assert response.status_code == 400
    assert response.json()["detail"] == "firebase_id_token is required"

def test_verify_firebase_phone_invalid_token(client, mocker):
    """
    Regression Test: Ensure invalid Firebase tokens are rejected.
    """
    mock_verify = mocker.patch("app.api.endpoints.auth.firebase_auth.verify_id_token")
    mock_verify.side_effect = Exception("Invalid token")

    response = client.post("/auth/verify-firebase-phone", json={"firebase_id_token": "invalid_token"})
    assert response.status_code == 401
    assert "Invalid, expired, or unusable Firebase ID token" in response.json()["detail"]

def test_verify_firebase_phone_no_phone(client, mocker):
    """
    Test token without phone_number claim.
    """
    mock_verify = mocker.patch("app.api.endpoints.auth.firebase_auth.verify_id_token")
    mock_verify.return_value = {"uid": "123"}

    response = client.post("/auth/verify-firebase-phone", json={"firebase_id_token": "valid_token_no_phone"})
    assert response.status_code == 401
    assert "no phone_number claim" in response.json()["detail"]

def test_verify_firebase_phone_duplicate_token(client, mocker):
    """
    Regression Test: Replay attack prevention. Fail-closed ledger.
    """
    mock_verify = mocker.patch("app.api.endpoints.auth.firebase_auth.verify_id_token")
    mock_verify.return_value = {"uid": "123", "phone_number": "+923001234567", "exp": 9999999999}

    # Mock DB session to simulate IntegrityError on ledger insertion
    mock_db = MagicMock()
    mock_db.commit.side_effect = IntegrityError("duplicate key", params={}, orig=Exception())
    
    # We must patch the dependency to return our mock_db
    from app.api.deps import get_db
    app = client.app
    app.dependency_overrides[get_db] = lambda: mock_db

    response = client.post("/auth/verify-firebase-phone", json={"firebase_id_token": "duplicate_token"})
    
    assert response.status_code == 409
    assert "already been used" in response.json()["detail"]
    mock_db.rollback.assert_called_once()
    
    app.dependency_overrides.clear()

def test_verify_firebase_phone_success_new_user(client, mocker):
    """
    Test successful token exchange for a new user.
    """
    mock_verify = mocker.patch("app.api.endpoints.auth.firebase_auth.verify_id_token")
    mock_verify.return_value = {"uid": "123", "phone_number": "+923001234567", "exp": 9999999999}

    mock_db = MagicMock()
    # First query for user returns None (new user)
    mock_db.query().filter().first.return_value = None
    
    from app.api.deps import get_db
    app = client.app
    app.dependency_overrides[get_db] = lambda: mock_db

    response = client.post("/auth/verify-firebase-phone", json={"firebase_id_token": "valid_token"})
    
    assert response.status_code == 200
    data = response.json()
    assert "access_token" in data
    assert data["token_type"] == "bearer"
    assert data["user"]["phone"] == "+923001234567"
    assert data["user"]["role"] == "guest"
    
    # Verify ledger insertion
    mock_db.add.assert_any_call(mocker.ANY) # Ledger entry
    # Verify user creation
    mock_db.add.assert_any_call(mocker.ANY) # User entry
    
    app.dependency_overrides.clear()
