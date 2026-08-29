from fastapi import APIRouter, HTTPException
import httpx
from pydantic import BaseModel

router = APIRouter()

class VersionResponse(BaseModel):
    latest_version: str
    download_url: str

@router.get("/version", response_model=VersionResponse)
async def get_version():
    """
    Fetches the latest app release version from GitHub API.
    Returns the version tag (e.g., '1.0.0') and the direct download URL for the APK.
    """
    try:
        async with httpx.AsyncClient() as client:
            # Public GitHub repo API doesn't strictly need auth for rate limit of 60 req/hr
            # But in production, you might want a Personal Access Token or caching.
            response = await client.get(
                "https://api.github.com/repos/abdulhayykhan/QATRA/releases/latest",
                headers={"Accept": "application/vnd.github.v3+json"},
                timeout=5.0
            )
            response.raise_for_status()
            data = response.json()
            
            tag_name = data.get("tag_name", "0.0.0").lstrip("v")
            html_url = data.get("html_url", "https://github.com/abdulhayykhan/QATRA/releases/latest")
            
            # Prefer a direct APK link if one exists
            download_url = html_url
            for asset in data.get("assets", []):
                name = asset.get("name", "")
                if name.endswith(".apk") and "arm64-v8a" in name:
                    download_url = asset.get("browser_download_url", html_url)
                    break
                elif name.endswith(".apk"):
                    # Fallback to any APK if arm64 isn't specifically found
                    download_url = asset.get("browser_download_url", html_url)
            
            return VersionResponse(
                latest_version=tag_name,
                download_url=download_url
            )
    except Exception as e:
        # Graceful fallback: return 503 so the client knows it couldn't be fetched
        raise HTTPException(status_code=503, detail="Unable to fetch latest version")
