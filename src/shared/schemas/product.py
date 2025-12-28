from enum import Enum
from typing import Optional
from pydantic import BaseModel, Field

class ProductType(str, Enum):
    MEDICINE = "MEDICINE"           # Standard pills, syrups
    VACCINE = "VACCINE"             # Requires cold chain
    BLOOD = "BLOOD"                 # Critical, time-sensitive, temp sensitive
    ORGAN = "ORGAN"                 # Extremely critical, strict handling
    MEDICAL_DEVICE = "MEDICAL_DEVICE" # Defibrillators, EPIPens, Kits

class Product(BaseModel):
    id: str = Field(..., description="Unique product SKU or ID")
    name: str = Field(..., description="Human readable name of the product")
    product_type: ProductType
    weight_kg: float = Field(..., gt=0, description="Weight of one unit in KG")
    
    # Conservation constraints
    requires_cold_chain: bool = False
    min_temperature_celsius: Optional[float] = None
    max_temperature_celsius: Optional[float] = None
    
    is_fragile: bool = False
    
    class Config:
        json_schema_extra = {
            "example": {
                "id": "PROD-001",
                "name": "Pfizer Vaccine Dose",
                "product_type": "VACCINE",
                "weight_kg": 0.05,
                "requires_cold_chain": True,
                "min_temperature_celsius": -70.0,
                "max_temperature_celsius": -10.0
            }
        }

