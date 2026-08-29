from fastapi import FastAPI, APIRouter
from fastapi.middleware.cors import CORSMiddleware
from app.api.endpoints import auth, donor, requests, matching, contact, awareness, prescreening, drives, feedback, system

app = FastAPI(title="QATRA API")
api_router = APIRouter()

app.include_router(auth.router, prefix="/auth", tags=["auth"])
app.include_router(donor.router, prefix="/api/donors", tags=["donors"])
api_router.include_router(matching.router, prefix="/matching", tags=["matching"])
api_router.include_router(requests.router, prefix="/requests", tags=["requests"])
api_router.include_router(contact.router, prefix="/contact", tags=["contact"])
api_router.include_router(awareness.router, prefix="/awareness", tags=["awareness"])
api_router.include_router(prescreening.router, prefix="/prescreening", tags=["prescreening"])
api_router.include_router(drives.router, prefix="/drives", tags=["drives"])
api_router.include_router(feedback.router, prefix="/feedback", tags=["feedback"])
api_router.include_router(system.router, prefix="/system", tags=["system"])

app.include_router(api_router, prefix="/api")

@app.get("/")
def root():
    return {"message": "QATRA API is running"}
