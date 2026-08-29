from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
import uuid

from app.api import deps
from app.db import models
from app.schemas import feedback as schemas
from app.api.authorization import apply_request_feedback_policy, verify_request_feedback_write

router = APIRouter()

@router.post("/{request_id}", response_model=schemas.RequestFeedbackResponse)
def submit_feedback(
    request_id: uuid.UUID,
    feedback_in: schemas.RequestFeedbackCreate,
    db: Session = Depends(deps.get_db),
    current_user: deps.CurrentUser = Depends(deps.get_current_user)
):
    request = db.query(models.BloodRequest).filter(models.BloodRequest.id == request_id).first()
    if not request:
        raise HTTPException(status_code=404, detail="Request not found")

    feedback = models.RequestFeedback(**feedback_in.model_dump(), request_id=request_id)
    
    # We set the request object on feedback for the write verify check to work
    feedback.request = request 

    if not verify_request_feedback_write(feedback, current_user):
        raise HTTPException(status_code=403, detail="Not enough permissions to submit feedback for this request")
    
    db.add(feedback)
    db.commit()
    db.refresh(feedback)
    return feedback

@router.get("/{request_id}", response_model=List[schemas.RequestFeedbackResponse])
def get_feedback(
    request_id: uuid.UUID,
    db: Session = Depends(deps.get_db),
    current_user: deps.CurrentUser = Depends(deps.get_current_user)
):
    query = db.query(models.RequestFeedback).filter(models.RequestFeedback.request_id == request_id)
    query = apply_request_feedback_policy(query, current_user)
    return query.all()
