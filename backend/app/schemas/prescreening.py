from pydantic import BaseModel
import uuid
from typing import Optional

class PreScreeningAnswerBase(BaseModel):
    age_valid: bool
    weight_valid: bool
    no_recent_illness: bool
    no_recent_donation: bool
    no_recent_tattoo_or_surgery: bool

class PreScreeningAnswerCreate(PreScreeningAnswerBase):
    pass

class PreScreeningAnswerResponse(PreScreeningAnswerBase):
    id: uuid.UUID
    donor_id: uuid.UUID

    class Config:
        from_attributes = True
