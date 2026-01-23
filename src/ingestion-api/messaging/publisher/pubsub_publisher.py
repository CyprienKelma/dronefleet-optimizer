import json
from typing import Any, Dict
from ..base_publisher import MessagePublisher

# Note: Requires 'google-cloud-pubsub' installed
try:
    from google.cloud import pubsub_v1
except ImportError:
    pubsub_v1 = None

class PubSubPublisher(MessagePublisher):
    """
    Concrete implementation of MessagePublisher for Google Cloud Pub/Sub.
    """

    def __init__(self, project_id: str):
        if pubsub_v1 is None:
            raise ImportError("google-cloud-pubsub library is not installed.")

        self.publisher = pubsub_v1.PublisherClient()
        self.project_id = project_id

    def publish(self, topic: str, message: Dict[str, Any], **kwargs) -> bool:
        """
        Publishes to a Pub/Sub topic.
        Expects topic to be the topic ID (not full path), unless fully qualified.
        """
        try:
            # Construct full topic path
            topic_path = self.publisher.topic_path(self.project_id, topic)

            data_str = json.dumps(message)
            data = data_str.encode("utf-8")

            # Pub/Sub specific args (like attributes or ordering keys)
            future = self.publisher.publish(topic_path, data, **kwargs)

            # Block to ensure message ID is returned (confirms publish)
            message_id = future.result()
            print(f"Message published to Pub/Sub on topic {topic}: {message_id}")
            return True
        except Exception as e:
            print(f"Error publishing to Pub/Sub on topic {topic}: {e}")
            return False

    def close(self):
        # Pub/Sub client handles connections automatically,
        # but explicit cleanup can be done if needed.
        pass
