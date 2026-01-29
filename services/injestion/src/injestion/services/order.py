import structlog

from myorg_shared.models.order import DeliveryOrder

from ..messaging.factory import PublisherFactory

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

    def process_order(self, order: DeliveryOrder) -> str:
        """
        Validates and publishes a delivery order.

        Args:
            order (DeliveryOrder): The validated Pydantic model of the order.

        Returns:
            str: The order_id if successful.

        Raises:
            RuntimeError: If publishing to the message broker fails.
        """

        logger.info(
            "Processing delivery request",
            request_id=order.order_id,
            priority=order.priority,
        )

        # 1. Additional Business Validation (if any)
        # e.g., Check if pickup_location is within service area
        # For now, Pydantic schema validation is considered sufficient for ingestion.

        # 2. Serialize Payload
        # model_dump(mode='json') ensures Enums and Datetimes are serialized to strings
        message_payload = order.model_dump(mode="json")

        # publish to Event Bus
        try:
            success = self.publisher.publish(self.topic_name, message_payload)
            if not success:
                logger.error(
                    "Publisher returned False for order", order_id=order.order_id
                )
                raise RuntimeError("Failed to queue the order. Publisher declined.")

        except Exception as e:
            logger.error(
                "Exception while publishing order",
                order_id=order.order_id,
                error=str(e),
            )
            raise RuntimeError(f"Internal error publishing order: {str(e)}") from e

        logger.info(
            "Successfully queued order",
            order_id=order.order_id,
            topic=self.topic_name,
        )
        return order.order_id

    def shutdown(self):
        """Cleanup resources."""
        if self.publisher:
            self.publisher.close()
