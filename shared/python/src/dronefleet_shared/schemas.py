from datetime import datetime

from pydantic import BaseModel, Field

from .models import DroneStatus, OrderPriority, OrderStatus


class PositionSchema(BaseModel):
    """Pydantic schema for Position with range validation."""

    lat: float = Field(..., ge=-90, le=90)
    lon: float = Field(..., ge=-180, le=180)


class DroneTelemetrySchema(BaseModel):
    """Pydantic schema for Drone Telemetry ingestion."""

    drone_id: str
    timestamp: datetime
    position: PositionSchema
    battery_percentage: float = Field(..., ge=0, le=100)
    speed_kmh: float
    status: DroneStatus
    current_mission_id: str | None = None

    class Config:
        json_schema_extra = {
            "example": {
                "drone_id": "DRONE-001",
                "timestamp": "2025-12-14T10:00:00Z",
                "position": {"lat": 50.629, "lon": 3.057},
                "battery_percentage": 85.5,
                "speed_kmh": 45.2,
                "status": "MOVING",
                "current_mission_id": "MISSION-123",
            }
        }


class OrderSchema(BaseModel):
    """Pydantic schema for Order ingestion."""

    id: str
    pickup_location: PositionSchema
    delivery_location: PositionSchema
    status: OrderStatus = OrderStatus.ORDER_STATUS_PENDING
    priority: OrderPriority = OrderPriority.ORDER_PRIORITY_STANDARD
    product_type: str
    created_at: datetime | None = None
    assigned_drone_id: str | None = None
    assigned_mission_id: str | None = None
    solving_session_id: str | None = None

    class Config:
        json_schema_extra = {
            "example": {
                "id": "ORDER-01",
                "pickup_location": {"lat": 48.85, "lon": 2.35},
                "delivery_location": {"lat": 48.90, "lon": 2.40},
                "product_type": "BLOOD",
                "priority": "HIGH",
            }
        }
