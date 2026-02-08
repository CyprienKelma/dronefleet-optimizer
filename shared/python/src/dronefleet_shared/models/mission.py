import uuid
from datetime import datetime

from pydantic import BaseModel, Field

from .protocol import WaypointType
from .telemetry import GeoPoint

"""
OR Optimizer -> Queue -> State Manager -> Drones
"""


class Waypoint(BaseModel):
    type: WaypointType
    position: GeoPoint
    related_order_id: str | None = None
    related_warehouse_id: str | None = None


class MissionOrder(BaseModel):
    mission_id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    drone_id: str
    assigned_at: datetime = Field(default_factory=datetime.utcnow)

    # Ordered list of things to do
    route: list[Waypoint]

    # For tracking
    order_ids: list[str]  # Which packages does this mission handle

    estimated_battery_consumption: float | None = None
    estimated_duration_minutes: float | None = None
