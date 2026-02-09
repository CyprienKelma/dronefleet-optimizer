from functools import lru_cache
from typing import Any

from dronefleet_shared.models import DroneTelemetry
from dronefleet_shared.schemas import DroneTelemetrySchema
from fastapi import APIRouter, Depends, HTTPException, status

from ....services.telemetry import TelemetryService

router = APIRouter()


@lru_cache
def get_service() -> TelemetryService:
    return TelemetryService()


@router.post("/telemetry", status_code=status.HTTP_202_ACCEPTED)
async def ingest_telemetry(
    telemetry_in: DroneTelemetrySchema, service: TelemetryService = Depends(get_service)
) -> dict[str, Any]:
    """
    Ingest high-frequency drone telemetry.

    - Validates payload
    - Pushes to 'telemetry' topic (fire and forget pattern mostly)
    """
    try:
        # Map Schema to Shared Model
        # Use mode="json" to ensure datetimes are serialized to strings for betterproto
        telemetry_data = telemetry_in.model_dump(mode="json")
        telemetry = DroneTelemetry().from_dict(telemetry_data)

        service.process_telemetry(telemetry)
        return {"status": "ACK", "drone_id": telemetry.drone_id}
    except Exception as e:
        # We don't want to break the drone's loop, but we must signal error
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail=str(e)
        ) from e
