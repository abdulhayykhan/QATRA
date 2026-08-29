from pydantic import BaseModel
from typing import Optional
import uuid

class AwarenessArticleBase(BaseModel):
    title: str
    category: str
    read_time: str
    summary: str
    full_content: str

class AwarenessArticleCreate(AwarenessArticleBase):
    pass

class AwarenessArticleResponse(AwarenessArticleBase):
    id: uuid.UUID
    
    class Config:
        from_attributes = True
