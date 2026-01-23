import logging
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
    state_manager_url: str | None = Field(
        default=None, description="State Manager Cloud Run URL"
    )
    optimizer_url: str | None = Field(
        default=None, description="Optimizer Cloud Run URL"
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
        env_file=f"config/{environment}.env",  # Autoloaded based on ENVIRONMENT
        env_file_encoding="utf-8",
        case_sensitive=False,  # to have PROJECT_ID = project_id
        extra="ignore",  # to skip undefined vars
    )


# Instantiation with dynamic env file loading
logger = logging.getLogger(__name__)

try:
    settings = Settings()  # get correct env from model_config
    logger.info(f"Config loaded for environment: {settings.environment}")
    logger.info(f"Project ID: {settings.project_id}")
    logger.info(f"Deployment strategy: {settings.deployment_strategy}")
except ValidationError as e:
    logger.error(f"Config error: {e}")
    exit(1)
