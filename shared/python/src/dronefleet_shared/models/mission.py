from datetime import datetime

from pydantic import BaseModel

from .protocol import WaypointType
from .telemetry import Position


class Waypoint(BaseModel):
    type: WaypointType
    position: Position
    related_order_id: str | None = None
    related_warehouse_id: str | None = None


class Mission(BaseModel):
    id: str
    drone_id: str
    order_ids: list[str] = []
    route: list[Waypoint] = []
    status: str = "ACTIVE"  # ACTIVE, COMPLETED, FAILED
    start_time: datetime | None = None
    end_time: datetime | None = None
    estimated_battery_consumption: float | None = None
    estimated_duration_minutes: float | None = None
