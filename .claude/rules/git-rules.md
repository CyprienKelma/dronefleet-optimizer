# Git Rules

## Commit Message Format
```
<type>: <description>

<optional body>
```

**Types**: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `ci`

**Rules**:
- Description: imperative mood, lowercase, no period, max ~72 chars
- Body: explain *why*, not *what* (the diff shows what). Wrap at 72 chars.
- One logical change per commit (atomic commits).

**Examples**:
```
feat: add battery dimension to VRP solver

chore: update terraform cloud scheduler interval to 10s

fix: reject stale telemetry messages in state manager transaction

docs: add concurrency model to architecture docs

refactor: extract waypoint classification into SolutionExtractor

ci: add proto breaking change check to CI workflow
```

## Atomic Commits — Grouping Rules
Split changes into separate commits when they touch different concerns:
1. **Proto changes** → separate commit (`feat: add X field to drone.proto`)
2. **Generated model updates** → separate commit or squash with proto commit (`chore: regenerate models after drone proto update`)
3. **Infra / Terraform** → separate commit from application code
4. **CI/CD workflow changes** → `ci:` prefix, separate commit
5. **Docs updates** → `docs:` prefix, can be separate or bundled with the feature they document
6. **Config changes** (env files, mise.toml) → `chore:` prefix
7. **Tests** → `test:` prefix, separate from implementation when possible

## Workflow
1. `git status` + `git diff` to understand all changes
2. Group into logical atomic units
3. Stage and commit each group separately
4. Validate (lint, tests) before committing

## Branches
- Main branch: `main`
- Feature branches: `feat/<short-description>`
- Fix branches: `fix/<short-description>`
- Infra branches: `infra/<short-description>`

## Pre-commit Hooks
Hooks run automatically on `git commit`:
- detect-secrets, large-files check
- ruff (Python lint + fix)
- terraform_fmt, terraform_validate
- proto-sync-check (regenerates models, asserts no diff)

**Never use `--no-verify`** unless explicitly asked. Fix the root cause instead.

## Do NOT Commit
- `.env` files with real credentials
- `IMPLEMENTATION_PLAN.md`
- `.venv/`, `__pycache__/`, `*.pyc`, `.mypy_cache/`, `build/`, `dist/`
- Terraform state files (`*.tfstate`, `*.tfstate.backup`, `.terraform/`)
