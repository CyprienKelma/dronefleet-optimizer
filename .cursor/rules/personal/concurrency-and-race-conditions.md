# Concurrency and Race Condition Management

> **Technical Documentation** - DroneFleet Optimizer
>
> This document explains how the system handles concurrent operations, prevents race conditions, and ensures data consistency across distributed components.

## Table of Contents

1. [Overview](#1-overview)
2. [System Architecture for Concurrency](#2-system-architecture-for-concurrency)
3. [Data Flow and State Transitions](#3-data-flow-and-state-transitions)
4. [Firestore Transaction Patterns](#4-firestore-transaction-patterns)
5. [Race Condition Analysis](#5-race-condition-analysis)
6. [Protection Mechanisms](#6-protection-mechanisms)
7. [Edge Cases and Scenarios](#7-edge-cases-and-scenarios)
8. [Current Limitations and Gaps](#8-current-limitations-and-gaps)
9. [Design Decisions and Trade-offs](#9-design-decisions-and-trade-offs)

---

## 1. Overview

### The Concurrency Challenge

DroneFleet Optimizer operates as a distributed system where multiple components interact asynchronously:

- **Path Optimizer** (Python): Runs optimization cycles every ~10 seconds
- **State Manager** (Java/Spring Boot): Processes telemetry, orders, and decisions concurrently
- **Pub/Sub**: Delivers messages asynchronously with no ordering guarantees
- **Firestore**: Serves as the source of truth for drone and order states

The primary concurrency challenges are:

| Challenge | Description |
|-----------|-------------|
| **Concurrent Optimization Cycles** | Two optimization cycles may include the same drone/order |
| **Telemetry Ordering** | Messages may arrive out-of-order due to network conditions |
| **Decision Conflicts** | Multiple decisions may target the same drone simultaneously |
| **State Consistency** | Snapshot data may become stale during optimization |

### Consistency Model

The system uses **eventual consistency** with **strong consistency at write time**:

- Reads (snapshots) may see slightly stale data
- Writes (mission assignments) are validated and executed atomically
- Rejected operations are not retried; affected entities are picked up in the next cycle

---

## 2. System Architecture for Concurrency

### Component Interaction

```
                                   OPTIMIZATION CYCLE
                                   ==================

  +------------------+     HTTP GET /snapshot      +------------------+
  |                  | --------------------------> |                  |
  |  Path Optimizer  |                             |  State Manager   |
  |     (Python)     |     Pub/Sub decisions       |   (Java/Spring)  |
  |                  | --------------------------> |                  |
  +------------------+                             +------------------+
                                                           |
                                                           | Firestore
                                                           | Transaction
                                                           v
                                                   +------------------+
                                                   |                  |
                                                   |    Firestore     |
                                                   |   (Source of     |
                                                   |     Truth)       |
                                                   |                  |
                                                   +------------------+
```

### Concurrency Points

1. **Snapshot Acquisition** (`OptimizationSnapshotService`)
   - Reads IDLE drones and PENDING orders
   - Non-transactional (separate queries)

2. **Decision Processing** (`DecisionListener` -> `MissionCreationService`)
   - Validates and applies decisions
   - Fully transactional (Firestore atomic transaction)

3. **Telemetry Ingestion** (`TelemetryListener`)
   - Updates drone state (position, battery)
   - Transactional with timestamp ordering

4. **Order Ingestion** (`OrderListener`)
   - Creates or updates orders
   - Transactional with idempotency guard

---

## 3. Data Flow and State Transitions

### Drone State Machine

```
                    +-----------+
                    |   IDLE    |<-----------------+
                    +-----+-----+                  |
                          |                        |
        [Snapshot fetch]  |                        |
                          v                        |
                  +---------------+                |
                  |   RESERVED    |  (Not used)    |
                  +-------+-------+                |
                          |                        |
        [Decision valid]  |                        |
                          v                        |
                    +-----------+                  |
                    |  MOVING   |------------------+
                    +-----------+   [Mission complete]


        Note: RESERVED status exists in proto but is NOT currently used.
              Drones remain IDLE until decision is processed.
```

### Order State Machine

```
                    +-----------+
                    |  PENDING  |<-----------------+
                    +-----+-----+                  |
                          |                        |
        [Snapshot fetch]  |                        |
                          v                        |
                  +---------------+                |
                  |   SOLVING     |  (Not used)    |
                  +-------+-------+                |
                          |                        |
        [Decision valid]  |                        |
                          v                        |
                    +-----------+                  |
                    | ASSIGNED  |------------------+
                    +-----------+   [Delivery complete
                                     or rejection]
```

### Timeline of a Successful Assignment

```
T0  [Optimizer]     GET /api/v1/optimizer/snapshot?sessionId=abc-123
T1  [State Manager] Query drones WHERE status=IDLE AND battery>=20%
T2  [State Manager] Query orders WHERE status=PENDING
T3  [State Manager] Return snapshot (drones: [D1,D2], orders: [O1,O2,O3])
T4  [Optimizer]     Build VRP model and solve (~8 seconds)
T5  [Optimizer]     Publish decision: D1 -> [O1, O2]
T6  [State Manager] DecisionListener receives message
T7  [State Manager] BEGIN Firestore Transaction
T8  [State Manager]   - Read D1 (verify IDLE)
T9  [State Manager]   - Read O1, O2 (verify PENDING)
T10 [State Manager]   - Validate via MissionAssignmentPolicy
T11 [State Manager]   - Write Mission document
T12 [State Manager]   - Update D1.status = MOVING
T13 [State Manager]   - Update O1.status = ASSIGNED
T14 [State Manager]   - Update O2.status = ASSIGNED
T15 [State Manager] COMMIT Transaction (atomic)
T16 [State Manager] ACK Pub/Sub message
```

---

## 4. Firestore Transaction Patterns

### Overview of Transaction Types

The system uses four distinct transaction patterns, implemented in `FirestoreStateTransactionAdapter`:

| Transaction | Purpose | Atomicity |
|-------------|---------|-----------|
| `runMissionAssignmentTransaction` | Assign drone to orders | Full (multi-document) |
| `runTelemetryUpdateTransaction` | Update drone telemetry | Full (single document) |
| `runOrderIngestionTransaction` | Create/update orders | Full (single document) |
| `runSnapshotAcquisitionTransaction` | Reserved for future | Not implemented |

### Mission Assignment Transaction (Critical Path)

This is the most complex transaction, ensuring that mission assignments are atomic and consistent.

```java
// Simplified flow in FirestoreStateTransactionAdapter.runMissionAssignmentTransaction()

firestore.runTransaction(transaction -> {
    // PHASE 1: Read all documents FIRST (Firestore requirement)
    DocumentSnapshot droneDoc = transaction.get(droneRef).get();
    List<DocumentSnapshot> orderDocs = /* read all orders */;

    // PHASE 2: Convert to domain objects
    Drone drone = FirestoreMapper.toDrone(droneDoc);
    List<Order> orders = /* convert all orders */;

    // PHASE 3: Execute business logic (MissionAssignmentPolicy)
    // This validates:
    //   - drone.status == IDLE
    //   - all orders.status == PENDING
    MissionAssignmentResult result = assignmentLogic.apply(
        new DroneOrderContext(drone, orders)
    );

    // PHASE 4: Write all changes
    transaction.set(missionRef, missionData);
    transaction.update(droneRef, droneUpdates);
    for (Order order : orders) {
        transaction.update(orderRef, orderUpdates);
    }

    return result;
});
```

**Key Properties:**
- All reads happen before any writes (Firestore requirement)
- If any document was modified by another transaction between read and commit, Firestore automatically retries
- Either all writes succeed, or none do (atomicity)

### Telemetry Update Transaction

Handles out-of-order telemetry by comparing timestamps:

```java
firestore.runTransaction(transaction -> {
    DocumentSnapshot doc = transaction.get(droneRef).get();

    if (doc.exists()) {
        Instant existingTimestamp = /* get lastUpdate from doc */;
        Instant incomingTimestamp = telemetry.getTimestamp();

        // STALE TELEMETRY CHECK
        if (incomingTimestamp.isBefore(existingTimestamp)) {
            log.debug("Skipping stale telemetry for drone {}", droneId);
            return null;  // Skip - do not apply older data
        }
    }

    // Apply update (upsert)
    Drone updated = DronePolicy.applyTelemetryUpdate(drone, telemetry);
    transaction.set(droneRef, FirestoreMapper.toMap(updated));
    return updated;
});
```

### Order Ingestion Transaction

Prevents resetting orders that have already been processed:

```java
firestore.runTransaction(transaction -> {
    DocumentSnapshot doc = transaction.get(orderRef).get();

    if (doc.exists()) {
        OrderStatus currentStatus = /* get status from doc */;

        // IDEMPOTENCY GUARD
        if (currentStatus != PENDING && currentStatus != UNSPECIFIED) {
            log.debug("Skipping order {} - already processed (status={})",
                      orderId, currentStatus);
            return null;  // Do not overwrite processed orders
        }
    }

    // Create or update order with PENDING status
    Order order = /* build order with PENDING status */;
    transaction.set(orderRef, FirestoreMapper.toMap(order));
    return order;
});
```

---

## 5. Race Condition Analysis

### Scenario 1: Two Optimization Cycles Compete for Same Drone

This is the primary race condition scenario in the system.

```
Timeline:
=========

T0: Cycle A starts, calls getSnapshot()
    -> Drone D1 is IDLE -> included in snapshot A

T1: Cycle B starts, calls getSnapshot()
    -> Drone D1 is STILL IDLE -> included in snapshot B
    (A hasn't finished yet, so D1 status unchanged)

T2: Cycle A computes solution
    -> Assigns D1 to Order O1

T3: Cycle B computes solution
    -> Assigns D1 to Order O2

T4: Cycle A publishes decision (D1 -> O1)

T5: State Manager processes A's decision
    -> Transaction: D1 is IDLE? YES
    -> SUCCESS: D1.status = MOVING, Mission M1 created

T6: Cycle B publishes decision (D1 -> O2)

T7: State Manager processes B's decision
    -> Transaction: D1 is IDLE? NO (it's MOVING)
    -> REJECTED: BusinessRejectionException thrown
    -> Order O2 remains PENDING for next cycle
```

**Result:** System correctly prevents double-assignment through write-time validation.

### Scenario 2: Slow Optimizer Cycle Publishes After Fast Cycle

A variation where the order of completion differs from order of start:

```
Timeline:
=========

T0: Cycle A starts (has many orders, will be slow)
T1: Cycle B starts (has few orders, will be fast)
T2: Cycle B completes and publishes (D1 -> O2)
T3: State Manager processes B -> SUCCESS, D1 = MOVING
T4: Cycle A completes and publishes (D1 -> O1)
T5: State Manager processes A -> REJECTED (D1 is MOVING)
```

**Result:** "First-write-wins", not "first-start-wins". This is by design (see Trade-offs section).

### Scenario 3: Out-of-Order Telemetry

Network conditions can cause telemetry messages to arrive out of order:

```
Timeline:
=========

T0: Drone sends telemetry T1 (battery=80%, position=A)
T1: Drone sends telemetry T2 (battery=79%, position=B)
T2: Network delay causes T2 to arrive at State Manager
T3: State Manager processes T2 -> drone at position B, battery 79%
T4: T1 finally arrives (older data)
T5: State Manager checks timestamp: T1 < T2
    -> SKIPPED: Stale telemetry not applied
```

**Result:** Timestamp comparison prevents regression to older state.

### Scenario 4: Duplicate Order Ingestion

An order message might be delivered multiple times (Pub/Sub at-least-once):

```
Timeline:
=========

T0: Order O1 created (status=PENDING)
T1: Order O1 assigned to drone (status=ASSIGNED)
T2: Pub/Sub redelivers original O1 creation message
T3: State Manager checks: O1.status == ASSIGNED
    -> SKIPPED: Cannot reset processed order
```

**Result:** Idempotency guard prevents overwriting processed orders.

---

## 6. Protection Mechanisms

### Summary Table

| Mechanism | Location | Protection Provided |
|-----------|----------|---------------------|
| Firestore Transaction | `FirestoreStateTransactionAdapter` | Atomic multi-document writes |
| Optimistic Concurrency | Firestore built-in | Automatic retry on contention |
| Write-time Validation | `MissionAssignmentPolicy` | Status checks before assignment |
| Drone Status Guard | `DronePolicy.canAcceptMission()` | Only IDLE drones can accept missions |
| Order Status Guard | `MissionAssignmentPolicy` | Only PENDING orders can be assigned |
| Timestamp Ordering | `runTelemetryUpdateTransaction` | Reject stale telemetry |
| Idempotency Guard | `runOrderIngestionTransaction` | Prevent resetting processed orders |

### Validation Logic in MissionAssignmentPolicy

```java
// MissionAssignmentPolicy.computeAssignment()

public MissionAssignmentResult computeAssignment(
        Drone drone,
        List<Order> orders,
        MissionAssignmentDto dto) {

    // GUARD 1: Drone must be available for missions
    if (!dronePolicy.canAcceptMission(drone.getStatus())) {
        throw new BusinessRejectionException(
            "Drone " + drone.getDroneId() + " cannot accept mission. " +
            "Current status: " + drone.getStatus()
        );
    }

    // GUARD 2: All orders must be pending
    for (Order order : orders) {
        if (order.getStatus() != OrderStatus.ORDER_STATUS_PENDING) {
            throw new BusinessRejectionException(
                "Order " + order.getOrderId() + " is not pending. " +
                "Current status: " + order.getStatus()
            );
        }
    }

    // ... build mission and return result
}
```

### DronePolicy Status Validation

```java
// DronePolicy.java

public boolean canAcceptMission(DroneStatus status) {
    return status == DroneStatus.DRONE_STATUS_IDLE;
}

public boolean isAvailable(Drone drone) {
    return drone.getStatus() == DroneStatus.DRONE_STATUS_IDLE
        && drone.getBatteryPercentage() > 20.0;
}
```

---

## 7. Edge Cases and Scenarios

### Multi-Order Missions and Capacity

The optimizer can assign multiple orders to a single drone in one mission. This is correct behavior:

```
Mission for DRONE-012:
  Route: DEPOT_START
      -> PICKUP (WH-SOUTHWEST)    [capacity: 0 -> 1]
      -> DELIVERY (Hospital A)    [capacity: 1 -> 0]
      -> PICKUP (WH-SOUTHWEST)    [capacity: 0 -> 1]
      -> DELIVERY (Hospital B)    [capacity: 1 -> 0]
      -> PICKUP (WH-SOUTH)        [capacity: 0 -> 1]
      -> DELIVERY (Hospital C)    [capacity: 1 -> 0]
      -> DEPOT_RETURN
```

**Constraint:** Capacity = 1 means "one package at a time", not "one order per mission". The drone picks up, delivers, then can pick up again.

**Race Protection:** All orders in the mission are validated and updated atomically within a single Firestore transaction.

### Partial Mission Failure

If validation fails for any order in a multi-order mission, the entire transaction is rejected:

```
Scenario:
  Decision: D1 -> [O1, O2, O3]
  At write time: O2 was already assigned by another cycle

Result:
  - Transaction fails completely
  - D1 remains IDLE
  - O1, O3 remain PENDING
  - All three orders available for next cycle
```

### Firestore Transaction Retry

Firestore automatically retries transactions when contention is detected:

```
Scenario:
  Two concurrent transactions both read D1 as IDLE
  Both attempt to write D1.status = MOVING

Firestore behavior:
  1. Transaction A reads D1, Transaction B reads D1
  2. Transaction A commits first -> SUCCESS
  3. Transaction B attempts commit
  4. Firestore detects D1 was modified since B's read
  5. Firestore RETRIES Transaction B from the beginning
  6. Transaction B re-reads D1 (now MOVING)
  7. Validation fails -> BusinessRejectionException
```

---

## 8. Current Limitations and Gaps

### Gap 1: Non-Transactional Snapshot Acquisition

**Current Implementation:**
```java
// OptimizationSnapshotService.getSnapshot()
List<Drone> drones = droneRepository.findAvailableForOptimization(minBattery);
List<Order> orders = orderRepository.findPending();
// These are SEPARATE queries, not transactional
```

**Risk:** State may change between the two queries. A drone could become unavailable, or an order could be assigned, between the drone query and the order query.

**Mitigation:** This is acceptable because validation happens at write time. Stale data in the snapshot leads to rejected decisions, not incorrect assignments.

**Future Improvement:** Implement `runSnapshotAcquisitionTransaction()` to atomically read drones and orders within a single transaction.

### Gap 2: Unused Session ID Tracking

**Current State:**
- `solvingSessionId` field exists in proto definitions for Drone and Order
- Field is persisted to Firestore
- Field is NEVER set or validated

**Intended Purpose:**
```
1. Optimizer generates session_id (e.g., "opt-abc-123")
2. Snapshot acquisition marks entities:
   - drone.solvingSessionId = "opt-abc-123"
   - drone.status = RESERVED
   - order.solvingSessionId = "opt-abc-123"
   - order.status = SOLVING
3. Next snapshot EXCLUDES entities with non-null solvingSessionId
4. Decision validation CHECKS that session_id matches
5. After timeout, entities are released if session abandoned
```

**Current Risk:** Without session tracking:
- Multiple cycles may compute solutions for the same entities
- Results in wasted computation and rejected decisions

### Gap 3: Unused RESERVED and SOLVING States

**Defined in Proto:**
```protobuf
enum DroneStatus {
  DRONE_STATUS_UNSPECIFIED = 0;
  DRONE_STATUS_IDLE = 1;
  DRONE_STATUS_MOVING = 2;
  DRONE_STATUS_RESERVED = 3;  // <-- NOT USED
  // ...
}

enum OrderStatus {
  ORDER_STATUS_UNSPECIFIED = 0;
  ORDER_STATUS_PENDING = 1;
  ORDER_STATUS_SOLVING = 2;    // <-- NOT USED
  ORDER_STATUS_ASSIGNED = 3;
  // ...
}
```

**Purpose:** These states would provide "soft locks" during optimization, preventing other cycles from including the same entities.

### Gap 4: No Cleanup for Abandoned Sessions

If an optimizer crashes mid-cycle, any entities marked with its `solvingSessionId` would be stuck. A cleanup mechanism (TTL-based or heartbeat-based) is needed.

---

## 9. Design Decisions and Trade-offs

### First-Write-Wins vs First-Start-Wins

**Current Approach:** First-Write-Wins
- The first transaction to commit wins
- Does not matter which optimization cycle started first

**Alternative:** First-Start-Wins (with pessimistic locking)
- Mark entities as RESERVED/SOLVING at snapshot time
- Reject decisions from sessions that don't match

**Trade-off Analysis:**

| Aspect | First-Write-Wins | First-Start-Wins |
|--------|------------------|------------------|
| Complexity | Simple | Requires distributed locking |
| Wasted computation | Possible (rejected decisions) | Minimal |
| Deadlock risk | None | Possible if session dies |
| Implementation | Current state | Would require session cleanup |

**Rationale:** First-Write-Wins was chosen for simplicity. Given that:
- Optimization cycles run every ~10 seconds
- Solve time is ~8 seconds
- Cycles rarely overlap significantly

The amount of wasted computation is acceptable for the reduced complexity.

### Strong vs Eventual Consistency

**Current Approach:** Eventual consistency for reads, strong consistency for writes

**Read Path (Snapshot):**
- May see slightly stale data
- Fast, no coordination required

**Write Path (Decision):**
- Strong consistency via Firestore transactions
- Validation ensures correctness at write time

**Rationale:** The system tolerates stale reads because:
1. Orders not assigned in one cycle will be assigned in the next
2. No data corruption is possible (writes are always validated)
3. Performance is prioritized for the high-throughput telemetry path

### At-Least-Once vs Exactly-Once Delivery

**Current Approach:** At-least-once delivery with idempotent handlers

**Pub/Sub Guarantee:** At-least-once (messages may be redelivered)

**Idempotency Mechanisms:**
- Order ingestion: Status guard prevents resetting processed orders
- Telemetry: Timestamp comparison prevents applying older data
- Decisions: Status validation prevents double-assignment

**Rationale:** Exactly-once delivery is complex and expensive. Idempotent handlers provide equivalent correctness guarantees with simpler implementation.

---

## Appendix: Key File Locations

| Component | Path |
|-----------|------|
| Transaction Adapter | `services/state_manager/src/main/java/.../infrastructure/adapter/out/persistence/firestore/FirestoreStateTransactionAdapter.java` |
| Mission Assignment Policy | `services/state_manager/src/main/java/.../domain/service/MissionAssignmentPolicy.java` |
| Decision Listener | `services/state_manager/src/main/java/.../infrastructure/adapter/in/messaging/pubsub/DecisionListener.java` |
| Optimization Snapshot Service | `services/state_manager/src/main/java/.../domain/service/OptimizationSnapshotService.java` |
| Drone Policy | `services/state_manager/src/main/java/.../domain/service/DronePolicy.java` |
| Business Rejection Exception | `services/state_manager/src/main/java/.../domain/exception/BusinessRejectionException.java` |
| State Transaction Port (Interface) | `services/state_manager/src/main/java/.../domain/port/out/StateTransactionPort.java` |
| Path Optimizer Main | `services/path_optimizer/src/path_optimizer/main.py` |
| State Manager Client | `services/path_optimizer/src/path_optimizer/clients/state_manager.py` |

---

## Summary

The DroneFleet Optimizer handles concurrency through:

1. **Firestore Transactions** - Atomic multi-document writes for mission assignment
2. **Write-Time Validation** - Status checks happen at commit time, not at snapshot time
3. **Timestamp Ordering** - Prevents out-of-order telemetry regression
4. **Idempotency Guards** - Safe handling of message redelivery
5. **First-Write-Wins** - Simple conflict resolution without distributed locks

**Correctness Guarantee:** No race condition can cause incorrect data (double-assignment, lost orders).

**Efficiency Trade-off:** Concurrent optimization cycles may waste computation, but this is acceptable given the system's operational parameters.
