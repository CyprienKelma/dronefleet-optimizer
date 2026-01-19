from fastapi import APIRouter
from typing import Any

router = APIRouter()


@router.get("/states/{drone_id}")
async def get_order(drone_id: int) -> dict[str, Any]:

    return {
        "drone_id": drone_id,
        "position": "24°45°187°"
    }
