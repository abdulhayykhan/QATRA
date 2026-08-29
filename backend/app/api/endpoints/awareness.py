from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List

from app.api import deps
from app.db import models
from app.schemas import awareness as schemas
from app.api.authorization import apply_awareness_article_policy, verify_awareness_article_write

router = APIRouter()

@router.get("/", response_model=List[schemas.AwarenessArticleResponse])
def read_articles(
    db: Session = Depends(deps.get_db),
    current_user: deps.CurrentUser = Depends(deps.get_current_user)
):
    query = db.query(models.AwarenessArticle)
    query = apply_awareness_article_policy(query, current_user)
    return query.all()

@router.post("/", response_model=schemas.AwarenessArticleResponse)
def create_article(
    article_in: schemas.AwarenessArticleCreate,
    db: Session = Depends(deps.get_db),
    current_user: deps.CurrentUser = Depends(deps.get_current_user)
):
    article = models.AwarenessArticle(**article_in.model_dump())
    if not verify_awareness_article_write(article, current_user):
        raise HTTPException(status_code=403, detail="Not enough permissions")
    
    db.add(article)
    db.commit()
    db.refresh(article)
    return article
