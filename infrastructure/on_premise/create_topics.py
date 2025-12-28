from google.pubsub_v1.types.pubsub import Topic
import os
import time
from google.cloud import pubsub_v1
from google.api_core.exceptions import AlreadyExists, ServiceUnavailable, RetryError

# Configuration
PROJECT_ID = os.getenv("PROJECT_ID", "drone-project-dev")
TOPICS = [
    "requests",
    "telemetry",
    "decisions",
    "orders"
]

# Create subscriptions for debugging/monitoring
SUBSCRIPTIONS = {
    "requests": ["requests-sub"],
    "telemetry": ["telemetry-sub"],
    "decisions": ["decisions-sub"],
    "orders": ["orders-sub"]
}

def wait_for_emulator():
    print("Waiting for Pub/Sub emulator...")
    publisher = pubsub_v1.PublisherClient()
    project_path = f"projects/{PROJECT_ID}"
    
    retries = 0
    max_retries = 30
    
    while retries < max_retries:
        try:
            # Try to list topics to check connection
            list[Topic](publisher.list_topics(request={"project": project_path}))
            print("Pub/Sub emulator is ready!")
            return True
        except (ServiceUnavailable, RetryError, Exception) as e:
            print(f"Emulator not ready yet: {e}")
            time.sleep(1)
            retries += 1
            
    return False

def init_pubsub():
    if not wait_for_emulator():
        print("Could not connect to Pub/Sub emulator. Exiting.")
        exit(1)

    publisher = pubsub_v1.PublisherClient()
    subscriber = pubsub_v1.SubscriberClient()

    print(f"Initializing Pub/Sub resources for project: {PROJECT_ID}")

    # Create Topics
    for topic_id in TOPICS:
        topic_path = publisher.topic_path(PROJECT_ID, topic_id)
        try:
            publisher.create_topic(request={"name": topic_path})
            print(f"  -- Created topic: {topic_id}")
        except AlreadyExists:
            print(f"  -- Topic already exists: {topic_id}")
        except Exception as e:
            print(f"  -- Error creating topic {topic_id}: {e}")

        # Create Subscriptions
        if topic_id in SUBSCRIPTIONS:
            for sub_id in SUBSCRIPTIONS[topic_id]:
                sub_path = subscriber.subscription_path(PROJECT_ID, sub_id)
                try:
                    subscriber.create_subscription(
                        request={"name": sub_path, "topic": topic_path}
                    )
                    print(f"  -- Created subscription: {sub_id}")
                except AlreadyExists:
                    print(f"  -- Subscription already exists: {sub_id}")
                except Exception as e:
                    print(f"  -- Error creating subscription {sub_id}: {e}")

if __name__ == "__main__":
    init_pubsub()

