from pydantic import BaseModel, Field

from src.shared.schemas.telemetry import GeoPoint


class MissionAssignment(BaseModel):
    drone_id: str = Field(...)
    order_id: str = Field(...)
    route: list[GeoPoint]

    class Config:
        populate_by_name = True
