# Data Flow

## Telemetry and Order Ingestion Flow

```
┌─────────────┐
│  Simulator  │ (Generates drone telemetry + delivery orders)
└──────┬──────┘
       │ HTTP POST
       v
┌─────────────────┐
│ Ingestion API   │ (FastAPI - Validation with Pydantic)
│  Cloud Run      │
└────────┬────────┘
         │ Pub/Sub Publish
         ├──────────────────┬──────────────────┐
         v                  v                  v
   [telemetry]        [orders]           [decisions]
         │                  │                  │
         │                  │                  │
         v                  v                  v
┌────────────────────────────────────────────────┐
│         State Manager (Java/Spring Boot)       │
│  - TelemetryListener   - OrderListener         │
│  - DecisionListener    - REST API              │
└───────────────────┬────────────────────────────┘
                    │ Firestore Transactions
                    v
            ┌───────────────┐
            │  Firestore DB │
            │  Collections: │
            │  - drones     │
            │  - orders     │
            │  - missions   │
            │  - warehouses │
            └───────────────┘
```

### Telemetry Flow Details

1. **Simulator** generates drone position, battery level, status
2. **Ingestion API** validates payload against Pydantic schema
3. **Pub/Sub** delivers message to `telemetry` topic
4. **State Manager** `TelemetryListener` consumes message
5. **Firestore Transaction** updates drone document with:
   - Timestamp ordering (reject out-of-order messages)
   - Position update (GeoPoint)
   - Battery level update
   - Status transition validation

### Order Flow Details

1. **Simulator** generates delivery request (product type, priority, location)
2. **Ingestion API** validates order payload
3. **Pub/Sub** delivers message to `orders` topic
4. **State Manager** `OrderListener` consumes message
5. **Firestore Transaction** creates/updates order document with:
   - Idempotency guard (prevents overwriting processed orders)
   - Status: `PENDING`
   - Priority-based deadline calculation

## Optimization Cycle Flow

The global logical cycle of the optimization part of the system is represented by :

![Optimization Logical Cycle](images/optimization-cycle.png){ width="2000px" }

With more details, the complete flow :
```
┌──────────────────┐
│ Cloud Scheduler  │ (Triggers every 10 seconds)
└────────┬─────────┘
         │ HTTP POST (invoke Cloud Run Job)
         v
┌─────────────────────────────────────────────────┐
│          Path Optimizer (Python/OR-Tools)       │
│                                                 │
│  1. GET /api/v1/optimizer/snapshot              │
│     ├─ Fetch IDLE drones (battery > 20%)       │
│     ├─ Fetch PENDING orders                    │
│     ├─ Fetch warehouses + depot                │
│     └─ Return OptimizationSnapshot             │
│                                                 │
│  2. Build VRP Model (builder.py)                │
│     ├─ Create node graph (depot, pickups,      │
│     │  deliveries)                              │
│     ├─ Compute distance/time matrices          │
│     │  (Haversine)                              │
│     ├─ Map orders to compatible warehouses     │
│     └─ Define time windows per priority        │
│                                                 │
│  3. Solve VRP (solver.py)                       │
│     ├─ Distance dimension (minimize travel)    │
│     ├─ Time dimension (respect deadlines)      │
│     ├─ Battery dimension (consumption model)   │
│     ├─ Pickup-delivery constraints             │
│     └─ Disjunctions (allow dropping infeasible │
│        orders)                                  │
│                                                 │
│  4. Extract Solution (extractor.py)             │
│     ├─ Walk each vehicle route                 │
│     ├─ Classify waypoints (START, PICKUP,      │
│     │  DELIVERY, RETURN)                        │
│     ├─ Compute metrics (battery, duration)     │
│     └─ Build MissionAssignment messages        │
│                                                 │
│  5. Publish Decisions                           │
│     └─ Pub/Sub topic: decisions                │
└─────────────────────────────────────────────────┘
         │
         v
   [decisions] Pub/Sub Topic
         │
         v
┌─────────────────────────────────────────────────┐
│         State Manager - DecisionListener        │
│                                                 │
│  BEGIN Firestore Transaction:                   │
│    1. Read drone (verify IDLE)                  │
│    2. Read all orders (verify PENDING)          │
│    3. Validate via MissionAssignmentPolicy      │
│    4. Create Mission document                   │
│    5. Update drone.status = MOVING              │
│    6. Update orders.status = ASSIGNED           │
│  COMMIT (atomic, all-or-nothing)                │
│                                                 │
│  If validation fails (race condition):          │
│    - Transaction aborted                        │
│    - BusinessRejectionException thrown          │
│    - Affected entities picked up in next cycle  │
└─────────────────────────────────────────────────┘
```
