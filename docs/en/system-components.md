# System Components

## Ingestion API

**Location**: `services/ingestion/`

**Responsibilities:**
- HTTP REST endpoint for telemetry and order ingestion
- Pydantic validation of incoming payloads
- Pub/Sub publishing with error handling
- Health check endpoint

**Technology**: FastAPI (Python 3.11), uvicorn ASGI server

**Key Files:**
- `src/ingestion/api/v1/endpoints/telemetry.py` - Telemetry endpoint
- `src/ingestion/api/v1/endpoints/orders.py` - Orders endpoint
- `src/ingestion/services/` - Business logic and Pub/Sub publishers

## State Manager

**Location**: `services/state_manager/`

**Responsibilities:**
- Consume Pub/Sub events (telemetry, orders, decisions)
- Maintain state consistency via Firestore transactions
- Provide optimization snapshot endpoint
- Implement business policies (drone availability, mission assignment)

**Technology**: Java 21, Spring Boot 4, Google Cloud Firestore SDK

**Architecture**: Hexagonal (Ports and Adapters)
- **Domain Layer**: Business logic and policies
- **Application Layer**: Use cases and DTOs
- **Infrastructure Layer**: Firestore adapters, Pub/Sub listeners, REST controllers

**Key Files:**
- `domain/service/MissionAssignmentPolicy.java` - Mission validation logic
- `domain/service/OptimizationSnapshotService.java` - Snapshot generation
- `infrastructure/adapter/in/messaging/pubsub/` - Event listeners
- `infrastructure/adapter/out/persistence/firestore/` - Database adapters

**Concurrency Handling:**
- Firestore transactions for atomic multi-document writes
- Optimistic locking with automatic retry
- Write-time validation (first-write-wins strategy)
- Timestamp ordering for telemetry
- Idempotency guards for order ingestion

## Path Optimizer

**Location**: `services/path_optimizer/`

**Responsibilities:**
- Fetch optimization snapshot from State Manager
- Build and solve Vehicle Routing Problem
- Publish mission assignments to decisions topic

**Technology**: Python 3.11, Google OR-Tools, Haversine distance calculation

**Key Files:**
- `main.py` - Entry point and orchestration
- `services/builder.py` - VRP model construction
- `services/solver.py` - OR-Tools solver configuration
- `services/extractor.py` - Solution parsing and waypoint classification
- `clients/state_manager.py` - HTTP client for snapshot endpoint
- `clients/publisher.py` - Pub/Sub decision publisher

**Execution Model**: Stateless Cloud Run Job triggered by Cloud Scheduler (10-second interval)

## Simulator

**Location**: `services/simulators/`

**Responsibilities:**
- Generate synthetic telemetry data (drone movements)
- Generate synthetic delivery orders
- Consume mission assignments (work in progress)
- Execute missions by publishing telemetry along route

**Technology**: Python 3.11, asyncio for concurrent drone simulation

**Status**: Telemetry and order generation implemented, mission consumption in progress

## Frontend Visualizer

**Location**: `services/visualizer/`

**Responsibilities:**
- Real-time map display of drone positions
- WebSocket server for live telemetry streaming
- Mission route visualization
- Metrics dashboard (battery levels, active missions)

**Technology**: TypeScript, SolidJS, Vite, Leaflet (map library), Bun runtime

**Status**: Work in progress
