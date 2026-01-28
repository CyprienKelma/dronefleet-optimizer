import uuid
from datetime import datetime

from pydantic import BaseModel, Field

from .product import ProductType
from .protocol import UrgencyLevel
from .telemetry import GeoPoint

"""
Simulator -> Ingestion API -> Queue
"""


class DeliveryOrder(BaseModel):
    order_id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    created_at: datetime = Field(default_factory=datetime.utcnow)
    priority: UrgencyLevel

    # Locations
    pickup_location: GeoPoint  # Where to pick up the package (e.g., Central warehouse)
    dropoff_location: GeoPoint  # Where to deliver (e.g., South Hospital)

    # Package Details
    product_type: ProductType
    package_weight_kg: float = Field(
        ..., gt=0, description="Total weight including packaging"
    )
    content_description: str  # "Covid Vaccines", "O+ Blood"

    # Constraints
    requires_cold_chain: bool = False

    # Metadata
    requester_id: str | None = None  # Who asked for this (Doctor ID, Hospital ID)

    class Config:
        json_schema_extra = {
            "example": {
                "priority": "HIGH",
                "pickup_location": {"lat": 48.85, "lon": 2.35},
                "dropoff_location": {"lat": 48.90, "lon": 2.40},
                "product_type": "BLOOD",
                "package_weight_kg": 1.2,
                "content_description": "O- Negative Blood Bags",
                "requires_cold_chain": True,
            }
        }
