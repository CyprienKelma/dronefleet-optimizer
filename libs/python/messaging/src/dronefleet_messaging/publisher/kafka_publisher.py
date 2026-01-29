import json
from typing import Any

import structlog

# TODO : install 'kafka-python' or 'confluent-kafka'
from kafka import KafkaProducer

from ..base_publisher import MessagePublisher

logger = structlog.get_logger(__name__)


class KafkaPublisher(MessagePublisher):
    """
    Concrete implementation of MessagePublisher for Apache Kafka.
    """

    def __init__(self, bootstrap_servers: str = "localhost:9092"):
        self.producer = KafkaProducer(
            bootstrap_servers=bootstrap_servers,
            value_serializer=lambda m: json.dumps(m).encode("ascii"),
        )

    def publish(self, topic: str, message: dict[str, Any], **kwargs) -> bool:
        try:
            # Kafka publish is asynchronous by default.
            # We assume success if the send method returns a future.
            # For strict reliability, one might wait for the future.
            future = self.producer.send(topic, message)
            # block for result (optional, depends on use case)
            future.get(timeout=10)
            return True
        except Exception as e:
            # In a real app, log this error securely
            logger.error("Error publishing to Kafka", topic=topic, error=str(e))
            return False

    def close(self):
        if self.producer:
            self.producer.flush()
            self.producer.close()
