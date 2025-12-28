from typing import List, Optional
from pydantic import BaseModel, Field
from .telemetry import GeoPoint
from .product import ProductType

class Warehouse(BaseModel):
    id: str = Field(..., description="Unique warehouse ID")
    name: str = Field(..., description="Human friendly name")
    
    location: GeoPoint
    
    # Storage capabilities
    total_capacity_kg: float = Field(..., description="Total weight capacity")
    is_cold_storage_capable: bool = False
    
    # Logistics
    landing_pads_count: int = Field(default=1, ge=0)
    charging_stations_count: int = Field(default=1, ge=0)
    
    # Authorized products to store here
    authorized_product_types: List[ProductType] = Field(default_factory=list)
    
    description: Optional[str] = None

    class Config:
        json_schema_extra = {
            "example": {
                "id": "WH-CENTRAL-01",
                "name": "Central Hospital Hub",
                "location": {"lat": 48.8566, "lon": 2.3522},
                "total_capacity_kg": 500.0,
                "is_cold_storage_capable": True,
                "landing_pads_count": 4,
                "authorized_product_types": ["MEDICINE", "VACCINE", "BLOOD"]
            }
        }

