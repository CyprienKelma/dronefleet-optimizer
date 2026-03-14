Analyze all unstaged and staged changes in the git working tree and create clean, atomic commits — one per logical concern. Do NOT push to remote.

Follow these steps strictly:

## Step 1 — Analyze changes
Run in parallel:
- `git status` to see all modified, staged, and untracked files
- `git diff HEAD` to see all changes (staged + unstaged)
- `git log --oneline -10` to understand recent commit style

## Step 2 — Group into atomic commits
Group changes by logical concern using these rules:
1. **Proto changes** (`shared/proto/**`) → own commit
2. **Generated model updates** (`shared/java/`, `shared/python/`, `shared/ts/`) → own commit (or grouped with proto if trivial)
3. **Infra / Terraform** (`infra/terraform/**`) → own commit
4. **CI/CD workflows** (`.github/workflows/**`) → own commit with `ci:` prefix
5. **Docker / local infra** (`infra/local/**`) → own commit with `chore:` prefix
6. **Config / env files** (`configs/**`, `mise.toml`) → own commit with `chore:` prefix
7. **Service implementation** (one commit per service if changes are independent)
8. **Tests** → own commit with `test:` prefix when possible
9. **Docs** → own commit with `docs:` prefix

## Step 3 — Present the plan
Present the proposed commit sequence to the user BEFORE executing:
```
Proposed commits:
1. feat(ingestion): ...
2. chore: update dev.env with new solver param
3. test(state_manager): ...
```
Ask: "Proceed with these commits? (yes / adjust)"

## Step 4 — Execute (only after approval)
For each commit:
1. Stage only the relevant files: `git add <specific files>`
2. Commit using the format:
```
<type>: <description>

<optional body explaining why>
```
3. If a pre-commit hook fails: fix the issue, re-stage, create a NEW commit (never amend)

## Commit Message Rules
- Type: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `ci`
- Description: imperative, lowercase, no trailing period, max 72 chars
- Body: explain *why*, not *what*. Wrap at 72 chars.
- Do NOT add "Co-Authored-By" lines (attribution disabled globally)

## Hard Rules
- **Never push** — local commits only
- **Never use `--no-verify`** — fix hook failures instead
- **Never use `git add -A` or `git add .`** — always stage specific files
- **Never amend** a previous commit — always create a new one
- Do not commit: `.env` with secrets, `IMPLEMENTATION_PLAN.md`, `.venv/`, `__pycache__/`, Terraform state files
