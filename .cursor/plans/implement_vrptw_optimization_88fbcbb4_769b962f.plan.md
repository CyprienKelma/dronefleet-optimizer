---
name: implement_vrptw_optimization_88fbcbb4
overview: Transform the system from simple assignment to Multi-Trip VRPTW with battery constraints, time windows, and multi-warehouse selection. Updates shared models, State Manager (Java), and Optimizer (Python).
todos:
  - id: 1-shared-java
    content: Refactor Shared Java Models (Drone, Order, Mission, Warehouse, Depot)
    status: completed
  - id: 2-shared-python
    content: Refactor Shared Python Models to match Java definitions
    status: completed
  - id: 3-statemanager-snapshot
    content: Implement State Manager Snapshot Service (Optimistic Locking) & Controller
    status: completed
  - id: 4-statemanager-mission
    content: Implement State Manager Mission Creation (Multi-Order support)
    status: completed
  - id: 5-optimizer-models
    content: Update Optimizer Models (Snapshot & Decision)
    status: completed
  - id: 6-optimizer-builder
    content: Implement VRP Problem Builder (Multi-Warehouse, Time Windows)
    status: completed
  - id: 7-optimizer-solver
    content: Implement VRP Solver (Battery & Time Dimensions)
    status: completed
  - id: 8-optimizer-extractor
    content: Implement Solution Extractor (Route generation)
    status: completed
  - id: 9-verification
    content: Verify Integration & Run E2E Test
    status: completed
isProject: false
---

# Implementation Plan: Multi-Trip VRPTW with Battery & Time Windows

This plan details the transformation of the DroneFleet Optimizer from a simple 1-to-1 assignment system to a complex Vehicle Routing Problem (VRP) solver with multiple constraints.

## Architecture & Data Flow

```mermaid
sequenceDiagram
    participant Scheduler
    participant Optimizer
    participant StateManager
    participant Firestore

    Scheduler->>Optimizer: Trigger Job (every 10s)
    Optimizer->>StateManager: GET /api/v1/optimizer/snapshot
    StateManager->>Firestore: Read State (No Locking)
    StateManager-->>Optimizer: Return OptimizationSnapshot (Drones, Orders, Warehouses, Depot)
    Optimizer->>Optimizer: Build VRP & Solve (OR-Tools)
    Optimizer->>Optimizer: Extract MissionAssignments
    Optimizer->>StateManager: POST /api/v1/missions/batch
    StateManager->>Firestore: Validate & Create Missions (Optimistic Locking)
```



## Phase 1: Shared Models Update (Java & Python)

Update shared schemas to support new VRP requirements.

### 1.1 Java Shared Models (`shared/java/.../models/`)

- **[Drone.java](shared/java/src/main/java/com/dronefleet/shared/models/Drone.java)**: Add `batteryCapacityMah`, `consumptionPerKm`, `maxFlightTimeMinutes`. Add `canCompleteRoute()`.
- **[Order.java](shared/java/src/main/java/com/dronefleet/shared/models/Order.java)**: Add `productType`, `maxDeliveryTimeMinutes`, `priority` (Enum). Add `getDeliveryDeadline()`.
- **[Warehouse.java](shared/java/src/main/java/com/dronefleet/shared/models/Warehouse.java)**: Add `canFulfillOrder()` method.
- **[Mission.java](shared/java/src/main/java/com/dronefleet/shared/models/Mission.java)**: Refactor `route` to `List<Waypoint>` and support `orderIds` (List).
- **[Depot.java](shared/java/src/main/java/com/dronefleet/shared/models/Depot.java)**: Create new class for Home Depot entity.
- **[OptimizationSnapshot.java](shared/java/src/main/java/com/dronefleet/shared/models/OptimizationSnapshot.java)**: Add `Depot` and `warehouses` list.

### 1.2 Python Shared Models (`shared/python/.../models/`)

- **[drone.py](shared/python/src/dronefleet_shared/models/drone.py)**: Ensure alignment with Java model.
- **[protocol.py](shared/python/src/dronefleet_shared/models/protocol.py)**: Add `WaypointType` enum.

## Phase 2: State Manager Refactoring (Java)

Implement logic for snapshot retrieval and safe mission assignment.

### 2.1 Domain & Ports (`services/state_manager/...`)

- **[OptimizationSnapshotService.java](services/state_manager/src/main/java/com/dronefleet/statemanager/domain/service/OptimizationSnapshotService.java)**:
  - Implement `GetOptimizationSnapshotUseCase`.
  - Retrieve IDLE drones, PENDING orders, Warehouses, and Depot.
  - **Crucial**: Do NOT change status to RESERVED.
- **[MissionCreationService.java](services/state_manager/src/main/java/com/dronefleet/statemanager/domain/service/MissionCreationService.java)**:
  - Validate Drone is still IDLE.
  - Validate Orders are still PENDING.
  - Create Mission with multi-waypoints.
  - Use `StateTransactionPort` for atomic updates.

### 2.2 Infrastructure

- **[OptimizerController.java](services/state_manager/src/main/java/com/dronefleet/statemanager/infrastructure/adapter/in/rest/OptimizerController.java)**: Create endpoint for snapshot retrieval.
- **[FirestoreRepositories](services/state_manager/src/main/java/com/dronefleet/statemanager/infrastructure/adapter/out/persistence/firestore/)**: Implement `DepotRepository`.

## Phase 3: Optimizer Implementation (Python)

Implement the VRP solver with OR-Tools.

### 3.1 Models (`services/path_optimizer/.../models/`)

- **[snapshot.py](services/path_optimizer/src/path_optimizer/models/snapshot.py)**: Update `DroneSnapshot`, `OrderSnapshot` (add deadline), `OptimizationSnapshot`.
- **[decision.py](services/path_optimizer/src/path_optimizer/models/decision.py)**: Update `MissionAssignment` to support `list[Waypoint]` and `list[order_id]`.

### 3.2 Core Logic (`services/path_optimizer/.../services/`)

- **[builder.py](services/path_optimizer/src/path_optimizer/services/builder.py)**:
  - Build `VRPProblem`.
  - Map multiple warehouses as pickup options for each order.
  - Calculate Time Windows based on Order priority.
- **[solver.py](services/path_optimizer/src/path_optimizer/services/solver.py)**:
  - Add **Time Dimension** (soft/hard windows).
  - Add **Battery Dimension** (consumption per distance, min return charge).
  - Configure `PARALLEL_CHEAPEST_INSERTION` and `GUIDED_LOCAL_SEARCH`.
- **[extractor.py](services/path_optimizer/src/path_optimizer/services/extractor.py)**:
  - Parse OR-Tools solution.
  - Construct `MissionAssignment` with explicit waypoints (Start -> Pickup -> Delivery -> ... -> End).

## Phase 4: Integration & Verification

- **API Integration**: Ensure Optimizer calls State Manager API correctly.
- **End-to-End Test**:
  1. Lunch the docker-compose
  2. Inject Orders and some Drones via Simulator.
  3. Trigger Optimizer.
  4. Verify Mission created in Firestore has correct multi-step route.



## Phase 5: Clear Explaination Step-by-Step

- Create a new Markdown file complete_flux.md in .cursor/rules.personal/ folder
- Explain inside all the logical step that append here with the optimizer on every file, from the lunch of the optimizer to write of the mission on Firestore. the goal is to be a claer and very detailed documention that explain all the code logic of the optimizer and the write of the mission by the state manager to Firestore, by assuming Telemetry and Orders Data are fetched by the state manager to FireStore with the 2 topics Telemetry topic and Orders topic.
