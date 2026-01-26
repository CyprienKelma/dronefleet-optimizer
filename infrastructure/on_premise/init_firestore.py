import time
import requests
import os

def wait_for_firestore():
    emulator_host = os.getenv("FIRESTORE_EMULATOR_HOST", "localhost:8080")
    print(f"Waiting for Firestore emulator at {emulator_host}...")

    url = f"http://{emulator_host}"
    retries = 0
    max_retries = 30

    while retries < max_retries:
        try:
            # Simple GET request to check if the emulator is up
            response = requests.get(url)
            if response.status_code == 200:
                print("Firestore emulator is ready.")
                return True
        except Exception:
            pass

        time.sleep(2)
        retries += 1
        print(f"  -- Attempt {retries}/{max_retries}...")

    return False

if __name__ == "__main__":
    if wait_for_firestore():
        print("Firestore initialization successful.")
    else:
        print("Could not connect to Firestore emulator. Exiting.")
        exit(1)
