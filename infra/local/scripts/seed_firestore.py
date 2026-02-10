import os
import uuid

from google.cloud import firestore

# Configuration pour l'émulateur
os.environ["FIRESTORE_EMULATOR_HOST"] = "localhost:8080"

def seed_data():
    db = firestore.Client(project="drone-fleet-optimizer-local")

    print("Seeding initial data...")

    # 1. Main Depot
    depot_ref = db.collection("depots").document("DEPOT-PARIS-01")
    depot_ref.set({
        "name": "Paris Main Hub",
        "position": {"lat": 48.8566, "lon": 2.3522},
        "capacity": 50,
        "chargingSlots": 10
    })
    print("- Main depot seeded")

    # 2. Warehouses
    warehouses = [
        {"id": "WH-NORTH", "name": "North Logistics Center", "pos": {"lat": 48.90, "lon": 2.35}},
        {"id": "WH-SOUTH", "name": "South Distribution Hub", "pos": {"lat": 48.80, "lon": 2.35}}
    ]
    for wh in warehouses:
        db.collection("warehouses").document(wh["id"]).set({
            "name": wh["name"],
            "position": wh["pos"],
            "authorizedProductTypes": ["BLOOD", "MEDICINE", "VACCINE", "ORGAN"],
            "isColdStorageCapable": True
        })
    print("- Warehouses seeded")

    # 3. Static Drones (IDLE and ready for optimization)
    drones = [
        {"id": "DRONE-DEBUG-01", "bat": 100.0},
        {"id": "DRONE-DEBUG-02", "bat": 85.5},
        {"id": "DRONE-DEBUG-03", "bat": 92.0},
        {"id": "DRONE-DEBUG-04", "bat": 88.0},
        {"id": "DRONE-DEBUG-05", "bat": 70.0},
    ]
    for d in drones:
        db.collection("drones").document(d["id"]).set({
            "batteryPercentage": d["bat"],
            "speedKmh": 0.0,
            "status": "DRONE_STATUS_IDLE",
            "homeDepotId": "DEPOT-PARIS-01",
            "position": {"lat": 48.8566, "lon": 2.3522}, # Start at depot
            "consumptionPerKm": 0.1,
            "maxFlightTimeMinutes": 30,
            "currentMissionId": "",
            "solvingSessionId": ""
        })
    print("- Drones seeded")

    # 4. Pending Orders
    orders = [
        {"id": str(uuid.uuid4()), "priority": "HIGH", "target": {"lat": 48.87, "lon": 2.33}},
        {"id": str(uuid.uuid4()), "priority": "CRITICAL", "target": {"lat": 48.84, "lon": 2.37}},
        {"id": str(uuid.uuid4()), "priority": "STANDARD", "target": {"lat": 48.88, "lon": 2.30}},
        {"id": str(uuid.uuid4()), "priority": "HIGH", "target": {"lat": 48.82, "lon": 2.32}},
        {"id": str(uuid.uuid4()), "priority": "CRITICAL", "target": {"lat": 48.89, "lon": 2.35}},
        {"id": str(uuid.uuid4()), "priority": "STANDARD", "target": {"lat": 48.86, "lon": 2.36}},
        {"id": str(uuid.uuid4()), "priority": "STANDARD", "target": {"lat": 48.68, "lon": 2.38}},
        {"id": str(uuid.uuid4()), "priority": "STANDARD", "target": {"lat": 48.90, "lon": 2.40}},
        {"id": str(uuid.uuid4()), "priority": "STANDARD", "target": {"lat": 48.92, "lon": 2.42}},
    ]
    for o in orders:
        db.collection("orders").document(o["id"]).set({
            "status": "ORDER_STATUS_PENDING",
            "priority": o["priority"],
            "productType": "BLOOD",
            "pickupLocation": {"lat": 48.90, "lon": 2.35}, # From North WH
            "deliveryLocation": o["target"],
            "createdAt": firestore.SERVER_TIMESTAMP,
            "assignedDroneId": "",
            "assignedMissionId": ""
        })
    print("- Orders seeded")

    print("Seeding done")

if __name__ == "__main__":
    seed_data()
