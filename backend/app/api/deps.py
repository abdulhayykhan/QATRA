from typing import Generator, Optional, List
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from jose import jwt, JWTError
from sqlalchemy.orm import Session
from app.db.database import SessionLocal
from app.core import security
from app.db import models

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/admin-login")

def get_db() -> Generator:
    try:
        db = SessionLocal()
        yield db
    finally:
        db.close()

class CurrentUser:
    """Dataclass to hold current user information from JWT."""
    def __init__(self, id: str, role: str, phone: Optional[str] = None):
        self.id = id
        self.role = role
        self.phone = phone

def get_current_user(token: str = Depends(oauth2_scheme)) -> CurrentUser:
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate credentials",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = jwt.decode(token, security.SECRET_KEY, algorithms=[security.ALGORITHM])
        user_id: str = payload.get("sub")
        role: str = payload.get("user_role")
        phone: str = payload.get("phone")
        if user_id is None or role is None:
            raise credentials_exception
        return CurrentUser(id=user_id, role=role, phone=phone)
    except JWTError:
        raise credentials_exception

class RoleChecker:
    """Dependency to check if the user has one of the allowed roles."""
    def __init__(self, allowed_roles: List[str]):
        self.allowed_roles = allowed_roles

    def __call__(self, current_user: CurrentUser = Depends(get_current_user)):
        if current_user.role not in self.allowed_roles:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Operation not permitted. Required roles: {self.allowed_roles}"
            )
        return current_user
