<<<<<<<< HEAD:src/ingestion/services/request.py
import structlog

from shared.schemas.request import DeliveryRequest
========
import logging
from ..messaging.factory import PublisherFactory
from shared.schemas.order import DeliveryOrder
>>>>>>>> 9b77deb (feat: integrate Firestore for order and mission management):src/ingestion/services/order.py

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
<<<<<<<< HEAD:src/ingestion/services/request.py
        self.topic_name = "requests"  # Topic name as defined in the architecture
========
        self.topic_name = "orders" # Topic name as defined in the architecture
>>>>>>>> 9b77deb (feat: integrate Firestore for order and mission management):src/ingestion/services/order.py

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
<<<<<<<< HEAD:src/ingestion/services/request.py
        logger.info(
            "Processing delivery request",
            request_id=request.request_id,
            priority=request.priority,
        )
========
        logger.info(f"Processing delivery order {order.order_id} [{order.priority}]")
>>>>>>>> 9b77deb (feat: integrate Firestore for order and mission management):src/ingestion/services/order.py

        # 1. Additional Business Validation (if any)
        # e.g., Check if pickup_location is within service area
        # For now, Pydantic schema validation is considered sufficient for ingestion.

        # 2. Serialize Payload
        # model_dump(mode='json') ensures Enums and Datetimes are serialized to strings
<<<<<<<< HEAD:src/ingestion/services/request.py
        message_payload = request.model_dump(mode="json")
========
        message_payload = order.model_dump(mode='json')
>>>>>>>> 9b77deb (feat: integrate Firestore for order and mission management):src/ingestion/services/order.py

        # publish to Event Bus
        try:
            success = self.publisher.publish(self.topic_name, message_payload)
            if not success:
<<<<<<<< HEAD:src/ingestion/services/request.py
                logger.error(
                    "Publisher returned False for order", request_id=request.request_id
                )
                raise RuntimeError("Failed to queue the order. Publisher declined.")

        except Exception as e:
            logger.error(
                "Exception while publishing order",
                request_id=request.request_id,
                error=str(e),
            )
            raise RuntimeError(f"Internal error publishing order: {str(e)}") from e

        logger.info(
            "Successfully queued order",
            request_id=request.request_id,
            topic=self.topic_name,
        )
        return request.request_id
========
                logger.error(f"Publisher returned False for order {order.order_id}")
                raise RuntimeError("Failed to queue the order. Publisher declined.")

        except Exception as e:
            logger.error(f"Exception while publishing order {order.order_id}: {str(e)}")
            raise RuntimeError(f"Internal error publishing order: {str(e)}")

        logger.info(f"Successfully queued order {order.order_id} to topic '{self.topic_name}'")
        return order.order_id
>>>>>>>> 9b77deb (feat: integrate Firestore for order and mission management):src/ingestion/services/order.py

    def shutdown(self):
        """Cleanup resources."""
        if self.publisher:
            self.publisher.close()
