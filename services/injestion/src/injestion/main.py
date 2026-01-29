from fastapi import FastAPI

from dronefleet_shared.utils.logging_config import setup_logging

from .api.v1.endpoints.orders import router as orders_router
from .api.v1.endpoints.telemetry import router as telemetry_router

# Initialize logging
setup_logging()

app = FastAPI()

# Register routers
app.include_router(orders_router, prefix="/api/v1", tags=["orders"])
app.include_router(telemetry_router, prefix="/api/v1", tags=["telemetry"])
