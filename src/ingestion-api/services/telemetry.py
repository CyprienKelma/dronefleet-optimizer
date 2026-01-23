import logging
from ..messaging.factory import PublisherFactory
from shared.schemas.telemetry import DroneTelemetry

# Configure logging
logger = logging.getLogger(__name__)

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
        message_payload = telemetry.model_dump(mode='json')

        # publish to Event Bus
        try:
            success = self.publisher.publish(self.topic_name, message_payload)

            # TODO : Remove
            print(message_payload)

            if not success:
                logger.error(f"Publisher declined telemetry for drone {telemetry.drone_id}")
                return False

        except Exception as e:
            logger.error(f"Exception while publishing telemetry for {telemetry.drone_id}: {str(e)}")
            # For telemetry (high frequency), we might not want to crash the request,
            # but logging is essential.
            raise RuntimeError(f"Internal error publishing telemetry: {str(e)}")

        return True

    def shutdown(self):
        if self.publisher:
            self.publisher.close()
