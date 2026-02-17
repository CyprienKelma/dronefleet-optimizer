# Repository Structure

## Monorepo Management with Mise

The repository is organized as a **polyglot monorepo** managed with [**mise**](https://mise.jdx.dev/) (formerly "mise en place"). Mise handles:

- **Tool version management**: Python 3.11, Java 21 (Temurin), Node.js, Gradle, Terraform, Buf, and more — all pinned in the root `mise.toml`
- **Environment variables**: Automatic loading of `.env` files from `configs/` based on the `ENVIRONMENT` variable (`local`, `dev`, `prod`)
- **Task orchestration**: Each service and shared module defines its own `mise.toml` with service-specific tasks (build, lint, test, dev), while the root `mise.toml` provides aggregator tasks (`test:all`, `lint:all`, `format:all`)
- **Virtual environment auto-activation**: Python services automatically create and activate `.venv` directories via `uv`

```
mise.toml                          # Root: tool versions, env vars, aggregator tasks
├── services/ingestion/mise.toml   # Python: dev, lint, format, build
├── services/state_manager/mise.toml  # Java: dev, lint, format, build, test
├── services/path_optimizer/mise.toml # Python: start, lint
├── services/simulators/mise.toml  # Python: run, lint
├── services/visualizer/mise.toml  # TypeScript: dev, build
├── shared/proto/mise.toml         # Protobuf: lint, format, generate, breaking
└── infra/local/mise.toml          # Docker Compose: up, down, logs
```

Tasks are invoked using the **monorepo path syntax**:
- Root tasks: `mise run <task>` (e.g., `mise run lint:all`)
- Service tasks: `mise //<path>:<task>` (e.g., `mise //services/ingestion:lint`, `mise //services/state_manager:build`)

## Directory Layout

```
dronefleet-optimizer/
├── services/                    # Microservices (each independently deployable)
│   ├── ingestion/               # Python/FastAPI — HTTP gateway, Pub/Sub publisher
│   ├── state_manager/           # Java/Spring Boot — Event processing, Firestore persistence
│   ├── path_optimizer/          # Python/OR-Tools — VRP solver, batch optimization
│   ├── simulators/              # Python — Synthetic telemetry & order generation
│   └── visualizer/              # TypeScript/SolidJS — Real-time map dashboard
│
├── shared/                      # Cross-service shared definitions
│   ├── proto/                   # Protobuf source of truth (.proto files + Buf config)
│   ├── java/                    # Generated Java models (betterproto/protobuf)
│   ├── python/                  # Generated Python models + shared utilities
│   └── ts/                      # Generated TypeScript models
│
├── libs/                        # Reusable internal libraries
│   ├── python/
│   │   ├── config/              # Shared Python configuration (pydantic-settings)
│   │   ├── logging/             # Structured logging setup (structlog, JSON)
│   │   └── messaging/           # Message publisher abstraction (Factory + Adapter)
│   ├── java/
│   │   ├── config/              # Shared Java configuration
│   │   └── logging/             # Java logging setup (Slf4j, JSON)
│   └── ts/
│       ├── config/              # Shared TypeScript configuration
│       └── logging/             # TypeScript logging setup
│
├── configs/                     # Environment-specific configuration files
│   ├── local.env                # Local dev with emulators (PUBSUB_EMULATOR_HOST, etc.)
│   ├── dev.env                  # GCP dev environment (real Pub/Sub, Firestore)
│   └── prod.env                 # GCP production environment
│
├── infra/
│   ├── local/                   # Docker Compose for local emulators (Pub/Sub, Firestore)
│   └── terraform/               # IaC: modules for Cloud Run, Pub/Sub, Firestore, IAM
│       ├── environments/dev/    # Dev environment Terraform config
│       ├── environments/prod/   # Prod environment Terraform config
│       └── modules/             # Reusable Terraform modules
│
├── tests/                       # Cross-service tests
│   ├── unit/
│   ├── integration/
│   └── e2e/
│
└── docs/                        # Documentation and architecture diagrams
```

## Shared Models via Protocol Buffers + Buf

All data models shared across services are defined as **Protocol Buffers** (`.proto` files) in `shared/proto/dronefleet/v1/`. This is the single source of truth for:

- Drone, Order, Mission, Warehouse entities
- Event messages (telemetry, decisions)
- Enum definitions (DroneStatus, OrderStatus, OrderPriority, WaypointType)

The [**Buf**](https://buf.build/) CLI manages the protobuf workflow:

- **`buf lint`**: Enforces consistent proto style
- **`buf format`**: Auto-formats `.proto` files
- **`buf generate`**: Generates typed code for Java, Python, and TypeScript simultaneously
- **`buf breaking`**: Detects breaking schema changes against the `main` branch (run in CI)

Generated code is placed in `shared/java/`, `shared/python/`, and `shared/ts/`. This approach ensures that:

- All services share an **identical, strongly-typed contract** — no drift between a Python DTO and a Java DTO
- Schema versioning and breaking change detection are automated
- Migration to gRPC or binary serialization is possible in the future with minimal effort

The trade-off is slightly more complex tooling, but the generation and CI checks are fully automated via `mise //shared/proto:generate` and the CI pipeline.

## Messaging Library: Factory + Adapter Pattern

The `libs/python/messaging/` library abstracts the message bus implementation using a **Factory + Adapter** design pattern:

```
libs/python/messaging/src/dronefleet_messaging/
├── base_publisher.py          # Abstract base class (MessagePublisher)
├── factory.py                 # PublisherFactory — selects implementation
└── publisher/
    ├── pubsub_publisher.py    # Google Cloud Pub/Sub adapter
    └── kafka_publisher.py     # Apache Kafka adapter (on-premise option)
```

The `PublisherFactory` reads the `DEPLOYMENT_STRATEGY` environment variable and instantiates the appropriate publisher:

- **`on_cloud`**: Uses `PubSubPublisher` — connects to GCP Pub/Sub (or the Pub/Sub emulator when `PUBSUB_EMULATOR_HOST` is set)
- **`on_premise`**: Uses `KafkaPublisher` — connects to a Kafka cluster (for hypothetical on-premise deployments)

This separation allows the system to run in three modes without any code changes:

| Mode | Strategy | Infrastructure | Use Case |
|------|----------|---------------|----------|
| **Local sandbox** | `on_cloud` | Docker Compose + GCP emulators (Pub/Sub on `localhost:8085`, Firestore on `localhost:8080`) | Day-to-day development, zero cost |
| **GCP Dev/Prod** | `on_cloud` | Real GCP Pub/Sub + Firestore | Deployed environments (`dev.env`, `prod.env`) |
| **On-premise** | `on_premise` | Self-managed Kafka cluster | Hypothetical enterprise deployment |

The environment configuration files in `configs/` are loaded by mise and injected as environment variables. In CI/CD (e.g., `cd-dev.yml`), these same variables are set via the deployment workflow to configure services for the target GCP environment.
