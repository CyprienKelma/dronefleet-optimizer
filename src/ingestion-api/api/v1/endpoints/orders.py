from fastapi import APIRouter, Depends, HTTPException, status
from functools import lru_cache
from typing import Any, Dict

from shared.schemas.request import DeliveryRequest
from ....services.request import RequestService

router = APIRouter()

@lru_cache()
def get_service() -> RequestService:
    return RequestService()

@router.post("/orders", status_code=status.HTTP_201_CREATED)
async def create_order(
    request: DeliveryRequest,
    service: RequestService = Depends(get_service)
) -> Dict[str, Any]:
    """
    Ingest a new delivery order.

    - Validates the payload (Pydantic)
    - Publishes to the event bus
    - Returns the request ID
    """
    try:
        request_id = service.process_order(request)
        return {
            "request_id": request_id,
            "status": "QUEUED",
            "message": "Order successfully received and queued for processing."
        }
    except RuntimeError as e:
        # 500 Internal Server Error if publishing fails
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=str(e)
        )
    except Exception as e:
        # Catch-all for other unexpected errors
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Error processing request: {str(e)}"
        )

@router.get("/orders/{order_id}")
async def get_order(order_id: str) -> Dict[str, Any]:
    # Placeholder for status check - would likely query Firestore or State Manager
    return {
        "order_id": order_id,
        "state": "UNKNOWN", # Implementation pending State Manager integration
        "note": "Status lookup not yet connected to persistence layer."
    }
