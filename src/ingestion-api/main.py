from fastapi import FastAPI
from .api.v1.endpoints.orders import router as orders_router
from .api.v1.endpoints.states import router as states_router
from .api.v1.endpoints.telemetry import router as telemetry_router

app = FastAPI()

# Register routers
app.include_router(orders_router, prefix="/api/v1", tags=["orders"])
app.include_router(telemetry_router, prefix="/api/v1", tags=["telemetry"])
app.include_router(states_router, prefix="/api/v1", tags=["states"])
