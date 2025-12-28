from email import message
import logging
from ..messaging.factory import PublisherFactory
from shared.schemas.request import DeliveryRequest

# Configure logging
logger = logging.getLogger(__name__)

class RequestService:
    """
    Service responsible for handling delivery requests, validating them,
    and publishing them to the message broker.
    """
    
    def __init__(self):
        # Initialize the publisher via the factory pattern
        # This decouples the service from the specific broker (Kafka vs PubSub)
        self.publisher = PublisherFactory.get_publisher()
        self.topic_name = "requests" # Topic name as defined in the architecture

    def process_order(self, request: DeliveryRequest) -> str:
        """
        Validates and publishes a delivery request.
        
        Args:
            request (DeliveryRequest): The validated Pydantic model of the request.
            
        Returns:
            str: The request_id if successful.
            
        Raises:
            RuntimeError: If publishing to the message broker fails.
        """
        logger.info(f"Processing delivery request {request.request_id} [{request.priority}]")

        # 1. Additional Business Validation (if any)
        # e.g., Check if pickup_location is within service area
        # For now, Pydantic schema validation is considered sufficient for ingestion.

        # 2. Serialize Payload
        # model_dump(mode='json') ensures Enums and Datetimes are serialized to strings
        message_payload = request.model_dump(mode='json')

        # TODO : remove
        print(message_payload)

        # publish to Event Bus
        try:
            success = self.publisher.publish(self.topic_name, message_payload)
            if not success:
                logger.error(f"Publisher returned False for order {request.request_id}")
                raise RuntimeError("Failed to queue the order. Publisher declined.")
                
        except Exception as e:
            logger.error(f"Exception while publishing order {request.request_id}: {str(e)}")
            raise RuntimeError(f"Internal error publishing order: {str(e)}")

        logger.info(f"Successfully queued order {request.request_id} to topic '{self.topic_name}'")
        return request.request_id

    def shutdown(self):
        """Cleanup resources."""
        if self.publisher:
            self.publisher.close()

