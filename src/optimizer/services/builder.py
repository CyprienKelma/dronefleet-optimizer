import math
from typing import NamedTuple

import structlog

from src.optimizer.models.snapshot import (
    OptimizationSnapshot,
)

logger = structlog.get_logger(__name__)


class VRPProblem(NamedTuple):
    distance_matrix: list[list[int]]
    pickups_deliveries: list[tuple[int, int]]
    num_vehicles: int
    depot: int
    vehicle_capacities: list[int]
    node_locations: list[tuple[float, float]]
    drone_ids: list[str]
    order_ids: list[str]
    node_to_order_id: dict[int, str]


class VRPProblemBuilder:
    def __init__(self, snapshot: OptimizationSnapshot):
        self.snapshot = snapshot
        self.drones = snapshot.drones
        self.orders = snapshot.orders
        self.warehouses = snapshot.warehouses

    def _haversine_distance(
        self, p1: tuple[float, float], p2: tuple[float, float]
    ) -> int:
        """Calculate distance in meters between two points."""
        R = 6371000  # Radius of earth in meters
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

    def build(self) -> VRPProblem:
        # nodes: [warehouse_0, ..., pickup_0, delivery_0, pickup_1, delivery_1, ...]
        # For simplicity, we assume one warehouse for now (the first one)
        # OR-Tools VRP often uses a single depot for all vehicles.

        depot_node = 0
        nodes = []
        # Add warehouse (depot)
        if not self.warehouses:
            raise ValueError("Cannot build VRPProblem: no warehouses available in snapshot")
        nodes.append((self.warehouses[0].position.lat, self.warehouses[0].position.lon))

        pickups_deliveries = []
        node_to_order_id = {}
        order_ids = []

        for order in self.orders:
            pickup_idx = len(nodes)
            nodes.append((order.pickup_location.lat, order.pickup_location.lon))
            delivery_idx = len(nodes)
            nodes.append((order.delivery_location.lat, order.delivery_location.lon))

            pickups_deliveries.append((pickup_idx, delivery_idx))
            node_to_order_id[pickup_idx] = order.id
            node_to_order_id[delivery_idx] = order.id
            order_ids.append(order.id)

        # Build distance matrix
        num_nodes = len(nodes)
        distance_matrix = [[0] * num_nodes for _ in range(num_nodes)]
        for i in range(num_nodes):
            for j in range(num_nodes):
                if i == j:
                    distance_matrix[i][j] = 0
                else:
                    distance_matrix[i][j] = self._haversine_distance(nodes[i], nodes[j])

        logger.debug("Distance matrix rendered", matrix=distance_matrix)

        return VRPProblem(
            distance_matrix=distance_matrix,
            pickups_deliveries=pickups_deliveries,
            num_vehicles=len(self.drones),
            depot=depot_node,
            vehicle_capacities=[1] * len(self.drones),  # Each drone carries 1 order
            node_locations=nodes,
            drone_ids=[d.id for d in self.drones],
            order_ids=order_ids,
            node_to_order_id=node_to_order_id,
        )
