from dronefleet_shared.models.telemetry import GeoPoint
from pydantic import BaseModel, Field


class MissionAssignment(BaseModel):
    drone_id: str = Field(...)
    order_id: str = Field(...)
    route: list[GeoPoint]

    class Config:
        populate_by_name = True
