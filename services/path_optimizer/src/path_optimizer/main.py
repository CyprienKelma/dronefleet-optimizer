import uuid

import structlog

from src.optimizer.clients.publisher import DecisionPublisher
from src.optimizer.clients.state_manager import StateManagerClient
from src.optimizer.services.builder import VRPProblemBuilder
from src.optimizer.services.extractor import SolutionExtractor
from src.optimizer.services.solver import VRPSolver
from src.shared.configs.logging_config import setup_logging

# Initialize logging
setup_logging()
logger = structlog.get_logger(__name__)


def run_optimization():
    session_id = str(uuid.uuid4())
    logger.info("Starting optimization cycle", session_id=session_id)

    try:
        # acquire snapshot of data
        sm_client = StateManagerClient()
        snapshot_data = sm_client.acquire_snapshot(session_id)

        if not snapshot_data.drones:
            logger.info("No available drones for optimization", session_id=session_id)
            return

        if not snapshot_data.orders:
            logger.info("No pending orders for optimization", session_id=session_id)
            return

        logger.info(
            "Acquired snapshot",
            drones_count=len(snapshot_data.drones),
            orders_count=len(snapshot_data.orders),
            warehouses_count=len(snapshot_data.warehouses),
        )

        # call OR builder
        builder = VRPProblemBuilder(snapshot_data)
        problem = builder.build()

        # solve problem with current data
        solver = VRPSolver(problem)
        solution_data = solver.solve()

        if solution_data is None:
            logger.warning("No optimization solution found", session_id=session_id)
            return

        assignment_result, routing, manager = solution_data

        # 4. Extract assignments
        extractor = SolutionExtractor()
        assignments = extractor.extract(assignment_result, routing, manager, problem)

        if not assignments:
            logger.info(
                "No mission assignments extracted from solution", session_id=session_id
            )
            return

        logger.info("Extracted mission assignments", count=len(assignments))

        # 5. Publish decisions
        publisher = DecisionPublisher()
        for assignment in assignments:
            publisher.publish_decision(assignment)

        logger.info(
            "Optimization cycle completed successfully",
            session_id=session_id,
            assignments_count=len(assignments),
        )

    except Exception as e:
        logger.error("Optimization cycle failed", error=str(e), session_id=session_id)


if __name__ == "__main__":
    run_optimization()
