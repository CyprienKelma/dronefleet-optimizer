import os

from google.cloud import firestore


def seed():
    project_id = "drone-fleet-optimizer-local"
    # Set emulator host before initializing client
    os.environ["FIRESTORE_EMULATOR_HOST"] = "localhost:8080"

    print(f"Connecting to Firestore Emulator at {os.environ['FIRESTORE_EMULATOR_HOST']}...")
    db = firestore.Client(project=project_id)

    # 1. Seed Main Depot
    print("Seeding main depot...")
    depot_ref = db.collection("depots").document("DEPOT-PARIS-01")
    depot_ref.set({
        "name": "Paris Main Hub",
        "position": {"lat": 48.8566, "lon": 2.3522},
        "capacity": 50,
        "chargingSlots": 20
    })

    # 2. Seed some Warehouses
    print("Seeding warehouses...")
    warehouses = [
        {
            "id": "WH-NORTH",
            "name": "North Logistics Center",
            "position": {"lat": 48.90, "lon": 2.35},
            "authorizedProductTypes": ["BLOOD", "MEDICINE", "VACCINE"],
            "isColdStorageCapable": True
        },
        {
            "id": "WH-SOUTH",
            "name": "South Distribution Hub",
            "position": {"lat": 48.80, "lon": 2.35},
            "authorizedProductTypes": ["MEDICINE", "MEDICAL_DEVICE"],
            "isColdStorageCapable": False
        }
    ]

    for wh in warehouses:
        db.collection("warehouses").document(wh["id"]).set(wh)

    print("Seed completed successfully!")

if __name__ == "__main__":
    seed()
