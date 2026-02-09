from pydantic import BaseModel

from .telemetry import Position


class Depot(BaseModel):
    id: str
    name: str
    position: Position
    capacity: int
    charging_slots: int
