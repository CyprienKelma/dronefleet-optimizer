import structlog
from ortools.constraint_solver import pywrapcp, routing_enums_pb2
from src.optimizer.services.builder import VRPProblem
from src.shared.configs.global_config import settings

logger = structlog.get_logger(__name__)


class VRPSolver:
    def __init__(self, problem: VRPProblem):
        self.problem = problem

    def solve(self):
        # Create the routing index manager.
        manager = pywrapcp.RoutingIndexManager(
            len(self.problem.distance_matrix),
            self.problem.num_vehicles,
            self.problem.depot,
        )

        # Create Routing Model.
        routing = pywrapcp.RoutingModel(manager)

        # Create and register a transit callback.
        def distance_callback(from_index, to_index):
            """Returns the distance between the two nodes."""
            # Convert from routing variable Index to distance matrix NodeIndex.
            from_node = manager.IndexToNode(from_index)
            to_node = manager.IndexToNode(to_index)
            return self.problem.distance_matrix[from_node][to_node]

        transit_callback_index = routing.RegisterTransitCallback(distance_callback)

        # Define cost of each arc.
        routing.SetArcCostEvaluatorOfAllVehicles(transit_callback_index)

        # Add Distance constraint.
        dimension_name = "Distance"
        routing.AddDimension(
            transit_callback_index,
            0,  # no slack
            30000,  # vehicle maximum travel distance (30km)
            True,  # start cumul to zero
            dimension_name,
        )
        distance_dimension = routing.GetDimensionOrDie(dimension_name)
        distance_dimension.SetGlobalSpanCostCoefficient(100)

        # Define Transportation Requests (Pickups and Deliveries)
        for request in self.problem.pickups_deliveries:
            pickup_index = manager.NodeToIndex(request[0])
            delivery_index = manager.NodeToIndex(request[1])
            routing.AddPickupAndDelivery(pickup_index, delivery_index)
            routing.solver().Add(
                routing.VehicleVar(pickup_index) == routing.VehicleVar(delivery_index)
            )
            routing.solver().Add(
                distance_dimension.CumulVar(pickup_index)
                <= distance_dimension.CumulVar(delivery_index)
            )

        # Setting first solution heuristic.
        search_parameters = pywrapcp.DefaultRoutingSearchParameters()
        search_parameters.first_solution_strategy = (
            routing_enums_pb2.FirstSolutionStrategy.PARALLEL_CHEAPEST_INSERTION
        )
        search_parameters.time_limit.seconds = settings.solver_time_limit_seconds

        # Solve the problem.
        assignment = routing.SolveWithParameters(search_parameters)

        if assignment:
            return assignment, routing, manager
        else:
            logger.warning("No solution found for VRP")
            return None
