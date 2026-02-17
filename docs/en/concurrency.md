# Concurrency and Race Condition Management

Operating as a distributed system with asynchronous messaging, DroneFleet Optimizer faces inherent concurrency challenges. Multiple components interact simultaneously — the Path Optimizer runs optimization cycles every ~10 seconds, the State Manager processes telemetry, orders, and decisions concurrently, Pub/Sub delivers messages with no ordering guarantees, and Firestore serves as the single source of truth. The system uses **eventual consistency for reads** combined with **strong consistency at write time** to ensure correctness without sacrificing performance.

## The Core Challenge: Concurrent Optimization Cycles

The primary race condition arises when two optimization cycles overlap and both include the same drone or order in their snapshots:

```
Timeline:
=========

T0: Cycle A starts, calls getSnapshot()
    -> Drone D1 is IDLE -> included in snapshot A

T1: Cycle B starts, calls getSnapshot()
    -> Drone D1 is STILL IDLE -> included in snapshot B
    (A hasn't finished yet, so D1 status unchanged)

T2: Cycle A computes solution -> Assigns D1 to Order O1
T3: Cycle B computes solution -> Assigns D1 to Order O2

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

The system correctly prevents double-assignment through **write-time validation** inside a Firestore transaction.

## First-Write-Wins Strategy

The conflict resolution model is **first-write-wins**, not first-start-wins. The first transaction to commit wins, regardless of which optimization cycle started first. This is a deliberate design choice:

- **Simpler implementation**: No distributed lock management, no session-based reservation system.
- **No deadlock risk**: Since there is no pessimistic locking, there is no possibility of deadlock.
- **Acceptable waste**: Given that optimization cycles run every ~10 seconds and the solver takes ~8 seconds, overlapping cycles are infrequent. The occasional rejected decision is recovered naturally in the next cycle.

An alternative approach (first-start-wins with pessimistic locking via `RESERVED`/`SOLVING` states) was considered. While it would reduce wasted computation, it introduces significant complexity: distributed lock management, session cleanup for crashed optimizers, and potential deadlocks.

## Firestore Transaction Pattern: Mission Assignment (Critical Path)

The mission assignment is the most complex transaction in the system. It validates and applies decisions atomically across multiple documents:

```java
firestore.runTransaction(transaction -> {
    // Read all documents FIRST (Firestore requirement)
    DocumentSnapshot droneDoc = transaction.get(droneRef).get();
    List<DocumentSnapshot> orderDocs = /* read all orders */;

    // Convert to domain objects
    Drone drone = FirestoreMapper.toDrone(droneDoc);
    List<Order> orders = /* convert all orders */;

    // Execute business logic (MissionAssignmentPolicy)
    //   - drone.status == IDLE (DronePolicy.canAcceptMission)
    //   - all orders.status == PENDING
    //   - If ANY validation fails -> BusinessRejectionException

    // Write all changes atomically
    transaction.set(missionRef, missionData);       // Create Mission
    transaction.update(droneRef, droneUpdates);      // drone.status = MOVING
    for (Order order : orders) {
        transaction.update(orderRef, orderUpdates);  // order.status = ASSIGNED
    }
    return result;
});
```

**Key properties:**
- All reads happen before any writes (Firestore requirement for optimistic concurrency)
- If any document was modified by another transaction between read and commit, Firestore automatically retries the entire transaction
- Either all writes succeed, or none do (atomicity)
- For multi-order missions, if validation fails for any single order, the entire transaction is rejected — the drone remains IDLE and all orders remain PENDING

## Optimistic Locking: How Firestore Handles Contention

Firestore implements **optimistic concurrency control** natively. When two transactions attempt to modify the same document concurrently:

1. Transaction A reads drone D1 (status = IDLE), Transaction B reads drone D1 (status = IDLE)
2. Transaction A commits first → SUCCESS: D1.status = MOVING
3. Transaction B attempts to commit
4. Firestore detects that D1 was modified since B's read
5. Firestore **automatically retries** Transaction B from the beginning
6. Transaction B re-reads D1 (now status = MOVING)
7. `MissionAssignmentPolicy` validation fails → `BusinessRejectionException`
8. The rejected decision is logged, and the affected entities are picked up in the next optimization cycle

This is a form of **optimistic locking** — there is no explicit lock acquisition. Instead, conflicts are detected at commit time and resolved by retry. The `MissionAssignmentPolicy` acts as the business-level guard, ensuring that only valid state transitions are committed.

## Telemetry Ordering Protection

Network conditions can cause telemetry messages to arrive out of order. The State Manager protects against stale data via timestamp comparison:

```java
firestore.runTransaction(transaction -> {
    DocumentSnapshot doc = transaction.get(droneRef).get();
    if (doc.exists()) {
        Instant existingTimestamp = /* get lastUpdate from doc */;
        Instant incomingTimestamp = telemetry.getTimestamp();
        if (incomingTimestamp.isBefore(existingTimestamp)) {
            return null;  // Skip stale telemetry — do not apply older data
        }
    }
    Drone updated = DronePolicy.applyTelemetryUpdate(drone, telemetry);
    transaction.set(droneRef, FirestoreMapper.toMap(updated));
    return updated;
});
```

If a telemetry message T1 (timestamp=10:00:01) arrives after T2 (timestamp=10:00:02), T1 is silently discarded. The drone state always reflects the most recent known data.

## Order Ingestion Idempotency

Pub/Sub guarantees **at-least-once delivery**, meaning the same order creation message may be delivered multiple times. The order ingestion handler includes an idempotency guard:

```java
firestore.runTransaction(transaction -> {
    DocumentSnapshot doc = transaction.get(orderRef).get();
    if (doc.exists()) {
        OrderStatus currentStatus = /* get status from doc */;
        if (currentStatus != PENDING && currentStatus != UNSPECIFIED) {
            return null;  // Do not overwrite — order already processed
        }
    }
    Order order = /* build order with PENDING status */;
    transaction.set(orderRef, FirestoreMapper.toMap(order));
    return order;
});
```

This prevents a redelivered message from resetting an `ASSIGNED` order back to `PENDING`, which would cause it to be re-assigned and potentially create duplicate missions.

## Protection Mechanisms Summary

| Mechanism | Location | Protection Provided |
|-----------|----------|---------------------|
| Firestore Transaction | `FirestoreStateTransactionAdapter` | Atomic multi-document writes |
| Optimistic Concurrency | Firestore built-in | Automatic retry on contention |
| Write-time Validation | `MissionAssignmentPolicy` | Status checks before assignment |
| Drone Status Guard | `DronePolicy.canAcceptMission()` | Only IDLE drones can accept missions |
| Order Status Guard | `MissionAssignmentPolicy` | Only PENDING orders can be assigned |
| Timestamp Ordering | `runTelemetryUpdateTransaction` | Reject stale telemetry |
| Idempotency Guard | `runOrderIngestionTransaction` | Prevent resetting processed orders |

## Known Limitations and Future Improvements

1. **Non-transactional snapshot acquisition**: The snapshot queries for drones and orders are separate, non-transactional reads. State may change between the two queries. This is acceptable because write-time validation catches any inconsistencies, but a future `runSnapshotAcquisitionTransaction()` could atomically read all entities.

2. **Unused session tracking**: The `solvingSessionId` field exists in protobuf definitions but is not yet implemented. It would allow marking entities as RESERVED/SOLVING during optimization, reducing wasted computation from overlapping cycles.

3. **No abandoned session cleanup**: If an optimizer crashes mid-cycle, a TTL-based or heartbeat-based cleanup mechanism would be needed to release reserved entities.

**Correctness guarantee**: No race condition can cause incorrect data (double-assignment, lost orders). The system may waste computation on rejected decisions, but this is an acceptable trade-off for simplicity and reliability.
