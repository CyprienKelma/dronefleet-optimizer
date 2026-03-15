import asyncio

import structlog
from dronefleet_messaging.base_publisher import MessagePublisher
from dronefleet_messaging.factory import PublisherFactory

logger = structlog.get_logger(__name__)


class HealthService:
    def __init__(self):
        self.publisher: MessagePublisher | None = None
        self.is_ready: bool = False

    async def start(self) -> None:
        asyncio.create_task(self._init_with_retry())

    async def _init_with_retry(self) -> None:
        delay = 1.0
        max_delay = 30.0

        while not self.is_ready:
            try:
                self.publisher = PublisherFactory.get_publisher()
                self.is_ready = True
                logger.info("Publisher initialized successfully")
            except Exception as e:
                logger.warning(
                    "Publisher initialization failed, retrying",
                    error=str(e),
                    next_delay=delay,
                )
                await asyncio.sleep(delay)
                delay = min(delay * 2, max_delay)

    def check_readiness(self) -> dict:
        if self.is_ready and self.publisher:
            return {"status": "ready", "publisher": "ok"}
        return {"status": "not_ready", "publisher": "not_initialized"}

    def shutdown(self) -> None:
        if self.publisher:
            self.publisher.close()
