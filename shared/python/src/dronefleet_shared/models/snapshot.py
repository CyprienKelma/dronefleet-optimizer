from datetime import datetime

from pydantic import BaseModel

from .depot import Depot
from .drone import Drone
from .order import Order
from .warehouse import Warehouse


class OptimizationSnapshot(BaseModel):
    session_id: str
    timestamp: datetime
    depot: Depot
    drones: list[Drone]
    orders: list[Order]
    warehouses: list[Warehouse]
