from datetime import datetime

from pydantic import BaseModel
from src.shared.schemas.telemetry import GeoPoint


class DroneSnapshot(BaseModel):
    id: str
    position: GeoPoint
    battery_percentage: float
    home_depot_id: str | None = None
    status: str


class OrderSnapshot(BaseModel):
    id: str
    pickup_location: GeoPoint
    delivery_location: GeoPoint
    priority: str
    product_type: str


class WarehouseSnapshot(BaseModel):
    id: str
    name: str
    position: GeoPoint
    authorized_product_types: list[str]
    is_cold_storage_capable: bool


class OptimizationSnapshot(BaseModel):
    session_id: str
    timestamp: datetime
    drones: list[DroneSnapshot]
    orders: list[OrderSnapshot]
    warehouses: list[WarehouseSnapshot]
