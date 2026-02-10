import structlog
from dronefleet_shared.models import (
    MissionAssignment,
    Position,
    Waypoint,
    WaypointType,
)

from .builder import VRPProblem

logger = structlog.get_logger(__name__)


class SolutionExtractor:
    """Extract mission assignments from an OR-Tools routing solution."""

    def extract(
        self, assignment, routing, manager, problem: VRPProblem
    ) -> list[MissionAssignment]:
        """Walk each vehicle route and build MissionAssignment objects."""

        # Pre-compute lookup sets for O(1) membership tests
        pickup_set = set(problem.pickup_nodes)
        delivery_set = set(problem.delivery_nodes)

        assignments: list[MissionAssignment] = []

        for vehicle_id in range(problem.num_vehicles):
            index = routing.Start(vehicle_id)
            route_nodes: list[int] = []

            while not routing.IsEnd(index):
                route_nodes.append(manager.IndexToNode(index))
                index = assignment.Value(routing.NextVar(index))

            # Append the terminal depot node
            route_nodes.append(manager.IndexToNode(index))

            # Skip idle drones (depot -> depot with no intermediate stops)
            if len(route_nodes) <= 2:
                continue

            waypoints: list[Waypoint] = []
            order_ids_in_mission: set[str] = set()

            for i, node_idx in enumerate(route_nodes):
                lat, lon = problem.node_locations[node_idx]
                position = Position(lat=lat, lon=lon)

                if node_idx == problem.depot_node:
                    wpt_type = (
                        WaypointType.WAYPOINT_TYPE_DEPOT_START
                        if i == 0
                        else WaypointType.WAYPOINT_TYPE_DEPOT_RETURN
                    )
                    waypoints.append(
                        Waypoint(
                            type=wpt_type,
                            position=position,
                            related_order_id="",
                            related_warehouse_id="",
                        )
                    )

                elif node_idx in pickup_set:
                    warehouse_id = problem.pickup_node_to_warehouse_id[node_idx]

                    # Consolidate consecutive pickups at the same warehouse
                    # to avoid redundant waypoints (e.g., 5 pickups at WH-EAST
                    # become a single WAREHOUSE_PICKUP waypoint)
                    if (
                        waypoints
                        and waypoints[-1].type
                        == WaypointType.WAYPOINT_TYPE_WAREHOUSE_PICKUP
                        and waypoints[-1].related_warehouse_id == warehouse_id
                    ):
                        # Skip adding duplicate pickup at same warehouse
                        continue

                    waypoints.append(
                        Waypoint(
                            type=WaypointType.WAYPOINT_TYPE_WAREHOUSE_PICKUP,
                            position=position,
                            related_order_id="",
                            related_warehouse_id=warehouse_id,
                        )
                    )

                elif node_idx in delivery_set:
                    order_id = problem.delivery_node_to_order_id[node_idx]
                    order_ids_in_mission.add(order_id)
                    waypoints.append(
                        Waypoint(
                            type=WaypointType.WAYPOINT_TYPE_HOSPITAL_DELIVERY,
                            position=position,
                            related_order_id=order_id,
                            related_warehouse_id="",
                        )
                    )

            # Aggregate route metrics
            total_distance_m = sum(
                problem.distance_matrix[route_nodes[i]][route_nodes[i + 1]]
                for i in range(len(route_nodes) - 1)
            )
            total_time_s = sum(
                problem.time_matrix[route_nodes[i]][route_nodes[i + 1]]
                for i in range(len(route_nodes) - 1)
            )
            battery_consumed_pct = (total_distance_m / 1000.0) * 2.5

            assignments.append(
                MissionAssignment(
                    drone_id=problem.drone_ids[vehicle_id],
                    order_ids=list(order_ids_in_mission),
                    route=waypoints,
                    estimated_battery_consumption=battery_consumed_pct,
                    estimated_duration_minutes=total_time_s / 60.0,
                )
            )

        logger.info("Extracted mission assignments", count=len(assignments))
        return assignments
