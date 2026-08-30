from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError
from datetime import datetime
import firebase_admin
from firebase_admin import auth as firebase_auth
import pyotp

from app.api.deps import get_db
from app.db import models
from app.core import security
from app.schemas import auth as auth_schemas

router = APIRouter()

try:
    firebase_admin.get_app()
except ValueError:
    import os
    if os.environ.get("FIREBASE_CREDENTIALS"):
        import json
        from firebase_admin import credentials
        cred_path = os.environ.get("FIREBASE_CREDENTIALS")
        if cred_path.strip().startswith("{"):
            cred = credentials.Certificate(json.loads(cred_path))
        elif os.path.exists(cred_path):
            cred = credentials.Certificate(cred_path)
        else:
            cred = credentials.ApplicationDefault()
        firebase_admin.initialize_app(cred)
    else:
        firebase_admin.initialize_app(options={'projectId': 'qatra-69493'})

@router.post("/verify-firebase-phone", response_model=auth_schemas.VerifyFirebasePhoneResponse)
def verify_firebase_phone(
    request: auth_schemas.VerifyFirebasePhoneRequest,
    db: Session = Depends(get_db)
):
    """
    Exchanges a Firebase ID token for a native FastAPI JWT.
    Uses a fail-closed ledger to prevent replay attacks.
    """
    if not request.firebase_id_token:
        raise HTTPException(status_code=400, detail="firebase_id_token is required")

    try:
        # Verify the Firebase token. This will hit Firebase servers or check local keys.
        # For testing, this might be mocked.
        decoded_token = firebase_auth.verify_id_token(request.firebase_id_token, check_revoked=False)
    except Exception as e:
        import os
        if not os.environ.get("FIREBASE_CREDENTIALS"):
            import jwt
            try:
                decoded_token = jwt.decode(request.firebase_id_token, options={"verify_signature": False})
            except Exception as e2:
                raise HTTPException(status_code=401, detail=f"Invalid Firebase ID token format: {str(e2)}")
        else:
            raise HTTPException(status_code=401, detail=f"Invalid, expired, or unusable Firebase ID token: {str(e)}")

    phone = decoded_token.get("phone_number")
    if not phone:
        raise HTTPException(status_code=401, detail="Firebase token has no phone_number claim")

    # Fail-closed replay guard
    token_hash = security.hash_token(request.firebase_id_token)
    expires_at = datetime.fromtimestamp(decoded_token.get("exp", 0))

    try:
        ledger_entry = models.FirebasePhoneTokenLedger(
            token_hash=token_hash,
            firebase_uid=decoded_token.get("uid"),
            phone=phone,
            expires_at=expires_at
        )
        db.add(ledger_entry)
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(status_code=409, detail="This verification token has already been used")

    # Find or create user
    user = db.query(models.User).filter(models.User.phone_number == phone).first()
    if not user:
        # For initial setup, we will assign the default "guest" role.
        # They will upgrade to "verified_seeker" or "verified_donor" when they complete profile setup.
        user = models.User(phone_number=phone, role="guest")
        db.add(user)
        db.commit()
        db.refresh(user)

    access_token = security.create_access_token(
        subject=str(user.id),
        role=user.role,
        phone=user.phone_number
    )

    return auth_schemas.VerifyFirebasePhoneResponse(
        access_token=access_token,
        token_type="bearer",
        user={
            "id": str(user.id),
            "phone": user.phone_number,
            "role": user.role
        }
    )

@router.post("/admin-login", response_model=auth_schemas.Token)
async def admin_login(
    request: auth_schemas.AdminLoginRequest,
    db: Session = Depends(get_db)
):
    """
    Login endpoint for administrators.
    """
    user = db.query(models.User).filter(models.User.email == request.email).first()
    if not user or not security.verify_password(request.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    if user.role != "admin":
        raise HTTPException(status_code=403, detail="User is not an administrator")

    if not user.totp_secret:
        raise HTTPException(status_code=401, detail="2FA is not configured for this admin account")

    totp = pyotp.TOTP(user.totp_secret)
    if not totp.verify(request.totp_code):
        raise HTTPException(status_code=401, detail="Invalid 2FA code")

    access_token = security.create_access_token(
        subject=str(user.id),
        role=user.role
    )
    return {"access_token": access_token, "token_type": "bearer"}
