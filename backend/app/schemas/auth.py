from pydantic import BaseModel
from typing import Optional

class VerifyFirebasePhoneRequest(BaseModel):
    firebase_id_token: str

class VerifyFirebasePhoneResponse(BaseModel):
    access_token: str
    token_type: str
    user: dict

class AdminLoginRequest(BaseModel):
    email: str
    password: str
    totp_code: str

class Token(BaseModel):
    access_token: str
    token_type: str
