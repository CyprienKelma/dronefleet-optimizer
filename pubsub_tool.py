import os

from google.api_core.exceptions import AlreadyExists, NotFound
from google.cloud import pubsub_v1

# CONFIGURATION
# On force l'utilisation de l'émulateur pour ce script
os.environ["PUBSUB_EMULATOR_HOST"] = "localhost:8085"
PROJECT_ID = "drone-project-dev"


def get_publisher():
    return pubsub_v1.PublisherClient()


def get_subscriber():
    return pubsub_v1.SubscriberClient()


def list_topics():
    publisher = get_publisher()
    project_path = f"projects/{PROJECT_ID}"
    print(f"\n📂 Topics dans '{PROJECT_ID}':")
    try:
        for topic in publisher.list_topics(request={"project": project_path}):
            print(f" - {topic.name}")
    except Exception as e:
        print(f"Erreur: {e}")


def create_topic(topic_id):
    publisher = get_publisher()
    topic_path = publisher.topic_path(PROJECT_ID, topic_id)
    try:
        publisher.create_topic(request={"name": topic_path})
        print(f"✅ Topic créé : {topic_path}")
    except AlreadyExists:
        print(f"⚠️  Le topic existe déjà : {topic_path}")


def create_subscription(topic_id, sub_id):
    subscriber = get_subscriber()
    topic_path = pubsub_v1.PublisherClient().topic_path(PROJECT_ID, topic_id)
    sub_path = subscriber.subscription_path(PROJECT_ID, sub_id)
    try:
        subscriber.create_subscription(request={"name": sub_path, "topic": topic_path})
        print(f"✅ Subscription créée : {sub_path}")
    except AlreadyExists:
        print(f"⚠️  La subscription existe déjà : {sub_path}")
    except NotFound:
        print(f"❌ Le topic '{topic_id}' n'existe pas. Créez-le d'abord.")


def publish_message(topic_id, message):
    publisher = get_publisher()
    topic_path = publisher.topic_path(PROJECT_ID, topic_id)
    data = message.encode("utf-8")
    try:
        future = publisher.publish(topic_path, data)
        print(f"📤 Message envoyé ! ID: {future.result()}")
    except NotFound:
        print(f"❌ Topic introuvable : {topic_id}")


def read_messages(sub_id):
    subscriber = get_subscriber()
    sub_path = subscriber.subscription_path(PROJECT_ID, sub_id)
    print(f"\n📥 Lecture de '{sub_id}'...")
    try:
        response = subscriber.pull(
            request={"subscription": sub_path, "max_messages": 5}, timeout=5.0
        )

        if not response.received_messages:
            print("📭 Aucun message en attente.")
            return

        ack_ids = []
        for msg in response.received_messages:
            print(f" - Message ID: {msg.message.message_id}")
            print(f"   Data: {msg.message.data.decode('utf-8')}")
            ack_ids.append(msg.ack_id)

        # Acknowledge (valider la lecture)
        if ack_ids:
            subscriber.acknowledge(
                request={"subscription": sub_path, "ack_ids": ack_ids}
            )
            print(f"✅ {len(ack_ids)} messages acquittés (supprimés de la file).")

    except Exception as e:
        print(f"Erreur lors de la lecture (timeout ou autre): {e}")


def main():
    while True:
        print("\n--- PUBSUB EMULATOR TOOL ---")
        print("1. Lister les Topics")
        print("2. Créer un Topic")
        print("3. Créer une Subscription")
        print("4. Publier un message")
        print("5. Lire les messages (Pull)")
        print("0. Quitter")

        choice = input("\nVotre choix : ")

        if choice == "1":
            list_topics()
        elif choice == "2":
            t = input("Nom du topic (ex: orders): ")
            create_topic(t)
        elif choice == "3":
            t = input("Topic cible (ex: orders): ")
            s = input("Nom de la sub (ex: orders-sub): ")
            create_subscription(t, s)
        elif choice == "4":
            t = input("Topic cible: ")
            m = input("Message: ")
            publish_message(t, m)
        elif choice == "5":
            s = input("Nom de la sub à lire: ")
            read_messages(s)
        elif choice == "0":
            break
        else:
            print("Choix invalide.")


if __name__ == "__main__":
    main()
