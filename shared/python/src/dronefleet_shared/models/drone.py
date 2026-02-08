from datetime import datetime

from pydantic import BaseModel, Field

from .protocol import DroneStatus
from .telemetry import Position


class Drone(BaseModel):
    id: str
    position: Position | None = None
    battery_percentage: float = Field(default=100.0, ge=0, le=100)
    speed_kmh: float = 0.0
    status: DroneStatus = DroneStatus.IDLE
    current_mission_id: str | None = None
    last_update: datetime | None = None
    solving_session_id: str | None = None
    home_depot_id: str | None = None

    # Battery calculation metadata
    battery_capacity_mah: int = 5000
    consumption_per_km: float = 2.5  # % per km
    max_flight_time_minutes: int = 30

    class Config:
        json_schema_extra = {
            "example": {
                "id": "DRONE-01",
                "status": "IDLE",
                "battery_percentage": 95.0,
                "home_depot_id": "DEPOT-01",
            }
        }
