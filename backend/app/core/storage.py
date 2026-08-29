import os
import uuid
import boto3
from botocore.exceptions import ClientError
from typing import Optional

# Configuration for S3-compatible storage (Targeting Supabase Storage S3 API)
# Fallbacks have been removed to prevent silent failures in production
try:
    AWS_ACCESS_KEY_ID = os.environ["S3_ACCESS_KEY_ID"]
    AWS_SECRET_ACCESS_KEY = os.environ["S3_SECRET_ACCESS_KEY"]
    AWS_REGION = os.environ.get("S3_REGION", "us-east-1") # Region is often static for Supabase
    S3_ENDPOINT_URL = os.environ["S3_ENDPOINT_URL"]
    S3_HOSPITAL_SLIPS_BUCKET = os.environ["S3_HOSPITAL_SLIPS_BUCKET"]
    S3_CNIC_DOCUMENTS_BUCKET = os.environ["S3_CNIC_DOCUMENTS_BUCKET"]
except KeyError as e:
    raise RuntimeError(f"Missing required storage environment variable: {e}")

s3_client = boto3.client(
    's3',
    endpoint_url=S3_ENDPOINT_URL,
    aws_access_key_id=AWS_ACCESS_KEY_ID,
    aws_secret_access_key=AWS_SECRET_ACCESS_KEY,
    region_name=AWS_REGION
)

def _ensure_bucket_exists(bucket_name: str):
    """Ensure the bucket exists in the storage provider, mostly for local dev."""
    try:
        s3_client.head_bucket(Bucket=bucket_name)
    except ClientError as e:
        error_code = e.response.get('Error', {}).get('Code')
        if error_code == '404':
            s3_client.create_bucket(Bucket=bucket_name)

def upload_file(file_content: bytes, bucket_name: str, object_name: str, content_type: str = "image/jpeg") -> bool:
    """
    Upload a file directly to the S3 compatible backend.
    In a high-scale production app, generating a presigned URL for direct 
    client->S3 upload is preferable, but streaming through FastAPI allows 
    virus scanning and payload validation on sensitive documents.
    """
    try:
        _ensure_bucket_exists(bucket_name)
        s3_client.put_object(
            Bucket=bucket_name,
            Key=object_name,
            Body=file_content,
            ContentType=content_type
        )
        return True
    except ClientError as e:
        print(f"Failed to upload to S3: {e}")
        return False

def get_presigned_url(bucket_name: str, object_name: str, expiration: int = 3600) -> Optional[str]:
    """
    Generate a presigned URL to share the S3 object securely.
    """
    try:
        response = s3_client.generate_presigned_url(
            'get_object',
            Params={'Bucket': bucket_name, 'Key': object_name},
            ExpiresIn=expiration
        )
        return response
    except ClientError as e:
        print(f"Failed to generate presigned URL: {e}")
        return None
