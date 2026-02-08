from pydantic import BaseModel

from .mission import Waypoint


class MissionAssignment(BaseModel):
    """Decision for one drone with multi-order route."""

    drone_id: str
    order_ids: list[str]
    route: list[Waypoint]
    estimated_battery_consumption: float
    estimated_duration_minutes: float
