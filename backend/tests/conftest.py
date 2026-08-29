import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool
from sqlalchemy.types import String

from app.main import app
from app.db.database import Base
from app.api.deps import get_db
from app.db import models
from app.core import security

# Setup an in-memory SQLite database for testing
# Note: In-memory SQLite does not support PostGIS/geoalchemy2 directly out of the box,
# but since we're only testing auth models right now, it might work if we don't query location.
# However, SQLAlchemy models that use Geography might fail on table creation with SQLite.
# To avoid issues, we should patch the models or use a proper test postgres instance.
# For this simple setup, we'll try it and if it fails, we can mock the DB session.

SQLALCHEMY_DATABASE_URL = "sqlite:///:memory:"

@pytest.fixture(scope="session")
def test_db():
    engine = create_engine(
        SQLALCHEMY_DATABASE_URL,
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    
    # Convert Geography columns to String for SQLite and remove geoalchemy2 events
    if hasattr(models.Hospital.__table__.c, 'location'):
        models.Hospital.__table__.c.location.type = String()
        models.Hospital.__table__.c.location.dispatch._clear()
    
    if hasattr(models.DonorLocation.__table__.c, 'location'):
        models.DonorLocation.__table__.c.location.type = String()
        models.DonorLocation.__table__.c.location.dispatch._clear()
        
    try:
        Base.metadata.create_all(bind=engine)
    except Exception as e:
        print(f"Failed to create tables: {e}")
    
    yield engine
    Base.metadata.drop_all(bind=engine)

@pytest.fixture
def db_session(test_db):
    TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=test_db)
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()

@pytest.fixture
def client(db_session):
    TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=db_session.get_bind())
    def override_get_db():
        yield db_session
            
    app.dependency_overrides[get_db] = override_get_db
    with TestClient(app) as test_client:
        yield test_client
    app.dependency_overrides.clear()

@pytest.fixture
def db(db_session):
    yield db_session

@pytest.fixture
def verified_seeker(db_session):
    import uuid
    user = models.User(id=uuid.uuid4(), role="verified_seeker", email="seeker@test.com", phone_number="+923000000001")
    db_session.add(user)
    db_session.commit()
    db_session.refresh(user)
    yield user
    db_session.delete(user)
    db_session.commit()

@pytest.fixture
def verified_donor(db_session):
    import uuid
    user = models.User(id=uuid.uuid4(), role="verified_donor", email="donor@test.com", phone_number="+923000000002")
    db_session.add(user)
    db_session.commit()
    db_session.refresh(user)
    
    donor_profile = models.DonorProfile(
        auth_user_id=user.id,
        display_name="Test Verified Donor",
        blood_group="O+",
    )
    db_session.add(donor_profile)
    db_session.commit()
    
    yield user
    
    db_session.delete(user)
    db_session.commit()

@pytest.fixture
def verified_seeker_token_headers(verified_seeker):
    token = security.create_access_token(subject=str(verified_seeker.id), role=verified_seeker.role)
    return {"Authorization": f"Bearer {token}"}

@pytest.fixture
def verified_donor_token_headers(verified_donor):
    token = security.create_access_token(subject=str(verified_donor.id), role=verified_donor.role)
    return {"Authorization": f"Bearer {token}"}
