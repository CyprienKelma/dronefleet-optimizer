# Agent Guardrails & Guidelines

This document outlines the operational protocols for AI agents working on the current project.

## 1. Environment & Tooling
*   **Mise First:** Always prioritize `mise` for environment setup, tool versioning, and task execution.
    *   Check `mise.toml` for available tools and versions.
    *   Use `mise run <task>` for project commands defined in `mise.toml` (or `Makefile` if wrapped).
*   **Tool Documentation:** If you are unsure about a tool's usage or capabilities, **always** ask for or fetch its documentation before proceeding. Do not guess flags or parameters.

## 2. Planning & Implementation
*   **Mandatory Planning Phase:** Before writing any functional code for a new feature, you **MUST** create or update `IMPLEMENTATION_PLAN.md`.
    1.  **Draft:** detailed specific steps, files to create/modify, and the verification strategy.
    2.  **Review:** Present the plan to the user.
    3.  **STOP & WAIT:** **Do not proceed** with coding until the user explicitly approves the plan.
    4.  **No Commit:** Do not commit `IMPLEMENTATION_PLAN.md` to version control (add it to `.gitignore` if necessary).
    5.  **Comment properly:** When adding a comment, write in English and explain consisely.
*   **Execution:** Once approved, follow the steps in `IMPLEMENTATION_PLAN.md` strictly.
*   **Global Plan:** Maintain `PLAN.md` for high-level project goals and architecture. Ensure it stays synchronized with current progress.
*   **Plan Updates:** If the user requests changes or a technical decision alters the agreed-upon approach during the conversation, **immediately update** `IMPLEMENTATION_PLAN.md` and ask for re-approval before continuing.

## 3. Git Workflow & Commits
When asked to commit changes, follow this strict two-step process:

### Step 1: Analysis & Planning
1.  **Analyze** the current `git status` and `git diff`.
2.  **Group** changes into logical, atomic units. Avoid monolithic "catch-all" commits.
3.  **Plan** the commit messages using the **Conventional Commits** standard (e.g., `feat:`, `fix:`, `chore:`, `docs:`).
4.  **Present** this plan to the user for approval.

### Step 2: Execution (After Approval)
1.  **Generate** the specific `git add` and `git commit` commands to execute the approved plan.
2.  Execute the commands only after explicit user confirmation.

## 4. General Behavior
*   Be concise and direct.
*   Make surgical changes; do not modify unrelated files.
*   Validate changes (run tests/linters) before committing.

## 5. Documentation Standards
*   **README Generation:** When asked to create or update `README.md`, strictly adhere to professional standards (e.g., [Make a README](https://www.makeareadme.com/) and [GitHub Docs](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-readmes)).
*   **Structure:** Ensure the file includes at minimum:
    *   **Title & Description:** A clear, concise explanation of what the project does and why it exists.
    *   **Installation:** Accurate steps reflecting the project's actual tooling (referencing `mise.toml` as per Section 1).
    *   **Usage:** Concrete examples or code snippets showing how to run the project.
    *   **Roadmap/Status:** Current state of the project (synced with `PLAN.md`).
    *   **License:** The project's license status.
*   **Accuracy:** Always analyze the codebase (`package.json`, `mise.toml`, `go.mod`, etc.) *before* writing to ensure installation commands and dependency lists are factual, not hallucinated.
