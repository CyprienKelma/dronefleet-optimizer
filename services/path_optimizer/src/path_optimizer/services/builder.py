import math
from typing import NamedTuple

import structlog
from dronefleet_shared.models import OptimizationSnapshot, OrderPriority

logger = structlog.get_logger(__name__)


def get_max_delivery_time_minutes(priority: OrderPriority) -> int:
    """Calculate deadline based on priority (minutes from creation)."""
    if priority == OrderPriority.ORDER_PRIORITY_CRITICAL:
        return 15
    elif priority == OrderPriority.ORDER_PRIORITY_HIGH:
        return 30
    return 60


class VRPProblem(NamedTuple):
    """Complete VRP problem definition."""

    distance_matrix: list[list[int]]  # meters
    time_matrix: list[list[int]]  # seconds

    # Nodes structure
    depot_node: int  # Index 0
    warehouse_nodes: list[int]  # Indices 1..N
    delivery_nodes: list[int]  # Indices N+1..M
    node_locations: list[tuple[float, float]]

    # Pickups & Deliveries with warehouse choices
    # Each order can be picked up from MULTIPLE compatible warehouses
    pickups_deliveries: list[tuple[list[int], int]]  # ([pickup_options], delivery_node)

    # Constraints
    num_vehicles: int
    vehicle_capacities: list[int]  # Always [1,1,1,...] but kept for clarity
    initial_battery_pct: list[float]  # Starting battery per drone
    time_windows: list[tuple[int, int]]  # (earliest, latest) in seconds

    # Metadata for solution extraction
    drone_ids: list[str]
    order_ids: list[str]
    warehouse_ids: list[str]
    delivery_node_to_order_id: dict[int, str]
    warehouse_node_to_warehouse_id: dict[int, str]


class VRPProblemBuilder:
    """Build VRP problem from optimization snapshot."""

    def __init__(self, snapshot: OptimizationSnapshot):
        self.snapshot = snapshot
        self.drones = snapshot.drones
        self.orders = snapshot.orders
        self.warehouses = snapshot.warehouses
        self.depot = snapshot.depot

    def _haversine_distance(
        self, p1: tuple[float, float], p2: tuple[float, float]
    ) -> int:
        """Calculate distance in meters."""
        R = 6371000
        lat1, lon1 = math.radians(p1[0]), math.radians(p1[1])
        lat2, lon2 = math.radians(p2[0]), math.radians(p2[1])

        dlat = lat2 - lat1
        dlon = lon2 - lon1

        a = (
            math.sin(dlat / 2) ** 2
            + math.cos(lat1) * math.cos(lat2) * math.sin(dlon / 2) ** 2
        )
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
        return int(R * c)

    def _travel_time_seconds(self, distance_m: int) -> int:
        """Calculate travel time assuming 50 km/h = 13.89 m/s."""
        DRONE_SPEED_MS = 13.89  # meters per second
        return int(distance_m / DRONE_SPEED_MS)

    def build(self) -> VRPProblem:
        """Build complete VRP problem with multi-warehouse support."""

        if not self.warehouses:
            raise ValueError("No warehouses available in snapshot")

        nodes = []

        # Node 0: Depot (unique)
        depot_node = 0
        nodes.append((self.depot.position.lat, self.depot.position.lon))

        # Nodes 1..N: Warehouses
        warehouse_nodes = []
        warehouse_node_to_id = {}
        for wh in self.warehouses:
            wh_idx = len(nodes)
            warehouse_nodes.append(wh_idx)
            warehouse_node_to_id[wh_idx] = wh.id
            nodes.append((wh.position.lat, wh.position.lon))

        # Nodes N+1..M: Delivery locations (hospitals)
        delivery_nodes = []
        delivery_node_to_order_id = {}
        pickups_deliveries = []
        # Default: no constraints for depot/warehouses (use large value)
        time_windows = [(0, 180 * 60)] * (len(warehouse_nodes) + 1)

        for order in self.orders:
            delivery_idx = len(nodes)
            delivery_nodes.append(delivery_idx)
            delivery_node_to_order_id[delivery_idx] = order.id
            nodes.append((order.delivery_location.lat, order.delivery_location.lon))

            # Time window for this delivery
            deadline_seconds = get_max_delivery_time_minutes(order.priority) * 60
            time_windows.append((0, deadline_seconds))

            # Find compatible warehouses for this order
            compatible_warehouse_nodes = [
                wh_idx
                for wh_idx, wh in zip(warehouse_nodes, self.warehouses, strict=False)
                if order.product_type in wh.authorized_product_types
            ]

            if not compatible_warehouse_nodes:
                logger.warning(
                    f"No compatible warehouse for order {order.id} product {order.product_type}"
                )
                continue

            # Add pickup-delivery pair with multiple pickup options
            pickups_deliveries.append((compatible_warehouse_nodes, delivery_idx))

        # Build distance matrix
        num_nodes = len(nodes)
        distance_matrix = [[0] * num_nodes for _ in range(num_nodes)]
        time_matrix = [[0] * num_nodes for _ in range(num_nodes)]

        for i in range(num_nodes):
            for j in range(num_nodes):
                if i != j:
                    dist = self._haversine_distance(nodes[i], nodes[j])
                    distance_matrix[i][j] = dist
                    time_matrix[i][j] = self._travel_time_seconds(dist)

        logger.info(
            f"VRP Problem built: {len(self.drones)} drones, "
            f"{len(self.orders)} orders, {len(self.warehouses)} warehouses"
        )

        return VRPProblem(
            distance_matrix=distance_matrix,
            time_matrix=time_matrix,
            depot_node=depot_node,
            warehouse_nodes=warehouse_nodes,
            delivery_nodes=delivery_nodes,
            node_locations=nodes,
            pickups_deliveries=pickups_deliveries,
            num_vehicles=len(self.drones),
            vehicle_capacities=[1] * len(self.drones),
            initial_battery_pct=[d.battery_percentage for d in self.drones],
            time_windows=time_windows,
            drone_ids=[d.id for d in self.drones],
            order_ids=[o.id for o in self.orders],
            warehouse_ids=[wh.id for wh in self.warehouses],
            delivery_node_to_order_id=delivery_node_to_order_id,
            warehouse_node_to_warehouse_id=warehouse_node_to_id,
        )
