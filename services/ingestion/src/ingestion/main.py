from contextlib import asynccontextmanager

from dronefleet_shared.utils.logging_config import setup_logging
from fastapi import FastAPI

from .api.v1.endpoints.health import router as health_router
from .api.v1.endpoints.orders import router as orders_router
from .api.v1.endpoints.telemetry import router as telemetry_router
from .services.health import HealthService

setup_logging()


@asynccontextmanager
async def lifespan(app: FastAPI):
    health_service = HealthService()
    await health_service.start()
    app.state.health = health_service
    yield
    health_service.shutdown()


app = FastAPI(lifespan=lifespan)

app.include_router(health_router, tags=["health"])
app.include_router(orders_router, prefix="/api/v1", tags=["orders"])
app.include_router(telemetry_router, prefix="/api/v1", tags=["telemetry"])
