import structlog
from dronefleet_shared.models import MissionAssignment, Position, Waypoint

from .builder import VRPProblem

logger = structlog.get_logger(__name__)


class SolutionExtractor:
    """Extract mission assignments from OR-Tools solution."""

    def extract(
        self, assignment, routing, manager, problem: VRPProblem
    ) -> list[MissionAssignment]:
        """Extract route for each vehicle."""

        assignments = []

        for vehicle_id in range(problem.num_vehicles):
            index = routing.Start(vehicle_id)
            route_nodes = []

            # Collect all nodes in route
            while not routing.IsEnd(index):
                node_index = manager.IndexToNode(index)
                route_nodes.append(node_index)
                index = assignment.Value(routing.NextVar(index))

            # Add final depot
            route_nodes.append(manager.IndexToNode(index))

            # Skip if drone didn't do anything (only depot -> depot)
            if len(route_nodes) <= 2:
                continue

            # Build waypoints and identify orders
            waypoints = []
            order_ids_in_mission = set()

            for i, node_idx in enumerate(route_nodes):
                position_lat, position_lon = problem.node_locations[node_idx]
                position = Position(lat=position_lat, lon=position_lon)

                if node_idx == problem.depot_node:
                    waypoint_type = "DEPOT_START" if i == 0 else "DEPOT_RETURN"
                    waypoints.append(
                        Waypoint(
                            type=waypoint_type,
                            position=position,
                            related_order_id=None,
                            related_warehouse_id=None,
                        )
                    )

                elif node_idx in problem.warehouse_nodes:
                    warehouse_id = problem.warehouse_node_to_warehouse_id[node_idx]
                    waypoints.append(
                        Waypoint(
                            type="WAREHOUSE_PICKUP",
                            position=position,
                            related_order_id=None,
                            related_warehouse_id=warehouse_id,
                        )
                    )

                elif node_idx in problem.delivery_nodes:
                    order_id = problem.delivery_node_to_order_id[node_idx]
                    order_ids_in_mission.add(order_id)
                    waypoints.append(
                        Waypoint(
                            type="HOSPITAL_DELIVERY",
                            position=position,
                            related_order_id=order_id,
                            related_warehouse_id=None,
                        )
                    )

            # Calculate estimated metrics
            total_distance_m = sum(
                problem.distance_matrix[route_nodes[i]][route_nodes[i + 1]]
                for i in range(len(route_nodes) - 1)
            )
            total_time_s = sum(
                problem.time_matrix[route_nodes[i]][route_nodes[i + 1]]
                for i in range(len(route_nodes) - 1)
            )

            # Battery consumption estimate (simplified)
            battery_consumed = (total_distance_m / 1000.0) * 2.5  # % per km

            assignments.append(
                MissionAssignment(
                    drone_id=problem.drone_ids[vehicle_id],
                    order_ids=list(order_ids_in_mission),
                    route=waypoints,
                    estimated_battery_consumption=battery_consumed,
                    estimated_duration_minutes=total_time_s / 60.0,
                )
            )

        logger.info(f"Extracted {len(assignments)} mission assignments")
        return assignments
