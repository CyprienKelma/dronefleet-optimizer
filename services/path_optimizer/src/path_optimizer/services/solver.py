import time

import structlog
from dronefleet_shared.utils.global_config import settings
from ortools.constraint_solver import pywrapcp, routing_enums_pb2

from .builder import VRPProblem

logger = structlog.get_logger(__name__)


class VRPSolver:
    """Solve the Vehicle Routing Problem with pickup-delivery, battery,
    capacity, and time-window constraints using OR-Tools CP-SAT backed routing."""

    # Battery consumption rate in percentage per kilometer.
    # 2.5% / km is a reasonable estimate for a delivery drone.
    CONSUMPTION_PER_KM: float = 2.5

    # Minimum battery percentage a drone must retain when returning to
    # the depot.  Expressed in the same scaled integer units (x10) used
    # by the battery dimension.
    MIN_RETURN_BATTERY_UNITS: int = 200  # 20% * 10

    # Penalty applied when the solver must drop an infeasible order.
    # Must be high enough so orders are only dropped as a last resort.
    DROP_PENALTY: int = 100_000

    # Fixed cost incurred when a vehicle is used.  This encourages the
    # solver to balance load across multiple drones rather than
    # overloading a few.  Value represents equivalent meters of travel.
    VEHICLE_FIXED_COST: int = 5_000  # 5 km equivalent

    # Coefficient penalizing the difference between longest and shortest
    # routes.  Higher values encourage more balanced route lengths.
    GLOBAL_SPAN_COST_COEFFICIENT: int = 500

    def __init__(self, problem: VRPProblem):
        self.problem = problem

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def solve(self):
        """Solve VRP problem and return (assignment, routing, manager) or None."""

        time_limit = settings.solver_time_limit_seconds

        manager = pywrapcp.RoutingIndexManager(
            len(self.problem.distance_matrix),
            self.problem.num_vehicles,
            [self.problem.depot_node] * self.problem.num_vehicles,
            [self.problem.depot_node] * self.problem.num_vehicles,
        )
        routing = pywrapcp.RoutingModel(manager)

        # Register dimensions in order
        distance_dimension = self._add_distance_dimension(routing, manager)
        self._add_time_dimension(routing, manager)
        self._add_battery_dimension(routing, manager)
        self._add_capacity_dimension(routing, manager)
        self._add_pickup_delivery_constraints(routing, manager, distance_dimension)

        # Add fixed cost per vehicle to encourage fleet utilization
        for vehicle_id in range(self.problem.num_vehicles):
            routing.SetFixedCostOfVehicle(self.VEHICLE_FIXED_COST, vehicle_id)

        # Search strategy
        search_parameters = pywrapcp.DefaultRoutingSearchParameters()
        search_parameters.first_solution_strategy = (
            routing_enums_pb2.FirstSolutionStrategy.PARALLEL_CHEAPEST_INSERTION
        )
        search_parameters.local_search_metaheuristic = (
            routing_enums_pb2.LocalSearchMetaheuristic.GUIDED_LOCAL_SEARCH
        )
        search_parameters.time_limit.seconds = time_limit

        logger.info(
            "Solving VRP",
            vehicles=self.problem.num_vehicles,
            nodes=len(self.problem.distance_matrix),
            orders=len(self.problem.pickups_deliveries),
            time_limit_seconds=time_limit,
        )

        start_time = time.monotonic()
        assignment = routing.SolveWithParameters(search_parameters)
        elapsed = time.monotonic() - start_time
        minutes, seconds = divmod(elapsed, 60)

        if assignment:
            logger.info(
                f"Solution found in {int(minutes)} minutes {seconds:.1f} seconds",
                objective=assignment.ObjectiveValue(),
                status=routing.status(),
            )
            return assignment, routing, manager

        logger.warning(
            f"No solution found after {int(minutes)} minutes {seconds:.1f} seconds",
            status=routing.status(),
        )
        return None

    # ------------------------------------------------------------------
    # Dimension builders
    # ------------------------------------------------------------------

    def _add_distance_dimension(self, routing, manager):
        """Arc cost and cumulative distance tracking.

        The maximum per-route distance is set generously (100 km);
        the battery dimension is the real physical limiter.
        """

        def distance_callback(from_index, to_index):
            from_node = manager.IndexToNode(from_index)
            to_node = manager.IndexToNode(to_index)
            return self.problem.distance_matrix[from_node][to_node]

        cb_index = routing.RegisterTransitCallback(distance_callback)
        routing.SetArcCostEvaluatorOfAllVehicles(cb_index)

        routing.AddDimension(
            cb_index,
            0,  # no slack
            100_000,  # 100 km upper bound (generous; battery is binding)
            True,  # start cumul at zero
            "Distance",
        )
        dimension = routing.GetDimensionOrDie("Distance")
        dimension.SetGlobalSpanCostCoefficient(self.GLOBAL_SPAN_COST_COEFFICIENT)
        return dimension

    def _add_time_dimension(self, routing, manager):
        """Travel-time dimension with time windows on delivery nodes."""

        def time_callback(from_index, to_index):
            from_node = manager.IndexToNode(from_index)
            to_node = manager.IndexToNode(to_index)
            return self.problem.time_matrix[from_node][to_node]

        cb_index = routing.RegisterTransitCallback(time_callback)
        routing.AddDimension(
            cb_index,
            30 * 60,  # up to 30 min slack (waiting time)
            180 * 60,  # 3 h horizon
            False,  # start cumul is free (routes can begin at any time)
            "Time",
        )
        time_dim = routing.GetDimensionOrDie("Time")

        # Enforce time windows only on delivery nodes
        delivery_set = set(self.problem.delivery_nodes)
        for node_idx, (earliest, latest) in enumerate(self.problem.time_windows):
            if node_idx in delivery_set:
                index = manager.NodeToIndex(node_idx)
                time_dim.CumulVar(index).SetRange(earliest, latest)

        return time_dim

    def _add_battery_dimension(self, routing, manager):
        """Cumulative battery-consumption dimension.

        The cumul starts at 0 and increases with each arc.  For each
        vehicle the maximum allowed cumul at the end node equals
        (initial_battery - min_return_battery), ensuring the drone can
        safely return to the depot.
        """

        def battery_callback(from_index, to_index):
            from_node = manager.IndexToNode(from_index)
            to_node = manager.IndexToNode(to_index)
            distance_km = self.problem.distance_matrix[from_node][to_node] / 1000.0
            consumption_pct = distance_km * self.CONSUMPTION_PER_KM
            # Scaled by 10 for integer precision (1 unit = 0.1%)
            return int(consumption_pct * 10)

        cb_index = routing.RegisterTransitCallback(battery_callback)
        routing.AddDimension(
            cb_index,
            0,  # no slack
            1000,  # theoretical max: 100% * 10
            True,  # start cumul at zero (tracks consumed energy)
            "Battery",
        )
        battery_dim = routing.GetDimensionOrDie("Battery")

        for vehicle_id in range(self.problem.num_vehicles):
            end_index = routing.End(vehicle_id)
            initial_battery = int(self.problem.initial_battery_pct[vehicle_id] * 10)
            max_consumption = max(0, initial_battery - self.MIN_RETURN_BATTERY_UNITS)
            battery_dim.CumulVar(end_index).SetMax(max_consumption)

        return battery_dim

    def _add_capacity_dimension(self, routing, manager):
        """Enforce vehicle capacity constraints.

        Each drone can carry at most 1 package at a time.  Pickup nodes
        add +1 to the load, delivery nodes subtract -1.  The cumulative
        load must never exceed the vehicle capacity (1).
        """

        # Build demand vector: +1 at pickup, -1 at delivery, 0 elsewhere
        demands = [0] * len(self.problem.distance_matrix)
        for pickup_node, delivery_node in self.problem.pickups_deliveries:
            demands[pickup_node] = 1
            demands[delivery_node] = -1

        def demand_callback(from_index):
            node = manager.IndexToNode(from_index)
            return demands[node]

        cb_index = routing.RegisterUnaryTransitCallback(demand_callback)
        routing.AddDimensionWithVehicleCapacity(
            cb_index,
            0,  # no slack
            self.problem.vehicle_capacities,  # max capacity per vehicle
            True,  # start cumul at zero
            "Capacity",
        )

        return routing.GetDimensionOrDie("Capacity")

    def _add_pickup_delivery_constraints(self, routing, manager, distance_dim):
        """Register 1-to-1 pickup-delivery pairs with optional dropping.

        Each pair is wrapped in disjunctions with a high penalty so the
        solver can drop orders that are physically infeasible (e.g. when
        the round-trip distance exceeds battery capacity).
        """

        for pickup_node, delivery_node in self.problem.pickups_deliveries:
            pickup_index = manager.NodeToIndex(pickup_node)
            delivery_index = manager.NodeToIndex(delivery_node)

            routing.AddPickupAndDelivery(pickup_index, delivery_index)

            # Same vehicle constraint
            routing.solver().Add(
                routing.VehicleVar(pickup_index) == routing.VehicleVar(delivery_index)
            )
            # Pickup must precede delivery (monotonic distance cumul)
            routing.solver().Add(
                distance_dim.CumulVar(pickup_index)
                <= distance_dim.CumulVar(delivery_index)
            )

            # Allow dropping infeasible pairs.  When one node of a pair
            # is dropped, the same-vehicle constraint forces the other
            # to be dropped as well.
            routing.AddDisjunction([pickup_index], self.DROP_PENALTY)
            routing.AddDisjunction([delivery_index], self.DROP_PENALTY)
