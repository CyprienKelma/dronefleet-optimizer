# Path Optimizer System

The **Path Optimizer** is the core intelligence of DroneFleet Optimizer — a batch optimization service that solves the Vehicle Routing Problem with Pickup and Delivery (VRPPD) for the drone fleet. It is implemented in Python 3.11 and relies on **Google OR-Tools** for the combinatorial optimization core.

## Execution Model

The service runs as a **stateless one-shot process**, triggered on a rolling 10-second schedule via Cloud Run Jobs in production (or as a Docker container locally). Each execution constitutes a single optimization cycle:

1. **Fetch** the current world state (snapshot) from the State Manager via HTTP GET
2. **Build** a mathematical model of the routing problem
3. **Solve** the model under physical and business constraints
4. **Publish** the resulting mission assignments to Pub/Sub

The optimizer reads everything it needs from the State Manager snapshot and produces output exclusively through the Pub/Sub message bus — no direct database access.

```
Ingestion API --> Pub/Sub --> State Manager --> Firestore
                                    |
                          HTTP GET /snapshot
                                    |
                             Path Optimizer
                                    |
                          Pub/Sub (decisions)
                                    |
                             State Manager --> Firestore (missions)
```

## Problem Classification

The problem solved is a **Vehicle Routing Problem with Pickup and Delivery (VRPPD)** — a well-known NP-hard variant of the classical VRP. In our context:

- **Vehicles** are drones, each with heterogeneous battery levels
- **Pickups** happen at warehouses (goods must be collected before delivery)
- **Deliveries** happen at hospitals (final destination for each order)
- Every drone starts and ends at the same **depot**

This is further augmented with:

- **Time windows** on deliveries (priority-based deadlines: CRITICAL 15 min, HIGH 30 min, STANDARD 60 min)
- **Battery constraints** (energy consumption proportional to distance, 2.5% per km, minimum 20% reserve on return)
- **Product-warehouse compatibility** (a warehouse must stock the product type requested by the order)

## VRP Graph Construction

The graph contains `2N + 1` nodes for N orders:

```
Index 0          : Depot (start/end for all vehicles)
Indices 1..N     : Pickup nodes (one per order, at nearest compatible warehouse)
Indices N+1..2N  : Delivery nodes (one per order, at the order's destination)
```

**Critical design decision — unique pickup nodes per order:** Each order gets its own dedicated pickup node, even though multiple orders may be picked up from the same physical warehouse. This is mandatory because OR-Tools `AddPickupAndDelivery(p, d)` requires a strict 1-to-1 relationship between pickup and delivery nodes. Sharing a warehouse node across multiple orders would make the problem infeasible.

For each order, the builder selects the **nearest compatible warehouse** (by Haversine distance to the delivery location). Two `(2N+1) × (2N+1)` matrices are computed:

- **Distance matrix** (meters): pairwise Haversine distances between all node coordinates
- **Time matrix** (seconds): derived from the distance matrix assuming a constant drone cruising speed of 50 km/h

## Solver Dimensions and Constraints

The solver uses OR-Tools' `RoutingModel` API, expressing the problem through **dimensions** (cumulative variables tracked along each route) and **constraints**:

| Dimension | Transit Callback | Upper Bound | Purpose |
|-----------|-----------------|-------------|---------|
| **Distance** | Meters between nodes | 100 km | Minimizes total distance (arc cost evaluator). Global span cost coefficient of 100 encourages even workload distribution across drones. |
| **Time** | Seconds between nodes | 3 hours (10,800s) | Enforces time windows on delivery nodes. Slack up to 30 min allows drones to "wait" at nodes. Start cumul is free (drones can depart at different times). |
| **Battery** | `distance_km × 2.5 × 10` (scaled for integer precision) | 1,000 units (100%) | Per-vehicle cap: `(initial_battery% - 20%) × 10`. Example: drone at 85% battery → max 650 units → 26 km total flight. |

**Pickup-delivery constraints** ensure each order's pickup and delivery are performed by the same drone, in the correct order. **Disjunctions** (penalty: 100,000 per node) allow the solver to gracefully drop infeasible orders rather than making the entire problem unsolvable.

## Algorithm Selection: Two-Phase Approach

Since VRPPD is **NP-hard** (no polynomial-time optimal solution exists), OR-Tools uses a two-phase heuristic/metaheuristic strategy:

![Optimizer Diagram](images/path_optimizer.png)

### Phase 1: Constructive Heuristic (PARALLEL_CHEAPEST_INSERTION)

A greedy algorithm that considers all unrouted nodes simultaneously across all vehicles. At each step, it inserts the node that causes the smallest increase in total cost into the best position of any route. Complexity: **O(n² × V)** where n = number of nodes, V = number of vehicles. Completes in milliseconds for typical problem sizes.

### Phase 2: Metaheuristic Improvement (GUIDED_LOCAL_SEARCH)

An iterative improvement metaheuristic that performs local search moves (relocate, swap, move segments) and escapes local optima by penalizing frequently appearing features. This is an **anytime algorithm**: the longer it runs, the better the solution. Time limit: **30 seconds** (configurable). Each iteration: **O(n²)**.

### Complexity Comparison

| Approach | Time Complexity | Optimality | Practical Use |
|----------|----------------|------------|---------------|
| Exact (branch-and-bound) | O(n! / symmetries) | Proven optimal | Feasible for n < 20-30 |
| MILP | Exponential worst case | Proven optimal | Feasible for n < 50-100 |
| Nearest neighbor heuristic | O(n²) | 15-25% from optimal | Very fast, poor quality |
| PARALLEL_CHEAPEST_INSERTION | O(n² × V) | 10-20% from optimal | Fast, reasonable quality |
| **GLS metaheuristic (our choice)** | **O(n²) per iteration, time-bounded** | **1-5% from optimal** | **Best balance of speed and quality in our case** |
| Genetic algorithms | O(P × n² × G) | Variable | Slower convergence for VRP |

## Solution Extraction

After solving, the `SolutionExtractor` walks each vehicle's route to build `MissionAssignment` messages:

1. Start at depot, follow the route until return to depot
2. Classify each node: `DEPOT_START`, `WAREHOUSE_PICKUP`, `HOSPITAL_DELIVERY`, `DEPOT_RETURN`
3. Compute aggregate metrics (total distance, time, battery consumption)
4. Skip idle drones (routes with only depot-start and depot-return)

A typical single-order route: `DEPOT_START → WAREHOUSE_PICKUP → HOSPITAL_DELIVERY → DEPOT_RETURN`

A multi-order route alternates pickups and deliveries:
```
DEPOT_START → WAREHOUSE_PICKUP → HOSPITAL_DELIVERY
            → WAREHOUSE_PICKUP → HOSPITAL_DELIVERY
            → DEPOT_RETURN
```

## Data Model

**Input (OptimizationSnapshot):**

| Field | Type | Description |
|-------|------|-------------|
| `session_id` | string | Unique identifier for this optimization cycle |
| `timestamp` | Timestamp | When the snapshot was created |
| `depot` | Depot | Main depot (start/end point for all drones) |
| `drones` | List[Drone] | Available IDLE drones with position, battery, consumption rate |
| `orders` | List[Order] | PENDING delivery orders with location, priority, product type |
| `warehouses` | List[Warehouse] | Pickup locations with authorized product types |

**Output (MissionAssignment):**

| Field | Type | Description |
|-------|------|-------------|
| `drone_id` | string | The assigned drone |
| `order_ids` | List[string] | Orders fulfilled in this mission |
| `route` | List[Waypoint] | Ordered sequence of waypoints with type, position, references |
| `estimated_battery_consumption` | double | Total estimated battery usage (%) |
| `estimated_duration_minutes` | double | Total estimated flight duration |

## Practical Performance

With the current seed data (5 drones, 18 orders, 2 warehouses = 37 nodes), the solver finds a high-quality solution well within the 10-second time limit. The system is designed to scale to the MVP target of 50-100 drones with hundreds of orders by adjusting the time limit and potentially sharding the problem geographically.
