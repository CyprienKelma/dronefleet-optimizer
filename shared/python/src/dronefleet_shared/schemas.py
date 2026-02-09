from datetime import datetime
from typing import Annotated, Any, TypeVar

from pydantic import (
    AliasChoices,
    BaseModel,
    BeforeValidator,
    ConfigDict,
    Field,
    field_validator,
)
from pydantic.alias_generators import to_camel

from .models import DroneStatus, OrderPriority, OrderStatus

T = TypeVar("T")


def validate_betterproto_enum(v: Any, enum_cls: type[T]) -> int:
    """Validator to handle betterproto enums from strings or ints."""
    if isinstance(v, int):
        # betterproto enums are just ints, so we return it directly
        return v

    if isinstance(v, str):
        v_upper = v.upper()

        # List of candidate names to try
        candidates = [v_upper]

        prefix = ""
        if enum_cls == DroneStatus:
            prefix = "DRONE_STATUS_"
        elif enum_cls == OrderPriority:
            prefix = "ORDER_PRIORITY_"
        elif enum_cls == OrderStatus:
            prefix = "ORDER_STATUS_"

        if prefix and not v_upper.startswith(prefix):
            candidates.append(prefix + v_upper)

        for name in candidates:
            # betterproto enums are class attributes that are ints
            if hasattr(enum_cls, name):
                return int(getattr(enum_cls, name))

    raise ValueError(f"Invalid {enum_cls.__name__}: {v}")


# Annotated types for betterproto enums (using int as base type to avoid Pydantic isinstance checks)
PydanticDroneStatus = Annotated[
    int, BeforeValidator(lambda v: validate_betterproto_enum(v, DroneStatus))
]
PydanticOrderPriority = Annotated[
    int,
    BeforeValidator(lambda v: validate_betterproto_enum(v, OrderPriority)),
]
PydanticOrderStatus = Annotated[
    int, BeforeValidator(lambda v: validate_betterproto_enum(v, OrderStatus))
]


class PositionSchema(BaseModel):
    """Pydantic schema for Position with range validation."""

    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
    )

    lat: float = Field(..., ge=-90, le=90)
    lon: float = Field(..., ge=-180, le=180)


class DroneTelemetrySchema(BaseModel):
    """Pydantic schema for Drone Telemetry ingestion."""

    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        arbitrary_types_allowed=True,
        json_schema_extra={
            "example": {
                "drone_id": "DRONE-001",
                "timestamp": "2025-12-14T10:00:00Z",
                "position": {"lat": 50.629, "lon": 3.057},
                "battery_percentage": 85.5,
                "speed_kmh": 45.2,
                "status": "MOVING",
                "current_mission_id": "MISSION-123",
            }
        },
    )

    drone_id: str
    timestamp: datetime
    position: PositionSchema
    battery_percentage: float = Field(0.0, ge=0, le=100)
    speed_kmh: float = 0.0
    status: PydanticDroneStatus = 0
    current_mission_id: str | None = None


class OrderSchema(BaseModel):
    """Pydantic schema for Order ingestion."""

    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        arbitrary_types_allowed=True,
        json_schema_extra={
            "example": {
                "id": "ORDER-01",
                "pickup_location": {"lat": 48.85, "lon": 2.35},
                "delivery_location": {"lat": 48.90, "lon": 2.40},
                "product_type": "BLOOD",
                "priority": "HIGH",
            }
        },
    )

    id: str | None = Field(default=None)
    pickup_location: PositionSchema
    delivery_location: PositionSchema = Field(
        ..., validation_alias=AliasChoices("delivery_location", "dropoff_location", "deliveryLocation")
    )
    status: PydanticOrderStatus = OrderStatus.ORDER_STATUS_PENDING
    priority: PydanticOrderPriority = OrderPriority.ORDER_PRIORITY_STANDARD
    product_type: str = ""
    created_at: datetime | None = None
    assigned_drone_id: str | None = None
    assigned_mission_id: str | None = None
    solving_session_id: str | None = None

    @field_validator("id", mode="before")
    @classmethod
    def ensure_id(cls, v: str | None) -> str:
        import uuid

        if not v:
            return f"ORDER-{uuid.uuid4().hex[:8].upper()}"
        return v
