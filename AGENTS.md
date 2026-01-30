# Agentic Coding Guidelines - DroneFleet Optimizer

This document provides essential instructions for agentic coding tools (AI agents) operating in this repository. Follow these guidelines strictly to maintain consistency and quality.

## 1. Monorepo Structure & Task Execution

This is a polyglot monorepo managed with **mise**.

- **Services:** `services/ingestion` (Python/FastAPI), `services/state_manager` (Java/Spring Boot), `services/path_optimizer` (Python).
- **Libraries:** `libs/java/*`, `libs/python/*`, `libs/ts/*`.
- **Shared Schemas:** `shared/` (Python, Java, TS definitions).

### Task Execution Rule
**ALWAYS** use the Monorepo Task Path Syntax for `mise`:
- Root tasks: `mise run <task>` (e.g., `mise run lint:all`)
- Service tasks: `mise //<path-from-root>:<task-name>`
  - Example (Python): `mise //services/ingestion:lint`
  - Example (Java): `mise //services/state_manager:build`

---

## 2. Build, Lint, and Test Commands

### Aggregator Tasks (Root)
- **Initialize:** `mise run init` (Syncs all dependencies).
- **Test All:** `mise run test:all`
- **Lint All:** `mise run lint:all`
- **Format All:** `mise run format:all`

### Python (using `uv`)
- **Lint:** `uv run ruff check .` and `uv run mypy .`
- **Format:** `uv run black .`
- **Run Tests:** `uv run pytest`
- **Single Test:** `uv run pytest tests/unit/test_file.py::test_function_name`

### Java (using `gradlew`)
- **Lint:** `./gradlew :services:state_manager:checkstyleMain`
- **Format:** `./gradlew :services:state_manager:spotlessApply`
- **Run Tests:** `./gradlew :services:state_manager:test`
- **Single Test:** `./gradlew :services:state_manager:test --tests "com.dronefleet.statemanager.domain.model.DroneTest"`

### TypeScript (using `biome`)
- **Lint/Format:** `npx @biomejs/biome check --write .` (Uses `biome.json` at root).

---

## 3. Code Style Guidelines

### Python (FastAPI/Services)
- **Formatting:** Strict adherence to `black` style.
- **Imports:** 
  1. Standard library.
  2. Third-party packages (FastAPI, Pydantic, etc.).
  3. Local module imports (relative or absolute).
- **Typing:** Use type hints for all function signatures and variables. Prefer `dict[str, Any]` over generic `dict`.
- **Naming:** 
  - Functions/Variables: `snake_case`.
  - Classes: `PascalCase`.
- **Error Handling:** Use `try...except` blocks with specific exceptions. In FastAPI endpoints, raise `HTTPException` with appropriate status codes and descriptive details.

### Java (Spring Boot/Hexagonal Architecture)
- **Architecture:** Follow Hexagonal (Ports & Adapters) patterns.
  - `domain.model`: Pure logic and entities.
  - `domain.port.in` / `domain.port.out`: Interfaces.
  - `infrastructure.adapter`: External implementations (REST, Persistence, Messaging).
- **Formatting:** Managed by `spotless`. Always run `spotlessApply` before committing.
- **Lombok:** Use `@Slf4j`, `@Getter`, `@Setter`, `@AllArgsConstructor`, etc., to reduce boilerplate.
- **Error Handling:** 
  - Throw `DomainException` or `BusinessRejectionException` in the domain layer.
  - Use `@ControllerAdvice` to map exceptions to HTTP responses in the adapter layer.

### TypeScript / Shared Schemas
- **Formatting:** Use Biome defaults (Double quotes, semicolons always, trailing commas).
- **Definitions:** Prefer Interfaces over Types for object definitions. Use Zod for runtime validation when possible.

---

## 4. Version Control Workflows

The user may request changes using either **Git** or **Jujutsu (jj)**. Respect the requested tool.

### Git Workflow (Standard)
- **Atomic Commits:** Group changes into small, logical units.
- **Conventional Commits:** Use `feat:`, `fix:`, `chore:`, `docs:`, `refactor:`.
- **Pre-commit:** Always run relevant `lint` and `format` tasks before committing.

### Jujutsu (jj) Workflow (Alternative)
- **Automatic Snapshots:** `jj` tracks working copy changes automatically.
- **Explicit Description:** Use `jj describe -m "message"` to set commit messages (Conventional Commits).
- **Revision Management:** Use `jj new` to start new changes and `jj squash` or `jj squash -i` to combine changes into the parent.
- **Git Sync:** If working on a git-backed repo, use `jj git push` or `jj git export` to sync with git.
- **Atomic Operations:** Create separate changes for independent logical units using `jj new`.

---

## 5. Security & Observability

- **Secrets:** NEVER commit `.env` files or hardcoded credentials. Use `configs/*.env` as templates only.
- **Logging:**
  - Python: Use structured logging (e.g., `structlog` or standard `logging` configured for JSON).
  - Java: Use `Slf4j` with JSON formatting in production profiles.
- **Traces:** Ensure `Trace ID` propagation across service boundaries (Pub/Sub, REST).

## 6. Development Environment

- **Emulators:** Use local emulators for Pub/Sub (`localhost:8085`) and Firestore (`localhost:8080`) during development.
---

## btca

When you need up-to-date information about technologies used in this project, use btca to query source repositories directly.

**Available resources**: fastapi, springBoot, springCloudGcp, pydantic, googleCloudPython, orTools, biome, uv, mise, mkdocsMaterial

### Usage

```bash
btca ask -r <resource> -q "<question>"
```

Use multiple `-r` flags to query multiple resources at once:

```bash
btca ask -r fastapi -r pydantic -q "How do I use Pydantic v2 with FastAPI?"
```
