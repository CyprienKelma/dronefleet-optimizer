import random
import signal
import time
import uuid
from datetime import UTC, datetime

import requests
import structlog
from dronefleet_shared.models import (
    DroneStatus,
    DroneTelemetry,
    Order,
    OrderPriority,
    OrderStatus,
    Position,
    ProductType,
)
from dronefleet_shared.utils.logging_config import setup_logging

# Configuration
TELEMETRY_API_URL = "http://localhost:8000/api/v1/telemetry"
ORDERS_API_URL = "http://localhost:8000/api/v1/orders"
DRONE_COUNT = 5
UPDATE_INTERVAL_SEC = 3.0
ORDER_PROBABILITY = 0.05  # 5% chance to generate an order per loop iteration

# Center: Paris (Lat: 48.8566, Lon: 2.3522)
# Keep generated points within ~3-4 km for feasible VRP (battery, time windows).
PARIS_LAT = 48.8566
PARIS_LON = 2.3522
# ~0.03 degrees ~ 3.3 km at Paris latitude
ZONE_RADIUS = 0.03

# Setup logging
setup_logging()
logger = structlog.get_logger(__name__)


class SimulatedDrone:
    def __init__(self, drone_id: str):
        self.drone_id = drone_id
        # Start around Paris (within zone radius)
        self.lat = PARIS_LAT + (random.uniform(-ZONE_RADIUS, ZONE_RADIUS))
        self.lon = PARIS_LON + (random.uniform(-ZONE_RADIUS, ZONE_RADIUS))
        self.battery = random.uniform(70.0, 100.0)
        self.speed = 0.0
        self.status = DroneStatus.DRONE_STATUS_IDLE
        self.mission_id = None

        # Movement vector (simple random walk)
        self.lat_velocity = random.uniform(-0.0001, 0.0001)
        self.lon_velocity = random.uniform(-0.0001, 0.0001)

    def update(self):
        """Update drone state for the next time step."""
        # Update position
        if self.status in [
            DroneStatus.DRONE_STATUS_MOVING,
            DroneStatus.DRONE_STATUS_DELIVERING,
        ]:
            self.lat += self.lat_velocity
            self.lon += self.lon_velocity
            self.speed = random.uniform(30.0, 60.0)  # km/h
            self.battery -= 0.005  # Drain battery
        else:
            self.speed = 0.0
            self.battery -= 0.001  # Idle drain

        # Simple state machine simulation
        # 10% chance to start moving if IDLE
        if self.status == DroneStatus.DRONE_STATUS_IDLE and random.random() < 0.1:
            self.status = DroneStatus.DRONE_STATUS_MOVING
            self.lat_velocity = random.uniform(-0.0005, 0.0005)
            self.lon_velocity = random.uniform(-0.0005, 0.0005)
            self.mission_id = str(uuid.uuid4())

        # 5% chance to stop if MOVING
        elif self.status == DroneStatus.DRONE_STATUS_MOVING and random.random() < 0.05:
            self.status = DroneStatus.DRONE_STATUS_IDLE
            self.mission_id = None

        # Clamp battery
        if self.battery < 0:
            self.battery = 0
            self.status = DroneStatus.DRONE_STATUS_IDLE

    def get_telemetry(self) -> DroneTelemetry:
        return DroneTelemetry(
            drone_id=self.drone_id,
            timestamp=datetime.now(UTC),
            position=Position(lat=self.lat, lon=self.lon),
            battery_percentage=round(self.battery, 2),
            speed_kmh=round(self.speed, 2),
            status=self.status,
            current_mission_id=self.mission_id,
        )


class SimulatedOrderGenerator:
    # Pre-filter UNSPECIFIED enum values (proto zero-value sentinels)
    _VALID_PRIORITIES = [
        p for p in OrderPriority if p != OrderPriority.ORDER_PRIORITY_UNSPECIFIED
    ]
    _VALID_PRODUCT_TYPES = [
        p for p in ProductType if p != ProductType.PRODUCT_TYPE_UNSPECIFIED
    ]

    @staticmethod
    def generate_random_order() -> Order:
        # Generate coordinates within compact zone (~3-4 km) for feasible VRP
        pickup_lat = PARIS_LAT + random.uniform(-ZONE_RADIUS, ZONE_RADIUS)
        pickup_lon = PARIS_LON + random.uniform(-ZONE_RADIUS, ZONE_RADIUS)

        # Delivery within ~1-2 km of pickup
        delivery_offset = 0.015  # ~1.5 km
        delivery_lat = pickup_lat + random.uniform(-delivery_offset, delivery_offset)
        delivery_lon = pickup_lon + random.uniform(-delivery_offset, delivery_offset)

        priority = random.choice(SimulatedOrderGenerator._VALID_PRIORITIES)
        product_type = random.choice(SimulatedOrderGenerator._VALID_PRODUCT_TYPES)

        # contents_map = {
        #     ProductType.PRODUCT_TYPE_BLOOD: ["O- Negative Blood Bags", "A+ Plasma", "Platelets"],
        #     ProductType.PRODUCT_TYPE_MEDICINE: ["Antibiotics", "Insulin", "Painkillers"],
        #     ProductType.PRODUCT_TYPE_VACCINE: ["Covid Vaccines", "Flu Vaccines"],
        #     ProductType.PRODUCT_TYPE_ORGAN: ["Kidney", "Heart", "Liver"],
        #     ProductType.PRODUCT_TYPE_MEDICAL_DEVICE: ["Defibrillator", "EPIPen", "First Aid Kit"],
        # }

        # content = random.choice(contents_map.get(product_type, ["Medical Supplies"]))

        return Order(
            id=str(uuid.uuid4()),
            priority=priority,
            pickup_location=Position(lat=pickup_lat, lon=pickup_lon),
            delivery_location=Position(lat=delivery_lat, lon=delivery_lon),
            product_type=product_type.name,
            status=OrderStatus.ORDER_STATUS_PENDING,
            created_at=datetime.now(UTC),
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
                payload = telemetry.to_dict()
                # Betterproto to_dict doesn't serialize datetime to string by default
                if isinstance(payload.get("timestamp"), datetime):
                    payload["timestamp"] = payload["timestamp"].isoformat()

                response = requests.post(TELEMETRY_API_URL, json=payload, timeout=0.5)
                if response.status_code != 202:
                    logger.warning(
                        "Failed to push telemetry",
                        drone_id=drone.drone_id,
                        status_code=response.status_code,
                        error=response.text,
                        payload=payload,
                    )

            except requests.exceptions.RequestException as e:
                logger.error(f"Telemetry API connection error: {e}")

        # 2. Randomly generate and send a new order
        if random.random() < ORDER_PROBABILITY:
            order = SimulatedOrderGenerator.generate_random_order()
            logger.info(
                "Generating new random order",
                order_id=order.id,
                priority=order.priority,
            )

            try:
                payload = order.to_dict()
                # Serialize any datetime fields if present (created_at might be None or datetime)
                if isinstance(payload.get("created_at"), datetime):
                    payload["created_at"] = payload["created_at"].isoformat()

                response = requests.post(ORDERS_API_URL, json=payload, timeout=0.5)
                if response.status_code == 201:
                    logger.info("Successfully pushed order", order_id=order.id)
                else:
                    logger.warning(
                        "Failed to push order",
                        order_id=order.id,
                        status_code=response.status_code,
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
