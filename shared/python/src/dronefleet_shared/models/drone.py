from enum import Enum

from pydantic import BaseModel, Field

from .protocol import DroneStatus


class DroneModel(str, Enum):
    LIGHT_DELIVERY = "LIGHT_DELIVERY"  # Quadcopter, small payload (<2kg), agile
    HEAVY_LIFT = "HEAVY_LIFT"  # Hexacopter, heavy payload (up to 10kg)
    LONG_RANGE = "LONG_RANGE"  # Hybrid/VTOL, long distance, medium payload


class Drone(BaseModel):
    id: str = Field(..., description="Unique drone serial number")
    model: DroneModel

    # Physical Capabilities
    max_payload_kg: float = Field(..., gt=0)
    max_range_km: float = Field(..., gt=0)
    cruise_speed_kmh: float = Field(..., gt=0)

    # Battery specs
    battery_capacity_mah: int = Field(..., gt=0)
    current_battery_cycles: int = 0

    # Current State (Snapshot) - Part of entity definition in some contexts
    # or kept separate in Telemetry. Here we define the "Entity" definition.
    # However, sometimes we want the full view.
    # Let's keep it as static/inventory definition mainly, but status is useful.
    # The user asked for "id unique, le modèles, batterie maximal, etc..."

    # Operational constraints
    requires_maintenance: bool = False
    default_status: DroneStatus = DroneStatus.IDLE

    class Config:
        json_schema_extra = {
            "example": {
                "id": "DRONE-ALPHA-01",
                "model": "LIGHT_DELIVERY",
                "max_payload_kg": 2.5,
                "max_range_km": 15.0,
                "cruise_speed_kmh": 60.0,
                "battery_capacity_mah": 5000,
            }
        }
