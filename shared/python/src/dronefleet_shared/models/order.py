from datetime import datetime

from pydantic import BaseModel, Field

from .protocol import OrderPriority, OrderStatus
from .telemetry import Position


class Order(BaseModel):
    id: str
    pickup_location: Position
    delivery_location: Position
    status: OrderStatus = OrderStatus.PENDING
    priority: OrderPriority = OrderPriority.STANDARD
    product_type: str
    created_at: datetime = Field(default_factory=datetime.utcnow)
    assigned_drone_id: str | None = None
    assigned_mission_id: str | None = None
    solving_session_id: str | None = None

    @property
    def max_delivery_time_minutes(self) -> int:
        """Calculate deadline based on priority (minutes from creation)."""
        if self.priority == OrderPriority.CRITICAL:
            return 15
        elif self.priority == OrderPriority.HIGH:
            return 30
        return 60

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
