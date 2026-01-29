import structlog
from dronefleet_shared.models.telemetry import GeoPoint

from ..models.decision import MissionAssignment
from .builder import VRPProblem

logger = structlog.get_logger(__name__)


class SolutionExtractor:
    def extract(
        self, assignment, routing, manager, problem: VRPProblem
    ) -> list[MissionAssignment]:
        assignments = []

        for vehicle_id in range(problem.num_vehicles):
            index = routing.Start(vehicle_id)
            route_indices = []
            while not routing.IsEnd(index):
                node_index = manager.IndexToNode(index)
                route_indices.append(node_index)
                index = assignment.Value(routing.NextVar(index))

            # Extract mission if vehicle visited any order nodes
            # Route indices: [depot, pickup_i, delivery_i, ..., end_depot]
            # We filter out the depot nodes to find the order assignments
            order_node_indices = [
                idx for idx in route_indices if idx in problem.node_to_order_id
            ]

            # Since OR-Tools ensures pickup comes before delivery for the same order,
            # and each vehicle carries 1 order (capacity constraint 1),
            # we can look for pairs in order_node_indices.
            # In our simple case, each vehicle will have at most one order.

            processed_orders = set()
            for idx in order_node_indices:
                order_id = problem.node_to_order_id[idx]
                if order_id in processed_orders:
                    continue

                # For this assignment, we only take the first order encountered
                # if multiple (though with capacity 1, there should be only one pair)

                # Build the route points: [pickup, delivery]
                # In a more advanced version, we'd include all intermediate points
                order_idx = problem.order_ids.index(order_id)
                pickup_node, delivery_node = problem.pickups_deliveries[order_idx]

                pickup_loc = problem.node_locations[pickup_node]
                delivery_loc = problem.node_locations[delivery_node]

                route = [
                    GeoPoint(lat=pickup_loc[0], lon=pickup_loc[1]),
                    GeoPoint(lat=delivery_loc[0], lon=delivery_loc[1]),
                ]

                assignments.append(
                    MissionAssignment(
                        drone_id=problem.drone_ids[vehicle_id],
                        order_id=order_id,
                        route=route,
                    )
                )
                processed_orders.add(order_id)

        return assignments
