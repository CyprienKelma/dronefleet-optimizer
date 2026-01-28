import httpx
import structlog

from src.optimizer.models.snapshot import OptimizationSnapshot
from src.shared.configs.global_config import settings

logger = structlog.get_logger(__name__)


class StateManagerClient:
    def __init__(self, base_url: str = None):
        self.base_url = base_url or settings.state_manager_url
        self.client = httpx.Client(timeout=30.0)

    def acquire_snapshot(self, session_id: str) -> OptimizationSnapshot:
        url = f"{self.base_url}/api/v1/optimizer/snapshot"
        logger.info(
            "Acquiring snapshot from State Manager", url=url, session_id=session_id
        )

        response = self.client.get(url, params={"sessionId": session_id})
        response.raise_for_status()

        snapshot_data = response.json()
        return OptimizationSnapshot.model_validate(snapshot_data)
