import structlog

from src.ingestion.messaging.factory import PublisherFactory
from src.optimizer.models.decision import MissionAssignment
from src.shared.configs.global_config import settings

logger = structlog.get_logger(__name__)


class DecisionPublisher:
    def __init__(self):
        self.publisher = PublisherFactory.get_publisher()
        self.topic = settings.pubsub_topic_decisions

    def publish_decision(self, assignment: MissionAssignment):
        logger.info(
            "Publishing mission assignment",
            drone_id=assignment.drone_id,
            order_id=assignment.order_id,
        )

        # Pass a dictionary, not JSON string, because the publisher will do json.dumps()
        message_dict = assignment.model_dump(by_alias=True)

        self.publisher.publish(self.topic, message_dict)
