import os
import random
import uuid

from google.cloud import firestore

# Configuration for the emulator
os.environ["FIRESTORE_EMULATOR_HOST"] = "localhost:8080"

# Compact zone around Paris center (48.8566, 2.3522).
# All points within ~5 km so depot->warehouse->delivery stays feasible
# for battery (2.5%/km) and time windows (15-30 min at 50 km/h).
DEPOT_LAT = 48.8566
DEPOT_LON = 2.3522


def clear_firestore():
    """Delete all documents from known collections."""
    db = firestore.Client(project="drone-fleet-optimizer-local")
    print("Clearing Firestore data...")
    collections = ["depots", "warehouses", "drones", "orders", "missions"]
    for coll_name in collections:
        docs = list(db.collection(coll_name).list_documents())
        batch = db.batch()
        for doc in docs:
            batch.delete(doc)
        if docs:
            batch.commit()
        print(f"- Deleted {len(docs)} documents from {coll_name}")
    print("Firestore cleared")


def seed_small():
    """Seed a small dataset: 5 drones, 9 orders, 2 warehouses.
    Useful for quick smoke tests and debugging."""
    db = firestore.Client(project="drone-fleet-optimizer-local")
    print("Seeding SMALL dataset (5 drones, 9 orders)...")

    _seed_depot(db)
    _seed_warehouses(db, count=2)
    _seed_drones(db, count=5)
    _seed_orders(db, count=9)

    print("Seeding done (small)")


def seed_medium():
    """Seed a medium dataset: 15 drones, 50 orders, 4 warehouses.
    Useful for testing solver performance and route quality."""
    db = firestore.Client(project="drone-fleet-optimizer-local")
    print("Seeding MEDIUM dataset (15 drones, 50 orders)...")

    _seed_depot(db)
    _seed_warehouses(db, count=4)
    _seed_drones(db, count=15)
    _seed_orders(db, count=50)

    print("Seeding done (medium)")


def seed_large():
    """Seed a large dataset: 30 drones, 150 orders, 6 warehouses.
    Useful for stress-testing the solver at scale."""
    db = firestore.Client(project="drone-fleet-optimizer-local")
    print("Seeding LARGE dataset (30 drones, 150 orders)...")

    _seed_depot(db)
    _seed_warehouses(db, count=6)
    _seed_drones(db, count=30)
    _seed_orders(db, count=150)

    print("Seeding done (large)")


# ---------------------------------------------------------------
# Internal helpers
# ---------------------------------------------------------------

# Warehouses positioned around the depot in a ring (~1-2 km away)
_WAREHOUSE_POOL = [
    {"id": "WH-NORTH", "name": "North Logistics Center", "lat": 48.870, "lon": 2.352},
    {"id": "WH-SOUTH", "name": "South Distribution Hub", "lat": 48.843, "lon": 2.352},
    {"id": "WH-EAST", "name": "East Medical Depot", "lat": 48.857, "lon": 2.370},
    {"id": "WH-WEST", "name": "West Supply Center", "lat": 48.857, "lon": 2.334},
    {"id": "WH-NORTHEAST", "name": "Northeast Pharma Hub", "lat": 48.868, "lon": 2.368},
    {
        "id": "WH-SOUTHWEST",
        "name": "Southwest Emergency Depot",
        "lat": 48.845,
        "lon": 2.336,
    },
]

_PRIORITIES = [
    "ORDER_PRIORITY_STANDARD",
    "ORDER_PRIORITY_STANDARD",
    "ORDER_PRIORITY_STANDARD",
    "ORDER_PRIORITY_HIGH",
    "ORDER_PRIORITY_HIGH",
    "ORDER_PRIORITY_CRITICAL",
]
_PRODUCT_TYPES = [
    "PRODUCT_TYPE_BLOOD",
    "PRODUCT_TYPE_MEDICINE",
    "PRODUCT_TYPE_VACCINE",
    "PRODUCT_TYPE_ORGAN",
    "PRODUCT_TYPE_MEDICAL_DEVICE",
]


def _seed_depot(db):
    db.collection("depots").document("DEPOT-PARIS-01").set(
        {
            "name": "Paris Main Hub",
            "position": {"lat": DEPOT_LAT, "lon": DEPOT_LON},
            "capacity": 50,
            "chargingSlots": 10,
        }
    )
    print("- Depot seeded")


def _seed_warehouses(db, count: int):
    batch = db.batch()
    for wh in _WAREHOUSE_POOL[:count]:
        ref = db.collection("warehouses").document(wh["id"])
        batch.set(
            ref,
            {
                "name": wh["name"],
                "position": {"lat": wh["lat"], "lon": wh["lon"]},
                "authorizedProductTypes": [
                    "PRODUCT_TYPE_BLOOD",
                    "PRODUCT_TYPE_MEDICINE",
                    "PRODUCT_TYPE_VACCINE",
                    "PRODUCT_TYPE_ORGAN",
                    "PRODUCT_TYPE_MEDICAL_DEVICE",
                ],
                "isColdStorageCapable": True,
            },
        )
    batch.commit()
    print(f"- {count} warehouses seeded")


def _seed_drones(db, count: int):
    batch = db.batch()
    for i in range(1, count + 1):
        drone_id = f"DRONE-{i:03d}"
        # Battery between 60% and 100%, slight position jitter around depot
        battery = round(random.uniform(60.0, 100.0), 1)
        ref = db.collection("drones").document(drone_id)
        batch.set(
            ref,
            {
                "batteryPercentage": battery,
                "speedKmh": 0.0,
                "status": "DRONE_STATUS_IDLE",
                "homeDepotId": "DEPOT-PARIS-01",
                "position": {
                    "lat": DEPOT_LAT + random.uniform(-0.001, 0.001),
                    "lon": DEPOT_LON + random.uniform(-0.001, 0.001),
                },
                "consumptionPerKm": 0.1,
                "maxFlightTimeMinutes": 30,
                "currentMissionId": "",
                "solvingSessionId": "",
            },
        )

        # Firestore batches are limited to 500 operations
        if i % 400 == 0:
            batch.commit()
            batch = db.batch()

    batch.commit()
    print(f"- {count} drones seeded")


def _seed_orders(db, count: int):
    warehouses = _WAREHOUSE_POOL[: max(2, count // 10)]
    batch = db.batch()

    for i in range(count):
        order_id = str(uuid.uuid4())
        # Pickup from a random warehouse
        wh = random.choice(warehouses)
        # Delivery within ~3-5 km of depot
        target_lat = DEPOT_LAT + random.uniform(-0.04, 0.04)
        target_lon = DEPOT_LON + random.uniform(-0.04, 0.04)

        ref = db.collection("orders").document(order_id)
        batch.set(
            ref,
            {
                "id": order_id,
                "status": "ORDER_STATUS_PENDING",
                "priority": random.choice(_PRIORITIES),
                "productType": random.choice(_PRODUCT_TYPES),
                "pickupLocation": {"lat": wh["lat"], "lon": wh["lon"]},
                "deliveryLocation": {
                    "lat": round(target_lat, 6),
                    "lon": round(target_lon, 6),
                },
                "createdAt": firestore.SERVER_TIMESTAMP,
                "assignedDroneId": "",
                "assignedMissionId": "",
            },
        )

        if (i + 1) % 400 == 0:
            batch.commit()
            batch = db.batch()

    batch.commit()
    print(f"- {count} orders seeded")


if __name__ == "__main__":
    clear_firestore()
    seed_large()  # Change to seed_small(), seed_medium(), or seed_large()
