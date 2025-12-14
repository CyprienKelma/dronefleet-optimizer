import os
from .base import MessagePublisher
from .kafka_publisher import KafkaPublisher
from .pubsub_publisher import PubSubPublisher

class PublisherFactory:
    """
    Factory class to instantiate the correct publisher based on configuration.
    """

    @staticmethod
    def get_publisher() -> MessagePublisher:
        env = os.getenv('APP_ENV', 'development')
        
        if env == 'production':
            project_id = os.getenv('GCP_PROJECT_ID')
            if not project_id:
                raise ValueError("GCP_PROJECT_ID must be set in production")
            return PubSubPublisher(project_id=project_id)
            
        elif env == 'local_kafka':
            bootstrap_servers = os.getenv('KAFKA_BOOTSTRAP_SERVERS', 'localhost:9092')
            return KafkaPublisher(bootstrap_servers=bootstrap_servers)
            
        else:
            # Default fallback or mock for testing
            print("Warning: Using default/mock publisher configuration.")
            # Could return a MockPublisher here for unit tests
            return KafkaPublisher()
