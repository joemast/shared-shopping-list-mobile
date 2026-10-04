"""Health check endpoint (AC1)."""

from fastapi import APIRouter

from app.schemas import HealthStatus

router = APIRouter(tags=["health"])


@router.get("/health", response_model=HealthStatus)
def health() -> HealthStatus:
    """Return the locked liveness body ``{"status": "ok"}``."""

    return HealthStatus(status="ok")
