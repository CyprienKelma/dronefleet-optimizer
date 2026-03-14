# General Project Rules

## Tooling: Mise First
- **Always use `mise` for task execution** — never run raw `python`, `java`, `gradle`, `terraform`, `buf` etc. directly unless no mise task exists.
- Check the relevant `mise.toml` before running any command.
- Task syntax: `mise run <task>` (root) or `mise //<path>:<task>` (service-specific).
- Examples: `mise //services/ingestion:lint`, `mise //shared/proto:generate`, `mise //infra/local:up`.

## Data Models: Proto is Source of Truth
- **Never hand-write data models** that already exist or belong in `shared/proto/dronefleet/v1/*.proto`.
- Any new shared entity or message → define in `.proto` first, then run `mise //shared/proto:generate`.
- Python imports: `from dronefleet_shared.models import Drone, Order, Mission, ...`
- Java imports: from `com.dronefleet.shared.models.*`
- Generated files in `shared/java/`, `shared/python/`, `shared/ts/` are **auto-generated — do not edit manually**.
- After adding/modifying `.proto` files always run `mise //shared/proto:check` (lint + breaking + generate).

## Messaging: Use the Factory
- Python services publish messages via `PublisherFactory.get_publisher()` from `dronefleet-messaging`.
- Never instantiate `PubSubPublisher` or `KafkaPublisher` directly in service code.
- Transport backend is configured via `DEPLOYMENT_STRATEGY` env var (`on_cloud` / `on_premise`).

## Config / Environment Variables
- Config is loaded from `configs/<ENVIRONMENT>.env` via mise + pydantic-settings (`dronefleet-config`).
- New config values → add to all three env files (`local.env`, `dev.env`, `prod.env`) and to the pydantic settings model in `libs/python/config/`.
- Never hardcode project IDs, region names, topic names, or URLs in service code.

## Language / Stack Per Service
| Service | Language | Framework | Do NOT switch |
|---------|----------|-----------|---------------|
| ingestion | Python 3.11 | FastAPI | — |
| state_manager | Java 21 | Spring Boot 4 | — |
| path_optimizer | Python 3.11 | OR-Tools | — |
| simulators | Python 3.11 | asyncio | — |
| visualizer | TypeScript | SolidJS / Bun | — |

## Code Quality
- **Python**: ruff (lint + format), mypy (strict). Run via `mise //<service>:lint` before committing.
- **Java**: Checkstyle (Google Java Style), Spotless (format). Run via `mise //services/state_manager:lint`.
- **TypeScript**: Biome (lint + format).
- **Terraform**: `terraform_fmt` + `terraform_validate` enforced by pre-commit.
- Pre-commit hooks run automatically — do not skip with `--no-verify`.

## Architecture Patterns
- **State Manager** uses Hexagonal Architecture: domain logic in `domain/`, adapters in `infrastructure/adapter/`.
- New State Manager features → domain logic first, then infrastructure adapters.
- Path Optimizer is **stateless** — reads only from State Manager HTTP snapshot, writes only to Pub/Sub. No direct Firestore access from optimizer.
- Ingestion API does validation + Pub/Sub publish only — no business logic.

## Concurrency (State Manager)
- All state mutations go through Firestore transactions.
- Mission assignment logic lives in `MissionAssignmentPolicy` — first-write-wins, no pessimistic locks.
- Do not add `RESERVED` or `SOLVING` drone states without explicit discussion (intentionally avoided — see design-decisions.md).


## Documentation
- Comments in English, concise, explain *why* not *what*.
- Do not add docstrings/comments to code you didn't change.
