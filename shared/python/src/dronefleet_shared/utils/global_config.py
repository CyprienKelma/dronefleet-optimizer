from typing import Literal

from pydantic import Field, ValidationError
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """
    Application settings with environment-based configuration.
    Automatically loads the appropriate .env file based on ENVIRONMENT variable.
    """

    # Environment
    environment: Literal["local", "dev", "prod"] = "local"
    deployment_strategy: Literal["on_cloud", "on_premise"] = "on_cloud"

    # GCP
    project_id: Literal[
        "drone-fleet-optimizer-local",
        "drone-fleet-optimizer-dev",
        "drone-fleet-optimizer-prod",
    ] = "drone-fleet-optimizer-local"
    gcp_region: str = Field(default="europe-west1", description="GCP Region")

    # Cloud Run
    min_instances: int = Field(
        default=0, ge=0, le=100, description="Minimum Cloud Run instances"
    )

    # Pub/Sub
    pubsub_emulator_host: str | None = Field(
        default=None, description="Pub/Sub emulator host (for local dev)"
    )
    pubsub_topic_telemetry: str = Field(
        default="telemetry", description="Pub/Sub topic for telemetry"
    )
    pubsub_topic_orders: str = Field(
        default="orders", description="Pub/Sub topic for orders"
    )
    pubsub_topic_commands: str = Field(
        default="commands", description="Pub/Sub topic for commands"
    )

    # Kafka (for on_premise)
    kafka_bootstrap_servers: str = Field(
        default="localhost:9092", description="Kafka bootstrap servers"
    )

    # Cloud Run URLs
    state_manager_url: str = Field(
        default="http://localhost:8080", description="State Manager Cloud Run URL"
    )
    optimizer_url: str | None = Field(
        default=None, description="Optimizer Cloud Run URL"
    )

    # Pub/Sub Topics
    pubsub_topic_decisions: str = Field(
        default="decisions", description="Pub/Sub topic for decisions"
    )

    # Solver parameters
    solver_time_limit_seconds: int = Field(
        default=180, description="Solver time limit in seconds"
    )
    min_battery_threshold: int = Field(
        default=20, description="Minimum battery threshold for optimization"
    )

    # Drone specs (consumption per km by model)
    drone_specs: dict[str, dict] = Field(
        default={"DEFAULT": {"battery_per_km": 1.0, "max_payload_kg": 5.0}},
        description="Drone specifications by model",
    )

    # API Settings (for local)
    ingestion_api_host: str = Field(default="0.0.0.0", description="Ingestion API host")
    ingestion_api_port: int = Field(default=8000, description="Ingestion API port")

    # Firestore Emulator (for local)
    firestore_emulator_host: str | None = Field(
        default=None, description="Firestore emulator host"
    )

    # Feature Flags
    feature_battery_optimization: bool = Field(
        default=False, description="Enable battery optimization feature"
    )
    feature_advanced_vrp: bool = Field(
        default=False, description="Enable advanced VRP feature"
    )

    # Logging
    log_level: str = Field(
        default="INFO", pattern="^(DEBUG|INFO|WARNING|ERROR)$", description="Log level"
    )

    # Monitoring (for prod)
    enable_profiling: bool = Field(default=False, description="Enable profiling")
    metrics_export_interval: int = Field(
        default=60, description="Metrics export interval in seconds"
    )

    @property
    def is_local(self) -> bool:
        """Check if running in local environment."""
        return self.environment == "local"

    @property
    def is_production(self) -> bool:
        """Check if running in production environment."""
        return self.environment == "prod"

    model_config = SettingsConfigDict(
        env_file_encoding="utf-8",
        case_sensitive=False,  # to have PROJECT_ID = project_id
        extra="ignore",  # to skip undefined vars
    )


def _resolve_env_file() -> str | None:
    """Resolve the .env file path based on the ENVIRONMENT variable.

    Tries two strategies:
    1. Relative path `configs/{env}.env` — works inside Docker containers
       where WORKDIR=/app and configs/ is copied alongside the code.
    2. Walk up from this file's location to find the repo root's configs/
       directory — works when running locally via mise or pytest.

    Returns None if not found, which is fine since OS environment variables
    (from docker-compose, Cloud Run --set-env-vars, or mise) always take
    precedence over .env files in pydantic-settings.
    """
    import os
    from pathlib import Path

    env = os.getenv("ENVIRONMENT", "local")
    filename = f"{env}.env"

    # Strategy 1: relative to cwd (Docker: /app/configs/dev.env)
    relative = Path("configs") / filename
    if relative.is_file():
        return str(relative)

    # Strategy 2: walk up from this file to find configs/ directory
    current = Path(__file__).resolve().parent
    for _ in range(10):  # safety bound
        candidate = current / "configs" / filename
        if candidate.is_file():
            return str(candidate)
        if current == current.parent:
            break  # reached filesystem root
        current = current.parent

    return None


# Instantiation with dynamic env file loading
try:
    settings = Settings(_env_file=_resolve_env_file())
except ValidationError as e:
    raise SystemExit(f"Config error: {e}") from e


if __name__ == "__main__":
    print("Loaded Settings:")
    print(settings.model_dump_json(indent=2))
