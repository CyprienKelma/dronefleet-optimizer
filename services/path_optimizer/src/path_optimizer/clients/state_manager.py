import httpx
import structlog
from dronefleet_shared.models import OptimizationSnapshot
from dronefleet_shared.utils.global_config import settings

logger = structlog.get_logger(__name__)


class StateManagerClient:
    def __init__(self, base_url: str | None = None):
        self.base_url = base_url or settings.state_manager_url
        self.client = httpx.Client(timeout=30.0)

    def get_snapshot(self, session_id: str) -> OptimizationSnapshot:
        url = f"{self.base_url}/api/v1/optimizer/snapshot"
        logger.info(
            "Getting snapshot from State Manager", url=url, session_id=session_id
        )

        try:
            response = self.client.get(url, params={"sessionId": session_id})
            logger.info(
                "Received response from State Manager",
                status_code=response.status_code,
                elapsed=response.elapsed.total_seconds(),
            )
            response.raise_for_status()

            snapshot_data = response.json()
            logger.info("Parsing snapshot data", keys=list(snapshot_data.keys()))
            # Map enum names to values because betterproto from_dict is strict
            # For drones
            if "drones" in snapshot_data:
                from dronefleet_shared.models import DroneStatus

                for drone in snapshot_data["drones"]:
                    if isinstance(drone.get("status"), str):
                        try:
                            # Try to map string to int value
                            status_name = drone["status"]
                            if not status_name.startswith("DRONE_STATUS_"):
                                status_name = "DRONE_STATUS_" + status_name.upper()
                            drone["status"] = int(getattr(DroneStatus, status_name))
                        except (AttributeError, ValueError):
                            drone["status"] = 0  # UNSPECIFIED

            # For orders
            if "orders" in snapshot_data:
                from dronefleet_shared.models import OrderPriority

                for order in snapshot_data["orders"]:
                    if isinstance(order.get("priority"), str):
                        try:
                            priority_name = order["priority"]
                            if not priority_name.startswith("ORDER_PRIORITY_"):
                                priority_name = (
                                    "ORDER_PRIORITY_" + priority_name.upper()
                                )
                            order["priority"] = int(
                                getattr(OrderPriority, priority_name)
                            )
                        except (AttributeError, ValueError):
                            order["priority"] = 0

            # Use betterproto to parse
            snapshot = OptimizationSnapshot().from_dict(snapshot_data)
            logger.info(
                "Successfully parsed snapshot",
                drones=len(snapshot.drones),
                orders=len(snapshot.orders),
            )
            return snapshot
        except httpx.TimeoutException as e:
            logger.error("Timeout while calling State Manager", url=url, error=str(e))
            raise
        except Exception as e:
            logger.error("Error calling State Manager", url=url, error=str(e))
            raise
