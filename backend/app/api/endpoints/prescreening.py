from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List

from app.api import deps
from app.db import models
from app.schemas import prescreening as schemas
from app.api.authorization import apply_pre_screening_answer_policy, verify_pre_screening_answer_write

router = APIRouter()

@router.get("/me", response_model=List[schemas.PreScreeningAnswerResponse])
def read_prescreening(
    db: Session = Depends(deps.get_db),
    current_user: deps.CurrentUser = Depends(deps.get_current_user)
):
    query = db.query(models.PreScreeningAnswer)
    query = apply_pre_screening_answer_policy(query, current_user)
    return query.all()

@router.post("/me", response_model=schemas.PreScreeningAnswerResponse)
def upsert_prescreening(
    answer_in: schemas.PreScreeningAnswerCreate,
    db: Session = Depends(deps.get_db),
    current_user: deps.CurrentUser = Depends(deps.get_current_user)
):


    donor = db.query(models.DonorProfile).filter(models.DonorProfile.auth_user_id == current_user.id).first()
    if not donor:
        raise HTTPException(status_code=404, detail="Donor profile not found")

    answer = db.query(models.PreScreeningAnswer).filter(models.PreScreeningAnswer.donor_id == donor.id).first()
    
    if answer:
        # Update existing
        for key, value in answer_in.model_dump().items():
            setattr(answer, key, value)
    else:
        # Create new
        answer = models.PreScreeningAnswer(**answer_in.model_dump(), donor_id=donor.id)
        db.add(answer)

    if not verify_pre_screening_answer_write(answer, current_user):
        raise HTTPException(status_code=403, detail="Not enough permissions")

    db.commit()
    db.refresh(answer)
    return answer
