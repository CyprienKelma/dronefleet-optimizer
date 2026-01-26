import structlog

from shared.configs.global_config import settings

from .base_publisher import MessagePublisher
from .publisher.kafka_publisher import KafkaPublisher
from .publisher.pubsub_publisher import PubSubPublisher

logger = structlog.get_logger(__name__)


class PublisherFactory:
    """
    Factory class to instantiate the correct publisher based on configuration.
    """

    @staticmethod
    def get_publisher() -> MessagePublisher:
        if settings.deployment_strategy == "on_cloud":
            if not settings.project_id:
                raise ValueError(
                    f"PROJECT_ID must be set for cloud deployment "
                    f"(env={settings.environment})"
                )

            # emulator pub/sub for local
            if settings.environment == "local" and settings.pubsub_emulator_host:
                logger.info(
                    "Using Pub/Sub Emulator", host=settings.pubsub_emulator_host
                )
            else:
                logger.info("Using real GCP Pub/Sub", project_id=settings.project_id)
            return PubSubPublisher(project_id=settings.project_id)

        elif settings.deployment_strategy == "on_premise":
            if not settings.kafka_bootstrap_servers:
                raise ValueError("KAFKA_BOOTSTRAP_SERVERS must be set for on-premise")
            return KafkaPublisher(bootstrap_servers=settings.kafka_bootstrap_servers)

        else:
            raise ValueError(
                f"Unknown deployment strategy: {settings.deployment_strategy}"
            )
