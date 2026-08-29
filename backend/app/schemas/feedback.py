from pydantic import BaseModel
import uuid
from typing import Optional

class RequestFeedbackBase(BaseModel):
    rating: int
    note: Optional[str] = None

class RequestFeedbackCreate(RequestFeedbackBase):
    pass

class RequestFeedbackResponse(RequestFeedbackBase):
    id: uuid.UUID
    request_id: uuid.UUID

    class Config:
        from_attributes = True
