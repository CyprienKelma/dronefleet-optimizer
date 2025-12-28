import os
from .base_publisher import MessagePublisher
from .publisher.kafka_publisher import KafkaPublisher
from .publisher.pubsub_publisher import PubSubPublisher

class PublisherFactory:
    """
    Factory class to instantiate the correct publisher based on configuration.
    """

    @staticmethod
    def get_publisher() -> MessagePublisher:
        strategy = os.getenv('PROJECT_STRATEGY', 'on_cloud')
        env = os.getenv('ENVIRONMENT', 'dev')
        PROJECT_ID = os.getenv("PROJECT_ID", "drone-project-dev")

                
        if strategy == 'on_cloud':

            if env == 'dev':
                bootstrap_servers = os.getenv('KAFKA_BOOTSTRAP_SERVERS', 'localhost:9092')
                return PubSubPublisher(project_id=PROJECT_ID)

            elif env == 'prod':
                project_id = os.getenv('GCP_PROJECT_ID')
                if not project_id:
                    raise ValueError("GCP_PROJECT_ID must be set in production")
                return PubSubPublisher(project_id=PROJECT_ID)
                
            else:
                # Default fallback or mock for testing
                print("Warning: Using default/mock publisher configuration.")
                # Could return a MockPublisher here for unit tests
                return PubSubPublisher(project_id=PROJECT_ID)

        elif strategy == 'on_premise':
            # TODO Setup proper Factory once on_premise is implemented
            if env == 'prod':
                bootstrap_servers = os.getenv('KAFKA_BOOTSTRAP_SERVERS')
                if not bootstrap_servers:
                    raise ValueError("KAFKA_BOOTSTRAP_SERVERS must be set for on-premise production")
                return KafkaPublisher(bootstrap_servers=bootstrap_servers)

            elif env == 'dev':
                bootstrap_servers = os.getenv('KAFKA_BOOTSTRAP_SERVERS', 'localhost:9092')
                return KafkaPublisher(bootstrap_servers=bootstrap_servers)

            else:
                # Default fallback or mock for testing
                print("Warning: Using default/mock publisher configuration.")
                # Could return a MockPublisher here for unit tests
                return KafkaPublisher()
