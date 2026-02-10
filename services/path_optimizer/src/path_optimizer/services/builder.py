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
    """Complete VRP problem definition for drone delivery routing.

    Node layout:
        - Node 0: Depot (start/end for all vehicles)
        - Nodes 1..N: Pickup nodes (one per order, located at nearest warehouse)
        - Nodes N+1..2N: Delivery nodes (one per order, at delivery location)

    Each order has its own unique pickup node to avoid shared-node conflicts
    in OR-Tools pickup-and-delivery constraints.
    """

    distance_matrix: list[list[int]]  # meters
    time_matrix: list[list[int]]  # seconds

    # Nodes structure
    depot_node: int  # Index 0
    pickup_nodes: list[int]  # Unique per order, at nearest warehouse location
    delivery_nodes: list[int]  # Unique per order, at order delivery location
    node_locations: list[tuple[float, float]]

    # 1-to-1 pickup-delivery pairs
    pickups_deliveries: list[tuple[int, int]]  # (pickup_node, delivery_node)

    # Constraints
    num_vehicles: int
    vehicle_capacities: list[int]
    initial_battery_pct: list[float]  # Starting battery percentage per drone
    time_windows: list[tuple[int, int]]  # (earliest, latest) in seconds

    # Metadata for solution extraction
    drone_ids: list[str]
    order_ids: list[str]
    warehouse_ids: list[str]
    delivery_node_to_order_id: dict[int, str]
    pickup_node_to_warehouse_id: dict[int, str]


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
        """Build VRP problem with unique pickup nodes per order.

        Instead of sharing warehouse nodes across orders (which causes
        OR-Tools pickup-delivery conflicts), each order gets its own
        pickup node located at the nearest compatible warehouse.
        """

        if not self.warehouses:
            raise ValueError("No warehouses available in snapshot")

        nodes: list[tuple[float, float]] = []

        # Node 0: Depot (start and end for all vehicles)
        depot_node = 0
        nodes.append((self.depot.position.lat, self.depot.position.lon))

        pickup_nodes: list[int] = []
        delivery_nodes: list[int] = []
        pickup_node_to_warehouse_id: dict[int, str] = {}
        delivery_node_to_order_id: dict[int, str] = {}
        pickups_deliveries: list[tuple[int, int]] = []
        order_ids: list[str] = []

        # Depot gets a permissive time window
        time_windows: list[tuple[int, int]] = [(0, 180 * 60)]

        for order in self.orders:
            # Identify warehouses that stock the required product type
            compatible_warehouses = [
                wh
                for wh in self.warehouses
                if order.product_type in wh.authorized_product_types
            ]

            if not compatible_warehouses:
                logger.warning(
                    "No compatible warehouse for order %s (product: %s), skipping",
                    order.id,
                    order.product_type,
                )
                continue

            # Select the warehouse closest to the delivery location to
            # minimize transit distance for each pickup-delivery leg.
            nearest_wh = min(
                compatible_warehouses,
                key=lambda wh: self._haversine_distance(
                    (wh.position.lat, wh.position.lon),
                    (order.delivery_location.lat, order.delivery_location.lon),
                ),
            )

            # Unique pickup node at the selected warehouse position
            pickup_idx = len(nodes)
            pickup_nodes.append(pickup_idx)
            pickup_node_to_warehouse_id[pickup_idx] = nearest_wh.id
            nodes.append((nearest_wh.position.lat, nearest_wh.position.lon))
            time_windows.append((0, 180 * 60))  # Flexible pickup window

            # Unique delivery node at the order destination
            delivery_idx = len(nodes)
            delivery_nodes.append(delivery_idx)
            delivery_node_to_order_id[delivery_idx] = order.id
            nodes.append(
                (order.delivery_location.lat, order.delivery_location.lon)
            )
            deadline_seconds = get_max_delivery_time_minutes(order.priority) * 60
            time_windows.append((0, deadline_seconds))

            # Register the 1-to-1 pickup-delivery pair
            pickups_deliveries.append((pickup_idx, delivery_idx))
            order_ids.append(order.id)

        # Build pairwise distance and travel-time matrices
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
            "VRP Problem built: %d drones, %d orders, %d warehouses, %d nodes",
            len(self.drones),
            len(order_ids),
            len(self.warehouses),
            num_nodes,
        )

        return VRPProblem(
            distance_matrix=distance_matrix,
            time_matrix=time_matrix,
            depot_node=depot_node,
            pickup_nodes=pickup_nodes,
            delivery_nodes=delivery_nodes,
            node_locations=nodes,
            pickups_deliveries=pickups_deliveries,
            num_vehicles=len(self.drones),
            vehicle_capacities=[1] * len(self.drones),
            initial_battery_pct=[d.battery_percentage for d in self.drones],
            time_windows=time_windows,
            drone_ids=[d.id for d in self.drones],
            order_ids=order_ids,
            warehouse_ids=[wh.id for wh in self.warehouses],
            delivery_node_to_order_id=delivery_node_to_order_id,
            pickup_node_to_warehouse_id=pickup_node_to_warehouse_id,
        )
