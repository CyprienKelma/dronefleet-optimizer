from dronefleet_shared.utils.global_config import settings
from google.api_core.exceptions import ServiceUnavailable
from google.cloud import pubsub_v1
from google.pubsub_v1.types.pubsub import Subscription, Topic

# Configuration par défaut (basée sur ton docker-compose)
PROJECT_ID = "drone-fleet-optimizer-local"


def list_resources():
    print(f"Connecting to emulator at {settings.pubsub_emulator_host}...")
    print(f"Project: {PROJECT_ID}")

    publisher = pubsub_v1.PublisherClient()
    subscriber = pubsub_v1.SubscriberClient()
    project_path = f"projects/{PROJECT_ID}"

    try:
        # Lister les Topics
        print("\n=== CURRENT TOPICS ===")
        topics = list[Topic](publisher.list_topics(request={"project": project_path}))
        if not topics:
            print("Aucun topic trouvé.")
        for topic in topics:
            print(f" - {topic.name}")

        # Lister les Subscriptions
        print("\n=== CURRENT SUBSCRIPTIONS ===")
        subscriptions = list[Subscription](
            subscriber.list_subscriptions(request={"project": project_path})
        )
        if not subscriptions:
            print("Aucune souscription trouvée.")
        for sub in subscriptions:
            print(f" - {sub.name} (Topic: {sub.topic})")

    except ServiceUnavailable:
        print("\nError: Could not connect to the emulator.")
        print("Ensure 'docker-compose up' is running and port 8085 is exposed.")
    except Exception as e:
        print(f"\nUnexpected error: {e}")


def list_last_messages():
    """Reads available messages from subscriptions (non-blocking)."""
    print("\n=== LATEST MESSAGES (PEEK) ===")

    subscriber = pubsub_v1.SubscriberClient()
    project_path = f"projects/{PROJECT_ID}"

    try:
        subscriptions = list[Subscription](
            subscriber.list_subscriptions(request={"project": project_path})
        )

        for sub in subscriptions:
            sub_name = sub.name.split("/")[-1]
            print(f"\n--- Subscription: {sub_name} ---")

            # The emulator (and Pub/Sub) doesn't support "peeking" without
            # acknowledgment easily. We use 'pull' with 'return_immediately=True' to
            # check for messages. WARNING: We use 'ack_ids' to acknowledge them
            # immediately in this debug tool so they don't clog the queue, but in
            # production, this would consume the message.
            try:
                response = subscriber.pull(
                    request={
                        "subscription": sub.name,
                        "max_messages": 5,
                        "return_immediately": True,  # Don't block if empty
                    }
                )

                if not response.received_messages:
                    print("  (No pending messages)")
                    continue

                ack_ids = []
                for received_message in response.received_messages:
                    msg = received_message.message
                    print(f"  [ID: {msg.message_id}] Data: {msg.data.decode('utf-8')}")
                    print(f"  Attributes: {dict[str, str](msg.attributes)}")
                    ack_ids.append(received_message.ack_id)

                # Acknowledge messages so they are removed from the subscription
                # (Remove this block if you want messages to reappear)
                # subscriber.acknowledge(request={
                # "subscription": sub.name, "ack_ids": ack_ids
                # })
                # print(f"-> Acknowledged {len(ack_ids)} messages (removed from queue)")

            except Exception as e:
                # Often occurs if no messages available and return_immediately is strict
                print(f"  Error pulling messages: {e}")

    except Exception as e:
        print(f"Error listing subscriptions for messages: {e}")


if __name__ == "__main__":
    list_resources()
    list_last_messages()
