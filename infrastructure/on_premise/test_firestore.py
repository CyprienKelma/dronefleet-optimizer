from google.cloud import firestore

PROJECT_ID = "drone-fleet-optimizer-local"
FIRESTORE_EMULATOR_HOST = "localhost:8080"


def test_firestore_connection():
    print(f"Connecting to Firestore Emulator at {FIRESTORE_EMULATOR_HOST}...")
    print(f"Project ID: {PROJECT_ID}")

    try:
        db = firestore.Client(project=PROJECT_ID)

        collections = ["drones", "orders", "missions"]

        for coll_name in collections:
            print(f"\n--- Collection: {coll_name} ---")
            docs = db.collection(coll_name).stream()

            count = 0
            for doc in docs:
                print(f"Document ID: {doc.id}")
                print(f"Data: {doc.to_dict()}")
                count += 1

            if count == 0:
                print(f"No documents found in '{coll_name}'.")
            else:
                print(f"Found {count} documents.")

    except Exception as e:
        print(f"\nError: Could not connect or fetch data from Firestore: {e}")
        print("Ensure the Firestore emulator is running (e.g., via docker-compose).")


if __name__ == "__main__":
    test_firestore_connection()
