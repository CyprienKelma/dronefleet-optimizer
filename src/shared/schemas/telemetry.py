from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field
from .protocol import DroneStatus

"""
Drones on flight -> Ingestion API -> Queue
"""

class GeoPoint(BaseModel):
    lat: float = Field(..., ge=-90, le=90) # Auto validation: Lat between -90 and 90
    lon: float = Field(..., ge=-180, le=180)

class DroneTelemetry(BaseModel):
    drone_id: str
    timestamp: datetime         # Exact time of the measurement
    position: GeoPoint
    battery_percentage: float = Field(..., ge=0, le=100)
    speed_kmh: float
    status: DroneStatus
    current_mission_id: Optional[str] = None # Null if IDLE

    class Config:
        json_schema_extra = {
            "example": {
                "drone_id": "DRONE-001",
                "timestamp": "2025-12-14T10:00:00Z",
                "position": {"lat": 50.629, "lon": 3.057},
                "battery_percentage": 85.5,
                "speed_kmh": 45.2,
                "status": "MOVING",
                "current_mission_id": "MISSION-123"
            }
        }
