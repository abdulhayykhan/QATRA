from pydantic import BaseModel
from typing import Optional

class LocationUpdate(BaseModel):
    latitude: float
    longitude: float

class LocationUpdateResponse(BaseModel):
    status: str
    message: str

class DeviceTokenCreate(BaseModel):
    token: str
    platform: str = "android"
