
import uuid
from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, Field

from .protocol import ActionType
from .telemetry import GeoPoint

"""
OR Optimizer -> Queue -> State Manager -> Drones
"""

class MissionAction(BaseModel):
    action_type: ActionType
    target_location: Optional[GeoPoint] = None # Null if the action is just "CHARGE" in place
    estimated_duration_seconds: int

class MissionOrder(BaseModel):
    mission_id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    drone_id: str
    assigned_at: datetime

    # Ordered list of things to do
    # Example: [FLY_TO(Warehouse), PICKUP, FLY_TO(Hospital), DROPOFF]
    sequence: List[MissionAction]

    # For tracking
    request_ids_covered: List[str] # Which packages does this mission handle
