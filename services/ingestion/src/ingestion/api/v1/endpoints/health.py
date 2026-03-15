from fastapi import APIRouter, Depends

from ....services.health import HealthService

router = APIRouter()


def get_health_service() -> HealthService:
    from ....main import app

    return app.state.health


@router.get("/healthz/live")
async def liveness() -> dict:
    return {"status": "alive"}


@router.get("/healthz/ready")
async def readiness(
    health_service: HealthService = Depends(get_health_service),
) -> dict:
    return health_service.check_readiness()
