import random
import signal
import time
import uuid
from datetime import UTC, datetime

import requests
import structlog

from shared.configs.logging_config import setup_logging
from shared.schemas.protocol import DroneStatus

# Assuming the shared schemas are available in the python path
# If running via 'uv run', the python path should be set correctly to include src/
from shared.schemas.telemetry import DroneTelemetry, GeoPoint

# Configuration
API_URL = "http://localhost:8000/api/v1/telemetry"
DRONE_COUNT = 15
UPDATE_INTERVAL_SEC = 1.0

# Setup logging
setup_logging()
logger = structlog.get_logger(__name__)


class SimulatedDrone:
    def __init__(self, drone_id: str):
        self.drone_id = drone_id
        # Start around Paris (Lat: 48.8566, Lon: 2.3522)
        self.lat = 48.8566 + (random.uniform(-0.05, 0.05))
        self.lon = 2.3522 + (random.uniform(-0.05, 0.05))
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

        for drone in drones:
            drone.update()
            telemetry = drone.get_telemetry()

            try:
                # We use model_dump(mode='json') to handle datetime serialization
                # automatically before sending, but since we use requests which
                # expects a dict/json, we can pass the dict directly if using
                # the 'json' parameter.
                payload = telemetry.model_dump(mode="json")

                response = requests.post(API_URL, json=payload, timeout=0.5)
                if response.status_code != 202:
                    logger.warning(
                        "Failed to push telemetry",
                        drone_id=drone.drone_id,
                        status_code=response.status_code,
                    )

            except requests.exceptions.RequestException as e:
                logger.error("Connection error", error=str(e))
                # Don't crash the simulator if API is down, just wait and retry

        # Calculate time to sleep to maintain interval
        elapsed = time.time() - start_time
        sleep_time = max(0, UPDATE_INTERVAL_SEC - elapsed)

        if running:
            # logger.info(
            #     f"Broadcasted telemetry for {len(drones)} drones. "
            #     f"Sleeping {sleep_time:.2f}s"
            # )
            time.sleep(sleep_time)

    logger.info("Simulator stopped.")


if __name__ == "__main__":
    main()
