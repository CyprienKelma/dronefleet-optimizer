# Design Decisions

## Why Polyglot Architecture?

I chose **Python for the Ingestion API and Optimizer** because each service had very different technical requirements. FastAPI is genuinely the best choice for high-throughput, asynchronous I/O-bound workloads like validating and routing incoming telemetry. The Ingestion API needs to handle thousands of position updates per second without blocking, and FastAPI + uvicorn delivers that effortlessly. For the Path Optimizer, Google OR-Tools is the de facto standard for routing problems — it's battle-tested, well-documented, and Python bindings are first-class. Rather than fight these ecosystems or try to force everything into one language, I leverage the right tool for each job.

**Java powers the State Manager** because it's where complex, correct business logic lives. The state manager is the heart of the system — it has to enforce invariants around drone status, order transitions, and mission creation atomically. Java's strong typing and compile-time guarantees catch entire categories of bugs before runtime. Spring Boot's transaction management and the Firestore SDK's maturity made it the natural choice for a service that needs to be rock-solid. The hexagonal architecture pattern is also much easier to implement cleanly in Java's ecosystem than elsewhere.

**TypeScript runs the Frontend** because real-time, reactive UI state is a first-class concern. SolidJS offers fine-grained reactivity — only the specific parts of the DOM that change actually re-render, which matters when pushing thousands of position updates per second. The type safety catches UI state bugs early, and modern tooling (Vite, Bun) keeps iteration fast.

The polyglot approach means I'm not forcing artificial abstractions or fighting language constraints. Each service uses its best-suited tool, and the shared Protocol Buffer definitions keep everyone on the same page. It's more operational complexity than a monolith, but it's worth it for the clarity and correctness it buys.

## Why Firestore over PostgreSQL?

I spent time deliberating between Firestore and PostgreSQL, and Firestore won because of the specific problem I'm solving. Drone positions are fundamentally geographic — Firestore's native GeoPoint type means I get geographic queries and distance calculations without manually managing lat/lon columns. Scaling is also beautifully simple: Firestore autoscales horizontally, no capacity planning, no database administration. I can start with the free tier during development (which I do) and scale linearly with usage.

Atomicity is another win. Firestore transactions are optimistic and built into the SDK — I don't need a separate distributed transaction coordinator or saga pattern. The tradeoff is that Firestore's query capabilities are less powerful than SQL (no complex joins, aggregations across millions of documents are expensive). But the State Manager's access patterns are simple: read/write individual documents by ID, or range queries on a few indexed fields. This is exactly where Firestore shines.

The cost argument used to scare me — Firestore charges per read/write operation. But batch writes within a transaction are cheap, and the real-time state is hot (drones, orders, missions), so the data set is compact. During local development with emulators (zero cost), I can iterate freely. Once in production, the operational simplicity of Firestore (no backup management, no replication configuration, no failover) more than compensates for the per-operation cost model.

If I needed complex relational queries (multi-table joins, analytics in the critical path), or if I had millions of historical records that needed to be accessed frequently, I'd reconsider. But for a real-time operational database with simple access patterns, Firestore was the right call.

## Concurrency Model: First-Write-Wins

The concurrency model is where I made a deliberate trade-off between simplicity and computation efficiency. I chose **first-write-wins with optimistic locking**, which means when two optimization cycles compete for the same drone, whoever commits first wins — the second one gets rejected and automatically recovers in the next cycle (10 seconds later).

I considered **pessimistic locking** with `RESERVED` and `SOLVING` states — marking entities as reserved at snapshot time would prevent other cycles from including them. This would eliminate wasted computation. But it introduces complexity: I'd need distributed lock management, session cleanup for crashed optimizers, and careful timeout handling. Plus, there's always the risk of deadlock or leaked locks.

Given that optimization cycles run every 10 seconds and the solver takes about 8 seconds, concurrent cycles are actually rare. When they do happen, a few decisions getting rejected is not a big deal — the entities will be picked up and correctly assigned in the next cycle. The simplicity of optimistic locking (just check status before committing) outweighs the occasional wasted computation. This is a pragmatic choice for an MVP where reliability and simplicity matter more than squeezing every ounce of efficiency.

## Event-Driven vs Request-Response

The system uses a hybrid messaging strategy, and the choice of which to use depends on the access pattern.

**Telemetry and orders flow through Pub/Sub** because they're high-frequency, asynchronous events that don't require synchronous responses. A drone sending its position 5 times per second doesn't care if the State Manager is busy — it just publishes and moves on. Pub/Sub gives me a natural buffer (the topic) and decouples the producer from the consumer. If the State Manager went down for 5 minutes, telemetry would queue up and be processed once it recovered. This async approach scales beautifully.

**The optimization snapshot, however, is synchronous HTTP GET.** The Optimizer needs a consistent point-in-time snapshot of the world — all drones and orders as they exist at this moment. Pub/Sub wouldn't make sense here because there's no "event" per se; it's a query for current state. HTTP GET is simpler, more direct, and it's easy to add timeouts and retry logic. If the State Manager is slow, the Optimizer can fail fast and try again in 10 seconds.

This hybrid approach is intentional: fire-and-forget event streams for state changes, synchronous queries for consistent snapshots. It's the best of both worlds.
