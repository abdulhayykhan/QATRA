import uuid
import hashlib
from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, Form, BackgroundTasks
from sqlalchemy.orm import Session
from app.api.deps import get_db
from app.api.deps import get_current_user, CurrentUser
from app.db import models
from app.schemas.requests import BloodRequestCreate, BloodRequestResponse, BloodRequestFeedItem
from app.core import storage
from app.services.geo_alert_service import trigger_geo_alert
from app.api.authorization import (
    apply_blood_request_policy,
    verify_blood_request_write,
    verify_hospital_slip_document_write,
    verify_seeker_or_admin
)
from typing import List

router = APIRouter()

@router.post("", response_model=BloodRequestResponse, status_code=201)
async def create_request(
    req: BloodRequestCreate,
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user)
):
    dummy_request = models.BloodRequest(seeker_auth_user_id=current_user.id)
    if not verify_blood_request_write(dummy_request, current_user):
        raise HTTPException(status_code=403, detail="Not authorized to create requests")
    hospital = db.query(models.Hospital).filter(models.Hospital.id == req.hospital_id).first()
    if not hospital:
        raise HTTPException(status_code=400, detail="Invalid hospital ID")

    # Initial status is VERIFYING until slip is uploaded and checked
    db_request = models.BloodRequest(
        seeker_auth_user_id=uuid.UUID(current_user.id),
        hospital_id=req.hospital_id,
        blood_group=req.blood_group,
        component=req.component,
        units_required=req.units_required,
        urgency=req.urgency,
        seeker_name=req.seeker_name,
        seeker_phone_masked=req.seeker_phone[:4] + "-XXXXXXX",
        seeker_cnic_masked=req.seeker_cnic[:5] + "-XXXXXXX-" + req.seeker_cnic[-1:],
        status="VERIFYING", 
        mrn_number=req.mrn_number,
        ocr_confidence=0,
        is_verified=False,
        doctor_stamp_verified=False
    )
    db.add(db_request)
    db.flush() # get id

    phone_hash = hashlib.sha256(req.seeker_phone.encode()).hexdigest()
    cnic_hash = hashlib.sha256(req.seeker_cnic.encode()).hexdigest()

    sensitive_data = models.RequestSensitiveData(
        request_id=db_request.id,
        seeker_phone_e164=req.seeker_phone,
        seeker_phone_hash=phone_hash,
        seeker_cnic=req.seeker_cnic,
        seeker_cnic_hash=cnic_hash,
    )
    db.add(sensitive_data)
    db.commit()
    
    return BloodRequestResponse(
        request_id=db_request.id,
        status=db_request.status,
        is_verified=db_request.is_verified,
        ocr_confidence=db_request.ocr_confidence
    )

@router.post("/{request_id}/hospital-slip")
async def upload_hospital_slip(
    request_id: uuid.UUID,
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    ocr_text: str = Form(""),
    ocr_confidence: int = Form(0),
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user)
):
    """
    Uploads a hospital slip to S3-compatible private storage and routes based on OCR confidence.
    """
    db_request = db.query(models.BloodRequest).filter(models.BloodRequest.id == request_id).first()
    if not db_request:
        raise HTTPException(status_code=404, detail="Request not found")
        
    if not verify_blood_request_write(db_request, current_user):
        raise HTTPException(status_code=403, detail="Not authorized to edit this request")

    file_content = await file.read()
    file_hash = hashlib.sha256(file_content).hexdigest()
    
    # Store in private bucket: hospital-slips/{request_id}/{file_hash}.jpg
    object_name = f"{request_id}/{file_hash}_{file.filename}"
    upload_success = storage.upload_file(
        file_content=file_content,
        bucket_name=storage.S3_HOSPITAL_SLIPS_BUCKET,
        object_name=object_name,
        content_type=file.content_type or "image/jpeg"
    )
    
    if not upload_success:
        raise HTTPException(status_code=500, detail="Failed to upload slip to storage backend")
        
    # Record the document metadata
    slip_doc = models.HospitalSlipDocument(
        request_id=request_id,
        document_kind="FRONT", # Defaulting to front
        storage_path=object_name,
        sha256_digest=file_hash,
        mrn=db_request.mrn_number,
        doctor_stamp_detected=False
    )
    
    # We assign the request object to slip_doc to allow the write verify check to work
    slip_doc.request = db_request
    if not verify_hospital_slip_document_write(slip_doc, current_user):
        raise HTTPException(status_code=403, detail="Not authorized to upload slip")

    db.add(slip_doc)
    
    # Update request OCR details
    db_request.ocr_confidence = ocr_confidence
    sensitive = db.query(models.RequestSensitiveData).filter_by(request_id=request_id).first()
    if sensitive:
        sensitive.raw_ocr_text = ocr_text

    # Routing Logic: If OCR confidence is high and we find the MRN, auto-broadcast.
    # Otherwise, queue for admin manual verification.
    CONFIDENCE_THRESHOLD = 80
    
    # Simple check if the MRN provided by user exists in the OCR text
    mrn_found = db_request.mrn_number.lower() in ocr_text.lower()
    
    if ocr_confidence >= CONFIDENCE_THRESHOLD and mrn_found:
        db_request.status = "BROADCASTING"
        db_request.is_verified = True
        background_tasks.add_task(trigger_geo_alert, request_id)
    else:
        db_request.status = "VERIFYING"
        # Push to manual verification queue
        hospital = db.query(models.Hospital).filter_by(id=db_request.hospital_id).first()
        queue_item = models.VerificationQueue(
            request_id=request_id,
            hospital_name=hospital.name if hospital else "Unknown",
            doctor_stamp_detected=False,
            mrn=db_request.mrn_number,
            blood_group=db_request.blood_group,
            units=db_request.units_required,
            ocr_confidence=ocr_confidence,
            blood_group_confidence=ocr_confidence,
            flag_warning="Low OCR confidence or MRN mismatch" if not mrn_found else "Low OCR confidence",
            status="Pending"
        )
        db.add(queue_item)

    db.commit()
    
    return {
        "detail": "Slip uploaded successfully", 
        "status": db_request.status,
        "is_verified": db_request.is_verified,
        "storage_path": object_name
    }

@router.get("/me", response_model=List[BloodRequestFeedItem])
async def get_my_requests(
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user)
):
    if not verify_seeker_or_admin(current_user):
        raise HTTPException(status_code=403, detail="Not authorized to view personal requests")
        
    query = db.query(models.BloodRequest)
    query = apply_blood_request_policy(query, current_user)
    
    return query.all()

@router.get("/feed", response_model=List[BloodRequestFeedItem])
async def get_request_feed(
    db: Session = Depends(get_db),
    current_user: CurrentUser = Depends(get_current_user)
):
    query = db.query(models.BloodRequest)
    query = apply_blood_request_policy(query, current_user)
    
    # Ordering by created_at desc to show newest first
    return query.order_by(models.BloodRequest.created_at.desc()).all()
