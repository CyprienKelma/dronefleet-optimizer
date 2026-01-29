import time
import uuid

import structlog
from google.cloud import firestore, pubsub_v1

logger = structlog.get_logger(__name__)

# Constants for local testing
PROJECT_ID = "drone-fleet-optimizer-local"
FIRESTORE_EMULATOR_HOST = "localhost:8080"
PUBSUB_EMULATOR_HOST = "localhost:8085"


def setup_test_data(db):
    """Creates dummy drones and orders for the optimizer to work with."""
    logger.info("Setting up test data in Firestore...")

    # Create a warehouse if it doesn't exist
    warehouse_ref = db.collection("warehouses").document("WH-LILLE-02")
    warehouse_ref.set(
        {
            "name": "Lille Central Warehouse",
            "position": {"lat": 50.6297, "lon": 3.0573},
            "authorizedProductTypes": ["STANDARD", "COLD"],
            "isColdStorageCapable": True,
        }
    )

    # Create an IDLE drone
    drone_id = f"TEST-DRONE-{uuid.uuid4().hex[:4].upper()}"
    db.collection("drones").document(drone_id).set(
        {
            "batteryPercentage": 90.0,
            "speedKmh": 40.0,
            "status": "IDLE",
            "position": {"lat": 50.6297, "lon": 3.0573},  # At warehouse
            "homeDepotId": "WH-LILLE-01",
            "lastUpdate": firestore.SERVER_TIMESTAMP,
        }
    )
    logger.info("Created test drone", drone_id=drone_id)

    # Create a PENDING order
    order_id = f"TEST-ORDER-{uuid.uuid4().hex[:4].upper()}"
    db.collection("orders").document(order_id).set(
        {
            "pickupLocation": {"lat": 50.6297, "lon": 3.0573},
            "deliveryLocation": {"lat": 50.6400, "lon": 3.0700},
            "status": "PENDING",
            "priority": "STANDARD",
            "createdAt": firestore.SERVER_TIMESTAMP,
        }
    )
    logger.info("Created test order", order_id=order_id)

    return drone_id, order_id


def check_results(db, subscriber, drone_id, order_id):
    """Checks if a mission was created and if a decision was published."""
    logger.info("Waiting for results (30s timeout)...")

    timeout = 30
    start_time = time.time()

    mission_found = False
    decision_found = False

    subscription_path = subscriber.subscription_path(PROJECT_ID, "decisions-sub")

    while time.time() - start_time < timeout:
        # 1. Check Firestore for mission
        missions = db.collection("missions").where("orderId", "==", order_id).stream()
        for mission in missions:
            logger.info(
                "SUCCESS: Mission found in Firestore!",
                mission_id=mission.id,
                data=mission.to_dict(),
            )
            mission_found = True
            break

        # 2. Check Pub/Sub for decision message
        try:
            response = subscriber.pull(
                request={
                    "subscription": subscription_path,
                    "max_messages": 1,
                    "return_immediately": True,
                }
            )
            for msg in response.received_messages:
                logger.info(
                    "SUCCESS: Decision message found in Pub/Sub!",
                    data=msg.message.data.decode(),
                )
                subscriber.acknowledge(
                    request={"subscription": subscription_path, "ack_ids": [msg.ack_id]}
                )
                decision_found = True
        except Exception as e:
            logger.debug("Error pulling from Pub/Sub", error=str(e))

        if mission_found and decision_found:
            logger.info("ALL TESTS PASSED!")
            return True

        time.sleep(2)

    if not mission_found:
        logger.error("FAILED: No mission created in Firestore for the order.")
    if not decision_found:
        logger.error("FAILED: No decision message published to Pub/Sub.")

    return False


def main():
    logger.info("Starting Optimizer Integration Test")

    # Clients
    db = firestore.Client(project=PROJECT_ID)
    subscriber = pubsub_v1.SubscriberClient()

    # 1. Setup
    drone_id, order_id = setup_test_data(db)

    print("\n" + "=" * 50)
    print("TEST DATA READY")
    print(f"Drone ID: {drone_id}")
    print(f"Order ID: {order_id}")
    print("=" * 50 + "\n")
    print("STEP 2: RUN THE OPTIMIZER")
    print("In another terminal, run:")
    print("uv run python -m src.optimizer.main")
    print("OR run the docker service:")
    print("docker compose run optimizer")
    print("\n" + "=" * 50 + "\n")

    # 2. Check
    check_results(db, subscriber, drone_id, order_id)


if __name__ == "__main__":
    main()
