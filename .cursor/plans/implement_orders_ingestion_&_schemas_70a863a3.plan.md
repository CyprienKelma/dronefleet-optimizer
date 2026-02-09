---
name: Implement Orders Ingestion & Schemas
overview: Define core Pydantic schemas for Drones, Warehouses, and Products, and implement the Order Ingestion API endpoint with business logic to publish valid requests to Pub/Sub.
todos:
  - id: schemas
    content: Define Product, Drone, and Warehouse schemas
    status: completed
  - id: service
    content: Implement RequestService with Pub/Sub publishing
    status: completed
    dependencies:
      - schemas
  - id: endpoint
    content: Implement Orders API endpoint
    status: completed
    dependencies:
      - service
isProject: false
---

# Implement Orders Ingestion & Schemas

## 1. Define Shared Schemas

We will populate the empty schema files with realistic, typed Pydantic models.

- **[src/shared/schemas/product.py](src/shared/schemas/product.py)**
  - Define `ProductType` Enum (Medicine, Organ, Blood, Device, etc.).
  - Define `Product` model (id, name, type, weight, temp_range, etc.).
- **[src/shared/schemas/drones.py](src/shared/schemas/drones.py)**
  - Define `DroneModel` Enum (Light, Heavy, LongRange).
  - Define `Drone` model (id, model_type, max_payload, battery_capacity, max_range, etc.).
- **[src/shared/schemas/warehouses.py](src/shared/schemas/warehouses.py)**
  - Define `Warehouse` model (id, name, location [GeoPoint], capacity, authorized_products, etc.).
- **[src/shared/schemas/request.py](src/shared/schemas/request.py)**
  - Enhance `DeliveryRequest` to include `product_type` (referencing `product.py`) or keep as is but validate against product rules if needed.

## 2. Implement Business Logic Service

Create the logic to validate and publish orders.

- **[src/ingestion/services/request.py](src/ingestion/services/request.py)**
  - Create `RequestService` class.
  - Initialize `PublisherFactory` to get the correct publisher.
  - Implement `process_order(request: DeliveryRequest)`:
    - Validate logic (e.g., weight limits vs priority - though basic validation is in Pydantic).
    - Publish message to Pub/Sub topic `demandes`.
    - Handle errors/exceptions.

## 3. Implement API Endpoint

Connect the HTTP layer to the service.

- **[src/ingestion/api/v1/endpoints/orders.py](src/ingestion/api/v1/endpoints/orders.py)**
