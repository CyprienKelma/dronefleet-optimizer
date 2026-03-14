# DroneFleet Optimizer — Claude Code Context

## Project Summary
Real-time cloud management system for emergency medical drone fleets (blood, vaccines, defibrillators) delivering within 15 min. Event-driven polyglot microservices on GCP. Personal project by Cyprien, end of computer engineering studies.

**Status**: Core engine complete (ingestion + state manager + optimizer). Frontend visualizer in progress. BigQuery pipeline planned. Simulator mission consumption in progress.

---

## Architecture

**Pattern**: Polyglot microservices + Hexagonal Architecture + Event-Driven (Pub/Sub)

```
Simulator → HTTP → Ingestion API → Pub/Sub (telemetry/orders/decisions) → State Manager → Firestore
                                                                                ↑
                                                              HTTP GET /snapshot |
                                                                                 |
Cloud Scheduler (every 10s) → Path Optimizer Job → Pub/Sub (decisions) ─────────┘
                                                                                ↓
                                                                        State Manager → Firestore (missions)
Visualizer ← WebSocket ← Pub/Sub (telemetry)
```

**Environments**: LOCAL (Docker Compose + emulators) → DEV (GCP, auto-deploy on push to main) → PROD (manual via release tags)

---

## Services

### `services/ingestion/` — Python 3.11 / FastAPI / Cloud Run Service
- HTTP gateway for telemetry and orders
- Pydantic validation → Pub/Sub publish
- Key: `src/ingestion/api/v1/endpoints/`, `src/ingestion/services/`
- Run: `mise //services/ingestion:dev`

### `services/state_manager/` — Java 21 / Spring Boot 4 / Cloud Run Service
- Consumes Pub/Sub events (telemetry, orders, decisions)
- Maintains Firestore state with optimistic locking transactions
- Provides `GET /api/v1/optimizer/snapshot` to Path Optimizer
- Architecture: Hexagonal (domain / application / infrastructure layers)
- Key: `domain/service/MissionAssignmentPolicy.java`, `infrastructure/adapter/in/messaging/pubsub/`, `infrastructure/adapter/out/persistence/firestore/`
- Run: `mise //services/state_manager:dev`

### `services/path_optimizer/` — Python 3.11 / OR-Tools / Cloud Run Job
- Stateless one-shot: fetch snapshot → build VRP → solve → publish decisions
- VRPPD (Vehicle Routing Problem with Pickup and Delivery), NP-hard
- Two-phase: PARALLEL_CHEAPEST_INSERTION (construction) + GUIDED_LOCAL_SEARCH (improvement)
- Time limit: 8s
- Key: `src/path_optimizer/main.py`, `services/builder.py`, `services/solver.py`, `services/extractor.py`, `clients/state_manager.py`, `clients/publisher.py`

### `services/simulators/` — Python 3.11 / asyncio / Cloud Run Job
- Generates synthetic telemetry (drone positions) and orders
- Mission execution via route waypoints: IN PROGRESS
- Key: `src/simulators/main.py`

### `services/visualizer/` — TypeScript / SolidJS / Vite / Bun / Cloud Run Service
- Real-time map (Leaflet), WebSocket server, battery/mission dashboard
- STATUS: Work in progress

---

## Shared / Libraries

### `shared/proto/` — Single Source of Truth for Data Models
- `.proto` files in `shared/proto/dronefleet/v1/`: `drone.proto`, `order.proto`, `mission.proto`, `warehouse.proto`, `depot.proto`, `snapshot.proto`, `common.proto`
- Managed by **Buf** CLI
- Generates code to: `shared/java/`, `shared/python/`, `shared/ts/`
- Post-gen script: `shared/proto/scripts/fix_python_init.py` (adds re-exports to `shared/python/src/dronefleet_shared/models/__init__.py`)
- Generate: `mise //shared/proto:generate`
- Pre-commit hook enforces models are in sync

### `shared/python/` — `dronefleet-shared` Python package
- Generated models + shared utilities (`global_config.py`, `logging_config.py`)
- Import: `from dronefleet_shared.models import Drone, Order, Mission, ...`

### `shared/java/` — `dronefleet-shared` Java package
- Generated Java models + shared utilities

### `shared/ts/` — TypeScript schemas

### `libs/python/messaging/` — `dronefleet-messaging` (Factory + Adapter)
- `PublisherFactory.get_publisher()` reads `DEPLOYMENT_STRATEGY` env var
  - `on_cloud` → `PubSubPublisher` (real GCP Pub/Sub or emulator if `PUBSUB_EMULATOR_HOST` set)
  - `on_premise` → `KafkaPublisher`
- Key: `src/dronefleet_messaging/factory.py`, `base_publisher.py`, `publisher/pubsub_publisher.py`, `publisher/kafka_publisher.py`

### `libs/python/config/` — `dronefleet-config` (pydantic-settings)
### `libs/python/logging/` — `dronefleet-logging` (structlog, JSON)
### `libs/java/config/`, `libs/java/logging/`

---

## Infrastructure

### GCP Services Used
- Cloud Run Services: `ingestion`, `state-manager`, `visualizer`
- Cloud Run Jobs: `path-optimizer`, `simulator`, `seed-firestore`
- Pub/Sub Topics: `telemetry`, `orders`, `commands`, `decisions`, `dead-letter-queue`
- Pub/Sub Subscriptions: `telemetry-sub`, `orders-sub`, `commands-sub`, `decisions-sub`
- Firestore (Native mode, `(default)` database)
- Artifact Registry: `europe-west1-docker.pkg.dev/drone-fleet-optimizer-dev/drone-fleet/`
- Cloud Scheduler: `trigger-path-optimizer` (every 10s)
- Cloud Billing Budget alerts

### Firestore Collections
- `drones` — drone state (position, battery, status)
- `orders` — delivery orders (status: PENDING → ASSIGNED → DELIVERED)
- `missions` — assigned routes
- `warehouses` — pickup locations with product types
- `depots` — start/end points

### Terraform
- `infra/terraform/environments/dev/main.tf` — full DEV infra definition
- `infra/terraform/modules/` — reusable modules (pubsub, cloud-run, iam)
- State per environment

### Local Development
- `infra/local/docker-compose.yml` — Pub/Sub emulator (`:8085`), Firestore emulator (`:8080`), all services
- Start: `mise //infra/local:up`
- Init topics: pubsub-init container runs automatically on compose up

---

## Monorepo Tooling

### Mise (primary tool orchestrator)
```
mise.toml                              # root: tool versions, env vars, aggregator tasks
services/ingestion/mise.toml           # dev, lint, format, build
services/state_manager/mise.toml       # dev, lint, format, build, test
services/path_optimizer/mise.toml      # start, lint
services/simulators/mise.toml          # run, lint
services/visualizer/mise.toml          # dev, build
shared/proto/mise.toml                 # lint, format, generate, breaking, check
infra/local/mise.toml                  # up, down, logs
infra/terraform/mise.toml              # dev:bootstrap, dev:up, dev:down, dev:status, dev:seed, dev:simulate, plan, apply
```

**Task syntax**:
- Root: `mise run <task>` (e.g., `mise run lint:all`)
- Service: `mise //<path>:<task>` (e.g., `mise //services/ingestion:lint`)

### Python Toolchain
- Package manager: `uv` (workspaces)
- UV workspace members: `services/ingestion`, `services/path_optimizer`, `services/simulators`, `shared/python`, `libs/python/*`
- Root `pyproject.toml` defines workspace + mypy/ruff config
- Linting: `ruff` (strict), Type check: `mypy`, Format: `black`/`ruff format`, Tests: `pytest`

### Java Toolchain
- Build: Gradle 8.5
- `settings.gradle` includes: `services:state_manager`, `shared:java`, `libs:java:logging`, `libs:java:config`
- Java 21 (Temurin), Spring Boot 4
- Linting: Checkstyle (Google Java Style), Formatting: Spotless, Tests: JUnit 5

### TypeScript Toolchain
- Runtime: Bun, Bundler: Vite
- Linting/Formatting: Biome

### Protobuf Toolchain
- Buf CLI for lint, format, generate, breaking change detection

---

## Configuration

### Environment Files (`configs/`)
Loaded by mise via `_.file = [{path = ".env"}, "configs/{{env.ENVIRONMENT}}.env"]`

| File | `ENVIRONMENT` | Use |
|------|--------------|-----|
| `configs/local.env` | `local` | Emulators on localhost |
| `configs/dev.env` | `dev` | Real GCP dev project |
| `configs/prod.env` | `prod` | Production |

### Key Environment Variables
| Var | Local | Dev/Prod |
|-----|-------|----------|
| `ENVIRONMENT` | `local` | `dev`/`prod` |
| `DEPLOYMENT_STRATEGY` | `on_cloud` | `on_cloud` |
| `PROJECT_ID` | `drone-fleet-optimizer-local` | `drone-fleet-optimizer-dev` |
| `PUBSUB_EMULATOR_HOST` | `localhost:8085` | unset |
| `FIRESTORE_EMULATOR_HOST` | `localhost:8080` | unset |
| `STATE_MANAGER_URL` | `http://localhost:8081` | Cloud Run URI |
| `SOLVER_TIME_LIMIT_SECONDS` | `180` | `8` |
| `MIN_BATTERY_THRESHOLD` | `20` | `20` |

---

## Concurrency Model

**Strategy**: First-write-wins with Firestore optimistic locking (no pessimistic locks, no deadlocks).

**Mission assignment transaction** (critical path in `State Manager`):
1. Read drone + all orders atomically
2. Validate via `MissionAssignmentPolicy` (drone=IDLE, all orders=PENDING)
3. Write atomically: create Mission, update drone→MOVING, update orders→ASSIGNED
4. If race condition: Firestore retries tx → policy fails → `BusinessRejectionException` → entities picked up in next cycle

**Other protections**:
- Telemetry ordering: reject messages with timestamp < current stored timestamp
- Order idempotency: don't overwrite non-PENDING orders (at-least-once delivery from Pub/Sub)

---

## CI/CD

### Workflows (`.github/workflows/`)
- `ci.yml` — PRs + pushes to main: detect changed services, proto validation, lint/type check/tests, docker build dry-run, terraform validate
- `cd-dev.yml` — Push to main + manual dispatch: terraform apply, build + push images, deploy to Cloud Run
- `cd-prod.yml` — Release tags: production deployment
- `docs.yml` — Deploy MkDocs to GitHub Pages

### Pre-commit Hooks (`.pre-commit-config.yaml`)
- `detect-secrets` — prevents accidental secret commits
- `check-added-large-files` (max 10MB)
- `check-json`, `check-yaml`, `end-of-file-fixer`, `trailing-whitespace`
- `ruff` — Python linting with auto-fix
- `terraform_fmt`, `terraform_validate`
- `proto-sync-check` — runs `mise //shared/proto:generate` and asserts no diff in generated models

---

## VRP / Optimizer Details

**Graph**: 2N+1 nodes (1 depot + N pickup nodes + N delivery nodes)
**Dimensions**: Distance (minimize), Time (enforce deadlines), Battery (2.5%/km, min 20% reserve)
**Constraints**: pickup-delivery pairs (same drone, pickup before delivery), disjunctions (allow dropping infeasible orders)
**Time windows**: CRITICAL=15min, HIGH=30min, STANDARD=60min
**Solver time limit**: 8s (dev), configurable via `SOLVER_TIME_LIMIT_SECONDS`

---

## Service Accounts (GCP DEV)
- `ingestion` — publishes to telemetry, orders, decisions topics
- `state-manager` — subscribes to all subs, Firestore read/write
- `optimizer` — publishes to decisions, Firestore read (snapshot)
- `visualizer` — subscribes to telemetry-sub
- `scheduler` — invokes Cloud Run jobs
- `simulator` — calls Ingestion API (HTTP)
- `seed-firestore` — Firestore write (initial seed data)

---

## Work in Progress
1. **Frontend Visualizer** — SolidJS + Leaflet + WebSocket, real-time map
2. **Simulator mission execution** — subscribe to decisions, simulate drone along waypoints
3. **BigQuery Analytics Pipeline** — Pub/Sub → BigQuery → dbt → Looker Studio

---

## Quick Reference Commands

```bash
# Local dev startup
mise //infra/local:up                    # start emulators (Pub/Sub + Firestore)
mise //services/ingestion:dev            # FastAPI dev server
mise //services/state_manager:dev        # Spring Boot dev
mise //services/path_optimizer:start     # run optimizer once
mise //services/simulators:run           # run simulator

# Protos
mise //shared/proto:generate             # regenerate all models
mise //shared/proto:check                # lint + breaking + generate

# GCP DEV
mise //infra/terraform:dev:bootstrap     # first-time full setup
mise //infra/terraform:dev:up            # restore scaling
mise //infra/terraform:dev:down          # scale to zero ($0)
mise //infra/terraform:dev:seed          # seed Firestore
mise //infra/terraform:dev:simulate      # run simulator job

# Lint/Test
mise run lint:all
mise run test:unit
./gradlew test                           # Java tests (from repo root)
```
