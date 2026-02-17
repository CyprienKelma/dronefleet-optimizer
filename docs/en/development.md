# Development

## Code Quality Standards

The project enforces strict quality standards:

**Python Services:**
- Linting: `ruff` with strict rules
- Type checking: `mypy` in strict mode
- Formatting: `ruff format`
- Testing: `pytest` with coverage requirements

**Java Services:**
- Linting: Checkstyle with Google Java Style
- Formatting: Spotless with automatic fixes
- Testing: JUnit 5

**TypeScript Services:**
- Linting: Biome (ESLint alternative)
- Type checking: TypeScript strict mode
- Formatting: Biome

## Pre-commit Hooks

The repository uses pre-commit hooks to enforce:
- Protobuf model synchronization
- Code formatting
- Linting rules
- Commit message conventions

Install hooks:

```bash
pre-commit install
```

## Testing

**Unit Tests:**

```bash
# Python services
cd services/ingestion
uv run pytest tests/

# Java services
cd services/state_manager
./gradlew test
```

**Integration Tests:**

Located in `tests/integration/` - test complete flows with emulators.

**End-to-End Tests:**

Located in `tests/e2e/` - test full system with simulated drone fleet.
