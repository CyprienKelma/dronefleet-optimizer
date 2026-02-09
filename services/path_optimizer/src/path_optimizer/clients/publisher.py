import structlog
from dronefleet_messaging.factory import PublisherFactory
from dronefleet_shared.models import MissionAssignment
from dronefleet_shared.utils.global_config import settings

logger = structlog.get_logger(__name__)


class DecisionPublisher:
    def __init__(self):
        self.publisher = PublisherFactory.get_publisher()
        self.topic = settings.pubsub_topic_decisions

    def publish_decision(self, assignment: MissionAssignment):
        logger.info(
            "Publishing mission assignment",
            drone_id=assignment.drone_id,
            order_ids=assignment.order_ids,
        )

        # Pass a dictionary, not JSON string, because the publisher will do json.dumps()
        message_dict = assignment.to_dict()

        self.publisher.publish(self.topic, message_dict)
