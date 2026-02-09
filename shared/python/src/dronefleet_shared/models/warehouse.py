from pydantic import BaseModel

from .telemetry import Position


class Warehouse(BaseModel):
    id: str
    name: str
    position: Position
    authorized_product_types: list[str] = []
    is_cold_storage_capable: bool = False
