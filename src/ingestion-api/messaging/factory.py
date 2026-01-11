from shared.configs.global_config import settings
from .base_publisher import MessagePublisher
from .publisher.kafka_publisher import KafkaPublisher
from .publisher.pubsub_publisher import PubSubPublisher

class PublisherFactory:
    """
    Factory class to instantiate the correct publisher based on configuration.
    """

    @staticmethod
    def get_publisher() -> MessagePublisher:
        
        if settings.deployment_strategy == 'on_cloud':
            return PubSubPublisher(project_id=settings.project_id)

        elif settings.deployment_strategy == 'on_premise':
            return KafkaPublisher(bootstrap_servers=settings.kafka_bootstrap_servers)

        else:
            raise ValueError(f"Unknown strategy: {settings.deployment_strategy}")