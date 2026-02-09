import structlog
from dronefleet_messaging.factory import PublisherFactory
from dronefleet_shared.models import Order

# Configure logging
logger = structlog.get_logger(__name__)


class OrderService:
    """
    Service responsible for handling delivery orders, validating them,
    and publishing them to the message broker.
    """

    def __init__(self):
        # Initialize the publisher via the factory pattern
        # This decouples the service from the specific broker (Kafka vs PubSub)
        self.publisher = PublisherFactory.get_publisher()

        self.topic_name = "orders"  # Topic name as defined in the architecture

    def process_order(self, order: Order) -> str:
        """
        Validates and publishes a delivery order.

        Args:
            order (Order): The validated Pydantic model of the order.

        Returns:
            str: The order_id if successful.

        Raises:
            RuntimeError: If publishing to the message broker fails.
        """

        logger.info(
            "Processing delivery request",
            request_id=order.id,
            priority=order.priority,
        )

        # 1. Additional Business Validation (if any)
        # e.g., Check if pickup_location is within service area
        # For now, Pydantic schema validation is considered sufficient for ingestion.

        # 2. Serialize Payload
        # to_dict() ensures Enums and Datetimes are serialized correctly for JSON
        message_payload = order.to_dict()

        # publish to Event Bus
        try:
            success = self.publisher.publish(self.topic_name, message_payload)
            if not success:
                logger.error("Publisher returned False for order", order_id=order.id)
                raise RuntimeError("Failed to queue the order. Publisher declined.")

        except Exception as e:
            logger.error(
                "Exception while publishing order",
                order_id=order.id,
                error=str(e),
            )
            raise RuntimeError(f"Internal error publishing order: {str(e)}") from e

        logger.info(
            "Successfully queued order",
            order_id=order.id,
            topic=self.topic_name,
        )
        return order.id

    def shutdown(self):
        """Cleanup resources."""
        if self.publisher:
            self.publisher.close()
