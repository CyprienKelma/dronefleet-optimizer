from functools import lru_cache
from typing import Any

from fastapi import APIRouter, Depends, HTTPException, status

from myorg_shared.models.telemetry import DroneTelemetry

from ....services.telemetry import TelemetryService

router = APIRouter()


@lru_cache
def get_service() -> TelemetryService:
    return TelemetryService()


@router.post("/telemetry", status_code=status.HTTP_202_ACCEPTED)
async def ingest_telemetry(
    telemetry: DroneTelemetry, service: TelemetryService = Depends(get_service)
) -> dict[str, Any]:
    """
    Ingest high-frequency drone telemetry.

    - Validates payload
    - Pushes to 'telemetry' topic (fire and forget pattern mostly)
    """
    try:
        service.process_telemetry(telemetry)
        return {"status": "ACK", "drone_id": telemetry.drone_id}
    except Exception as e:
        # We don't want to break the drone's loop, but we must signal error
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail=str(e)
        ) from e
