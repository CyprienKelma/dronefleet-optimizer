import random
import signal
import time
import uuid
from datetime import UTC, datetime

import requests
import structlog

from shared.configs.logging_config import setup_logging
from shared.schemas.order import DeliveryOrder
from shared.schemas.product import ProductType
from shared.schemas.protocol import DroneStatus, UrgencyLevel

# Assuming the shared schemas are available in the python path
# If running via 'uv run', the python path should be set correctly to include src/
from shared.schemas.telemetry import DroneTelemetry, GeoPoint

# Configuration
TELEMETRY_API_URL = "http://localhost:8000/api/v1/telemetry"
ORDERS_API_URL = "http://localhost:8000/api/v1/orders"
DRONE_COUNT = 15
UPDATE_INTERVAL_SEC = 1.0
ORDER_PROBABILITY = 0.05  # 5% chance to generate an order per loop iteration

# Center: Paris (Lat: 48.8566, Lon: 2.3522)
PARIS_LAT = 48.8566
PARIS_LON = 2.3522

# Setup logging
setup_logging()
logger = structlog.get_logger(__name__)


class SimulatedDrone:
    def __init__(self, drone_id: str):
        self.drone_id = drone_id
        # Start around Paris
        self.lat = PARIS_LAT + (random.uniform(-0.05, 0.05))
        self.lon = PARIS_LON + (random.uniform(-0.05, 0.05))
        self.battery = random.uniform(50.0, 100.0)
        self.speed = 0.0
        self.status = DroneStatus.IDLE
        self.mission_id = None

        # Movement vector (simple random walk)
        self.lat_velocity = random.uniform(-0.0001, 0.0001)
        self.lon_velocity = random.uniform(-0.0001, 0.0001)

    def update(self):
        """Update drone state for the next time step."""
        # Update position
        if self.status in [DroneStatus.MOVING, DroneStatus.DELIVERING]:
            self.lat += self.lat_velocity
            self.lon += self.lon_velocity
            self.speed = random.uniform(30.0, 60.0)  # km/h
            self.battery -= 0.05  # Drain battery
        else:
            self.speed = 0.0
            self.battery -= 0.01  # Idle drain

        # Simple state machine simulation
        # 10% chance to start moving if IDLE
        if self.status == DroneStatus.IDLE and random.random() < 0.1:
            self.status = DroneStatus.MOVING
            self.lat_velocity = random.uniform(-0.0005, 0.0005)
            self.lon_velocity = random.uniform(-0.0005, 0.0005)
            self.mission_id = str(uuid.uuid4())

        # 5% chance to stop if MOVING
        elif self.status == DroneStatus.MOVING and random.random() < 0.05:
            self.status = DroneStatus.IDLE
            self.mission_id = None

        # Clamp battery
        if self.battery < 0:
            self.battery = 0
            self.status = DroneStatus.IDLE

    def get_telemetry(self) -> DroneTelemetry:
        return DroneTelemetry(
            drone_id=self.drone_id,
            timestamp=datetime.now(UTC),
            position=GeoPoint(lat=self.lat, lon=self.lon),
            battery_percentage=round(self.battery, 2),
            speed_kmh=round(self.speed, 2),
            status=self.status,
            current_mission_id=self.mission_id,
        )


class SimulatedOrderGenerator:
    @staticmethod
    def generate_random_order() -> DeliveryOrder:
        # Generate coordinates within Paris area (±0.06 to ±0.2 for realistic but distinct locations)
        pickup_lat = PARIS_LAT + random.uniform(-0.1, 0.1)
        pickup_lon = PARIS_LON + random.uniform(-0.1, 0.1)

        # Delivery location relatively close but not too close (between 2km and 15km approx)
        # 0.01 degree is approx 1.1km
        delivery_lat = pickup_lat + random.uniform(-0.05, 0.05)
        delivery_lon = pickup_lon + random.uniform(-0.05, 0.05)

        priority = random.choice(list(UrgencyLevel))
        product_type = random.choice(list(ProductType))

        contents_map = {
            ProductType.BLOOD: ["O- Negative Blood Bags", "A+ Plasma", "Platelets"],
            ProductType.MEDICINE: ["Antibiotics", "Insulin", "Painkillers"],
            ProductType.VACCINE: ["Covid Vaccines", "Flu Vaccines"],
            ProductType.ORGAN: ["Kidney", "Heart", "Liver"],
            ProductType.MEDICAL_DEVICE: ["Defibrillator", "EPIPen", "First Aid Kit"],
        }

        content = random.choice(contents_map.get(product_type, ["Medical Supplies"]))

        return DeliveryOrder(
            priority=priority,
            pickup_location=GeoPoint(lat=pickup_lat, lon=pickup_lon),
            dropoff_location=GeoPoint(lat=delivery_lat, lon=delivery_lon),
            product_type=product_type,
            package_weight_kg=round(random.uniform(0.1, 5.0), 2),
            content_description=content,
            requires_cold_chain=(
                product_type
                in [ProductType.VACCINE, ProductType.BLOOD, ProductType.ORGAN]
            ),
            requester_id=f"HOSP-{random.randint(1, 100):03d}",
        )


def main():
    logger.info("Starting Drone Fleet Simulator", drone_count=DRONE_COUNT)

    # Initialize fleet
    drones: list[SimulatedDrone] = [
        SimulatedDrone(f"DRONE-{i + 1:03d}") for i in range(DRONE_COUNT)
    ]

    running = True

    def signal_handler(sig, frame):
        nonlocal running
        logger.info("Stopping simulator...")
        running = False

    signal.signal(signal.SIGINT, signal_handler)

    while running:
        start_time = time.time()

        # 1. Update and send telemetry for each drone
        for drone in drones:
            drone.update()
            telemetry = drone.get_telemetry()

            try:
                payload = telemetry.model_dump(mode="json")
                response = requests.post(TELEMETRY_API_URL, json=payload, timeout=0.5)
                if response.status_code != 202:
                    logger.warning(
                        "Failed to push telemetry",
                        drone_id=drone.drone_id,
                        status_code=response.status_code,
                    )

            except requests.exceptions.RequestException as e:
                logger.error(f"Telemetry API connection error: {e}")

        # 2. Randomly generate and send a new order
        if random.random() < ORDER_PROBABILITY:
            order = SimulatedOrderGenerator.generate_random_order()
            logger.info(
                f"Generating new random order: {order.order_id} ({order.priority})"
            )

            try:
                payload = order.model_dump(mode="json")
                response = requests.post(ORDERS_API_URL, json=payload, timeout=0.5)
                if response.status_code == 201:
                    logger.info(f"Successfully pushed order {order.order_id}")
                else:
                    logger.warning(
                        f"Failed to push order {order.order_id}: {response.status_code}"
                    )
            except requests.exceptions.RequestException as e:
                logger.error(f"Orders API connection error: {e}")

        # Calculate time to sleep to maintain interval
        elapsed = time.time() - start_time
        sleep_time = max(0, UPDATE_INTERVAL_SEC - elapsed)

        if running:
            time.sleep(sleep_time)

    logger.info("Simulator stopped.")


if __name__ == "__main__":
    main()
