import pytest
import pyotp
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session
from app.db import models
from app.core import security
from app.main import app

def test_admin_auth_bypassing_2fa_fails(client: TestClient, db: Session):
    # Setup test admin user with TOTP
    admin_email = "admin@example.com"
    admin_password = "SecurePassword123"
    totp_secret = pyotp.random_base32()

    # Clear existing if any
    db.query(models.User).filter(models.User.email == admin_email).delete()

    admin = models.User(
        email=admin_email,
        hashed_password=security.get_password_hash(admin_password),
        role="admin",
        totp_secret=totp_secret
    )
    db.add(admin)
    db.commit()

    # 1. Wrong Password
    response = client.post("/auth/admin-login", json={
        "email": admin_email,
        "password": "WrongPassword",
        "totp_code": pyotp.TOTP(totp_secret).now()
    })
    assert response.status_code == 401

    # 2. Correct Password, Missing TOTP
    response = client.post("/auth/admin-login", json={
        "email": admin_email,
        "password": admin_password
    })
    assert response.status_code == 422 # Validation error due to missing field

    # 3. Correct Password, Wrong TOTP
    response = client.post("/auth/admin-login", json={
        "email": admin_email,
        "password": admin_password,
        "totp_code": "000000"
    })
    assert response.status_code == 401
    assert "Invalid 2FA code" in response.json()["detail"]

    # 4. TOTP correct, but user isn't admin
    admin.role = "verified_donor"
    db.commit()

    response = client.post("/auth/admin-login", json={
        "email": admin_email,
        "password": admin_password,
        "totp_code": pyotp.TOTP(totp_secret).now()
    })
    assert response.status_code == 403

    admin.role = "admin"
    db.commit()

    # Correct Password, Correct TOTP
    response = client.post("/auth/admin-login", json={
        "email": admin_email,
        "password": admin_password,
        "totp_code": pyotp.TOTP(totp_secret).now()
    })
    assert response.status_code == 200
    data = response.json()
    assert "access_token" in data
    assert data["token_type"] == "bearer"
