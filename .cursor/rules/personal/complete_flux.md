# Optimizer and Mission Flux Documentation

This document describes the complete flow of the DroneFleet Optimizer system, from data ingestion to mission creation in Firestore.

## 1. Data Ingestion (Telemetry & Orders)

The system relies on two main data streams:
- **Telemetry**: Drones in flight publish their position, battery, and status to the `telemetry` Pub/Sub topic.
- **Orders**: The Ingestion API receives delivery requests and publishes them to the `orders` Pub/Sub topic.

The **State Manager** listens to these topics and atomically updates the **Firestore** database:
- Drones are updated with their latest position and battery level.
- Orders are stored with a `PENDING` status.

## 2. Optimization Trigger

Every 10 seconds (managed by a Cloud Scheduler or a local loop), the **Optimizer Engine** starts a new optimization cycle.

### 2.1 Session Initialization
A unique `session_id` is generated for tracking.

### 2.2 Snapshot Retrieval
The Optimizer calls the State Manager endpoint `GET /api/v1/optimizer/snapshot`.
The State Manager performs a read-only query (Optimistic Locking) to fetch:
- `IDLE` Drones with sufficient battery (>20%).
- `PENDING` Orders.
- The `Depot` location.
- All `Warehouses` with their authorized product types.

This data is returned to the Optimizer as an `OptimizationSnapshot`.

## 3. Solving the VRP

The Optimizer uses **Google OR-Tools** to solve the Multi-Trip VRPTW (Vehicle Routing Problem with Time Windows).

### 3.1 Problem Building (`builder.py`)
- Nodes are created for the Depot, all Warehouses, and all Order delivery locations.
- A **Distance Matrix** and a **Time Matrix** are calculated using the Haversine formula.
- Each Order is mapped to multiple compatible Warehouse nodes based on `product_type`.
- **Time Windows** are defined for each delivery node based on the order priority (CRITICAL: 15min, HIGH: 30min, STANDARD: 60min).

### 3.2 Solving (`solver.py`)
OR-Tools explores potential routes using:
- **Distance Dimension**: Minimizes total distance.
- **Time Dimension**: Ensures all deliveries meet their deadlines.
- **Battery Dimension**: Tracks cumulative battery consumption. Drones must return to the depot with at least 20% battery.
- **Pickup & Delivery Constraints**: Ensures an order is picked up from a compatible warehouse before being delivered to the hospital, by the same drone.

### 3.3 Solution Extraction (`extractor.py`)
The solver's output is parsed into a list of `MissionAssignment` objects. Each assignment contains:
- `drone_id`
- `order_ids` (multiple orders per mission)
- `route` (a sequence of `Waypoint` objects: Start -> Pickup -> Delivery -> ... -> End)
- Estimated metrics (battery consumption, duration).

## 4. Mission Assignment

### 4.1 Publishing Decisions
The Optimizer publishes each `MissionAssignment` to the `decisions` Pub/Sub topic.

### 4.2 Applying Decisions (`DecisionListener.java`)
The State Manager's `DecisionListener` receives the message and calls `MissionCreationService`.

### 4.3 Atomic Validation & Creation (`MissionAssignmentPolicy.java`)
Within a **Firestore Transaction**, the State Manager validates that:
1. The Drone is still `IDLE`.
2. All Orders in the mission are still `PENDING`.

If valid:
- A new **Mission** document is created.
- The Drone status is set to `MOVING` and linked to the mission.
- The Orders status is set to `ASSIGNED` and linked to the drone/mission.

If any resource is no longer available (due to a race condition or update between the snapshot and the decision), the transaction fails and the assignment is rejected (Optimistic Locking).

## 5. Execution

The mission is now in Firestore. The drone (or its simulator) will pick up the mission and start moving through the defined waypoints.
