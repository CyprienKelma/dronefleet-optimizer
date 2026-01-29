import structlog

from dronefleet_shared.models.telemetry import DroneTelemetry

from ..messaging.factory import PublisherFactory

# Configure logging
logger = structlog.get_logger(__name__)


class TelemetryService:
    """
    Service responsible for handling drone telemetry, validating it,
    and publishing it to the message broker.
    """

    def __init__(self):
        self.publisher = PublisherFactory.get_publisher()
        self.topic_name = "telemetry"

    def process_telemetry(self, telemetry: DroneTelemetry) -> bool:
        """
        Validates and publishes a telemetry update.

        Args:
            telemetry (DroneTelemetry): The validated Pydantic model.

        Returns:
            bool: True if published successfully.

        Raises:
            RuntimeError: If publishing fails.
        """
        # Serialize Payload
        message_payload = telemetry.model_dump(mode="json")

        # publish to Event Bus
        try:
            success = self.publisher.publish(self.topic_name, message_payload)

            if not success:
                logger.error(
                    "Publisher declined telemetry", drone_id=telemetry.drone_id
                )
                return False

        except Exception as e:
            logger.error(
                "Exception while publishing telemetry",
                drone_id=telemetry.drone_id,
                error=str(e),
            )
            # For telemetry (high frequency), we might not want to crash the request,
            # but logging is essential.
            raise RuntimeError(f"Internal error publishing telemetry: {str(e)}") from e

        return True

    def shutdown(self):
        if self.publisher:
            self.publisher.close()
