"""Initial schema

Revision ID: fddc8a76943a
Revises: 
Create Date: 2026-08-29 15:30:05.766715

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql
import geoalchemy2


# revision identifiers, used by Alembic.
revision: str = 'fddc8a76943a'
down_revision: Union[str, Sequence[str], None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # We create the postgis extension if it doesn't exist
    op.execute('CREATE EXTENSION IF NOT EXISTS postgis;')

    # 1. users
    op.create_table(
        'users',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('phone_number', sa.String(), nullable=True),
        sa.Column('email', sa.String(), nullable=True),
        sa.Column('hashed_password', sa.String(), nullable=True),
        sa.Column('totp_secret', sa.String(), nullable=True),
        sa.Column('role', sa.String(), nullable=True, server_default='guest'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
    )
    op.create_index(op.f('ix_users_email'), 'users', ['email'], unique=True)
    op.create_index(op.f('ix_users_phone_number'), 'users', ['phone_number'], unique=True)

    # 2. hospitals
    op.create_table(
        'hospitals',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('name', sa.String(), nullable=False),
        sa.Column('short_name', sa.String(), nullable=False),
        sa.Column('address', sa.String(), nullable=False),
        sa.Column('district', sa.String(), nullable=False),
        sa.Column('is_trauma_center', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('location', geoalchemy2.types.Geography(geometry_type='POINT', srid=4326, from_text='ST_GeogFromText', name='geography', nullable=True), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    # The GIST index on geography is usually automatically created by GeoAlchemy2, but we can enforce it:
    # op.create_index('idx_hospitals_location', 'hospitals', ['location'], postgresql_using='gist')

    # 3. awareness_articles
    op.create_table(
        'awareness_articles',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('title', sa.String(), nullable=False),
        sa.Column('category', sa.String(), nullable=False),
        sa.Column('read_time', sa.String(), nullable=False),
        sa.Column('summary', sa.String(), nullable=False),
        sa.Column('full_content', sa.String(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )

    # 4. donor_profiles
    op.create_table(
        'donor_profiles',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('auth_user_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id'), unique=True, nullable=True),
        sa.Column('display_name', sa.String(), nullable=False),
        sa.Column('blood_group', sa.String(), nullable=False),
        sa.Column('phone_masked', sa.String(), nullable=False, server_default='0300-XXXXXXX'),
        sa.Column('cnic_masked', sa.String(), nullable=False, server_default='42101-XXXXXXX-7'),
        sa.Column('is_available_to_donate', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('is_eligible', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('cooldown_days_remaining', sa.SmallInteger(), nullable=False, server_default='0'),
        sa.Column('lifetime_donations', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('tier', sa.String(), nullable=False, server_default='Silver Tier'),
        sa.Column('district', sa.String(), nullable=False, server_default='Karachi South'),
        sa.Column('is_cnic_verified', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )

    # 5. donor_locations
    op.create_table(
        'donor_locations',
        sa.Column('donor_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('donor_profiles.id', ondelete='CASCADE'), primary_key=True),
        sa.Column('latitude', sa.Float(), nullable=False),
        sa.Column('longitude', sa.Float(), nullable=False),
        sa.Column('location', geoalchemy2.types.Geography(geometry_type='POINT', srid=4326, from_text='ST_GeogFromText', name='geography', nullable=False), nullable=False),
        sa.Column('source', sa.String(), nullable=False, server_default='device'),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    # op.create_index('idx_donor_locations_location', 'donor_locations', ['location'], postgresql_using='gist')

    # 6. donor_device_tokens
    op.create_table(
        'donor_device_tokens',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('donor_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('donor_profiles.id', ondelete='CASCADE'), nullable=False),
        sa.Column('token', sa.String(), nullable=False, unique=True),
        sa.Column('platform', sa.String(), nullable=False, server_default='android'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_donor_device_tokens_donor_id'), 'donor_device_tokens', ['donor_id'], unique=False)

    # 7. donor_private_contacts
    op.create_table(
        'donor_private_contacts',
        sa.Column('donor_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('donor_profiles.id', ondelete='CASCADE'), primary_key=True),
        sa.Column('phone_e164', sa.String(), nullable=False),
        sa.Column('cnic', sa.String(), nullable=False),
        sa.Column('cnic_hash', sa.String(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )

    # 8. donor_cnic_documents
    op.create_table(
        'donor_cnic_documents',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('donor_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('donor_profiles.id', ondelete='CASCADE'), nullable=False),
        sa.Column('document_kind', sa.String(), nullable=False),
        sa.Column('storage_path', sa.String(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )

    # 9. blood_requests
    op.create_table(
        'blood_requests',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('seeker_auth_user_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('users.id'), nullable=True),
        sa.Column('hospital_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('hospitals.id'), nullable=False),
        sa.Column('blood_group', sa.String(), nullable=False),
        sa.Column('component', sa.String(), nullable=False),
        sa.Column('units_required', sa.Integer(), nullable=False),
        sa.Column('urgency', sa.String(), nullable=False),
        sa.Column('seeker_name', sa.String(), nullable=False),
        sa.Column('seeker_phone_masked', sa.String(), nullable=False, server_default='0300-XXXXXXX'),
        sa.Column('seeker_cnic_masked', sa.String(), nullable=False, server_default='42101-XXXXXXX-1'),
        sa.Column('status', sa.String(), nullable=False),
        sa.Column('active_donors_in_radius', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('responded_donors_count', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('mrn_number', sa.String(), nullable=False),
        sa.Column('ocr_confidence', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('is_verified', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('doctor_stamp_verified', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )
    op.create_index(op.f('ix_blood_requests_seeker_auth_user_id'), 'blood_requests', ['seeker_auth_user_id'], unique=False)

    # 10. request_sensitive_data
    op.create_table(
        'request_sensitive_data',
        sa.Column('request_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('blood_requests.id', ondelete='CASCADE'), primary_key=True),
        sa.Column('seeker_phone_e164', sa.String(), nullable=True),
        sa.Column('seeker_phone_hash', sa.String(), nullable=False),
        sa.Column('seeker_cnic', sa.String(), nullable=False),
        sa.Column('seeker_cnic_hash', sa.String(), nullable=False),
        sa.Column('raw_ocr_text', sa.String(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )

    # 11. hospital_slip_documents
    op.create_table(
        'hospital_slip_documents',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('request_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('blood_requests.id', ondelete='CASCADE'), nullable=False),
        sa.Column('document_kind', sa.String(), nullable=False),
        sa.Column('storage_path', sa.String(), nullable=False),
        sa.Column('sha256_digest', sa.String(), nullable=False),
        sa.Column('mrn', sa.String(), nullable=True),
        sa.Column('doctor_stamp_detected', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )

    # 12. matched_donor_requests
    op.create_table(
        'matched_donor_requests',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('request_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('blood_requests.id', ondelete='CASCADE'), nullable=False),
        sa.Column('donor_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('donor_profiles.id', ondelete='CASCADE'), nullable=False),
        sa.Column('blood_group', sa.String(), nullable=False),
        sa.Column('distance_km', sa.Float(), nullable=False, server_default='0.0'),
        sa.Column('eta_minutes', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('status_text', sa.String(), nullable=False),
        sa.Column('phone_masked', sa.String(), nullable=False, server_default='0300-XXXXXXX'),
        sa.Column('is_verified', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('lifetime_donations', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )

    # 13. verification_queue
    op.create_table(
        'verification_queue',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('request_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('blood_requests.id', ondelete='CASCADE'), nullable=False),
        sa.Column('hospital_name', sa.String(), nullable=False),
        sa.Column('doctor_stamp_detected', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('mrn', sa.String(), nullable=False),
        sa.Column('blood_group', sa.String(), nullable=False),
        sa.Column('units', sa.Integer(), nullable=False),
        sa.Column('ocr_confidence', sa.Integer(), nullable=False),
        sa.Column('blood_group_confidence', sa.Integer(), nullable=False),
        sa.Column('flag_warning', sa.String(), nullable=True),
        sa.Column('status', sa.String(), nullable=False, server_default='Pending'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )

    # 14. fraud_audit_items
    op.create_table(
        'fraud_audit_items',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('request_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('blood_requests.id', ondelete='CASCADE'), nullable=False),
        sa.Column('seeker_cnic_masked', sa.String(), nullable=False),
        sa.Column('phone_masked', sa.String(), nullable=False),
        sa.Column('hospital_mrn', sa.String(), nullable=False),
        sa.Column('ocr_confidence', sa.Integer(), nullable=False),
        sa.Column('flag_reason', sa.String(), nullable=False),
        sa.Column('action_status', sa.String(), nullable=False, server_default='Flagged'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )

    # 15. campus_drives
    op.create_table(
        'campus_drives',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('title', sa.String(), nullable=False),
        sa.Column('university_venue', sa.String(), nullable=False),
        sa.Column('target_quota_units', sa.Integer(), nullable=False),
        sa.Column('registered_donors', sa.Integer(), nullable=False, server_default='0'),
        sa.Column('date_str', sa.String(), nullable=False),
        sa.Column('time_str', sa.String(), nullable=False),
        sa.Column('status', sa.String(), nullable=False, server_default='Scheduled'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )

    # 16. drive_attendees
    op.create_table(
        'drive_attendees',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('drive_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('campus_drives.id', ondelete='CASCADE'), nullable=False),
        sa.Column('donor_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('donor_profiles.id', ondelete='CASCADE'), nullable=False),
        sa.Column('name', sa.String(), nullable=False),
        sa.Column('dept_year', sa.String(), nullable=True),
        sa.Column('cnic_status', sa.String(), nullable=False, server_default='Verified'),
        sa.Column('pre_screening_status', sa.String(), nullable=False, server_default='Passed'),
        sa.Column('check_in_status', sa.String(), nullable=False, server_default='Checked In 10:42 AM'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )

    # 17. pre_screening_answers
    op.create_table(
        'pre_screening_answers',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('donor_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('donor_profiles.id', ondelete='CASCADE'), nullable=False, unique=True),
        sa.Column('age_valid', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('weight_valid', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('no_recent_illness', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('no_recent_donation', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('no_recent_tattoo_or_surgery', sa.Boolean(), nullable=False, server_default='true'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )

    # 18. firebase_phone_token_ledger
    op.create_table(
        'firebase_phone_token_ledger',
        sa.Column('token_hash', sa.String(), primary_key=True),
        sa.Column('firebase_uid', sa.String(), nullable=False),
        sa.Column('phone', sa.String(), nullable=False),
        sa.Column('expires_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )

    # 19. request_feedback
    op.create_table(
        'request_feedback',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('request_id', postgresql.UUID(as_uuid=True), sa.ForeignKey('blood_requests.id', ondelete='CASCADE'), nullable=False),
        sa.Column('rating', sa.Integer(), nullable=False),
        sa.Column('note', sa.String(), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
    )

def downgrade() -> None:
    op.drop_table('request_feedback')
    op.drop_table('firebase_phone_token_ledger')
    op.drop_table('pre_screening_answers')
    op.drop_table('drive_attendees')
    op.drop_table('campus_drives')
    op.drop_table('fraud_audit_items')
    op.drop_table('verification_queue')
    op.drop_table('matched_donor_requests')
    op.drop_table('hospital_slip_documents')
    op.drop_table('request_sensitive_data')
    op.drop_index(op.f('ix_blood_requests_seeker_auth_user_id'), table_name='blood_requests')
    op.drop_table('blood_requests')
    op.drop_table('donor_cnic_documents')
    op.drop_table('donor_private_contacts')
    op.drop_index(op.f('ix_donor_device_tokens_donor_id'), table_name='donor_device_tokens')
    op.drop_table('donor_device_tokens')
    op.drop_index('idx_donor_locations_location', table_name='donor_locations', postgresql_using='gist')
    op.drop_table('donor_locations')
    op.drop_table('donor_profiles')
    op.drop_table('awareness_articles')
    op.drop_index('idx_hospitals_location', table_name='hospitals', postgresql_using='gist')
    op.drop_table('hospitals')
    op.drop_index(op.f('ix_users_phone_number'), table_name='users')
    op.drop_index(op.f('ix_users_email'), table_name='users')
    op.drop_table('users')
    op.execute('DROP EXTENSION IF EXISTS postgis;')
