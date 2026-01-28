from pydantic import BaseModel, Field

from src.shared.schemas.telemetry import GeoPoint


class MissionAssignment(BaseModel):
    drone_id: str = Field(..., alias="drone_id")
    order_id: str = Field(..., alias="order_id")
    route: list[GeoPoint]

    class Config:
        populate_by_name = True
