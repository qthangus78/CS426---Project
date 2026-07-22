# Agent Environment (v0.2)

This folder is the operating kit for AI agents working on FieldFlow.

## Purpose

The kit keeps agent work scoped, reviewable, and consistent with the FieldFlow architecture. It is not a replacement for the project docs; it points agents to the smallest useful context and the verification command that matches the changed area.

## Parts

- `AGENTS.md` - durable repo rules for Codex and other agents.
- `CONTEXT.md` - the minimum context pack and prompt template.
- `EVALUATION.md` - the quality gate used before finishing a task.
- `PROMPT_EXAMPLES.md` - token-efficient prompt patterns by task type.
- `SOURCES.md` - external sources for tools, evaluation, and the FieldFlow stack.
- `../../scripts/agent/verify.ps1` - scope-aware local verification.

## v0.2 Changes

- Clarifies that `AGENTS.md` should stay concise and durable.
- Adds a source policy for official Codex, Android, Circuit, GitHub, and OpenAI references.
- Separates current repo state from future proposal scope.
- Makes sandbox, approval, and docs-only verification expectations explicit.

## Recommended Agent Roles

For larger tasks, split the work into three roles:

- planner - reads the relevant docs, defines scope, and selects files;
- implementer - makes the smallest code change;
- verifier - runs the scoped checks and inspects the diff.

That pattern keeps the agent from over-reading the repo and helps preserve module boundaries.

Use subagents only when the work can be separated cleanly. Small changes should stay in one thread so the diff remains easy to reason about.

## Good Additions

If the project grows, the next useful additions are:

- `docs/adr/` for lightweight architecture decisions;
- `.github/CODEOWNERS` when GitHub usernames are ready;
- `docs/agent/TRACES.md` or similar for debugging agent runs;
- `scripts/agent/bootstrap.ps1` if you want automated setup checks.

Add a nested `AGENTS.md` only when a subtree has stable rules that differ from the repo-level rules. Otherwise keep module-specific guidance in the architecture docs or README files.

## Use It Like This

Start each task by reading `AGENTS.md`, then the relevant module docs, then the file set for the slice you are changing. Finish by running `scripts/agent/verify.ps1` with the narrowest scope that matches the change.

Examples:

```powershell
.\scripts\agent\verify.ps1 -Path domain\src\main\kotlin\com\topic11\cs426\domain\usecase\ObserveInspectionUseCase.kt
.\scripts\agent\verify.ps1 -Path README.md docs\agent\README.md
.\scripts\agent\verify.ps1 -Full
```

Use `-Plan` when the environment is offline or you only want to see which Gradle tasks would be selected:

```powershell
.\scripts\agent\verify.ps1 -Plan -Path domain\src\main\kotlin\com\topic11\cs426\domain\usecase\ObserveInspectionUseCase.kt
```

## Task Closeout

After each task:

- Do not update `CONTEXT.md` unless durable project state changed.
- Inspect changes with `git status --short` and `git diff --stat`.
- Final response should name changed files, verification command, and any follow-up.

When changing this agent environment, check `SOURCES.md` first and record any meaningful source comparison in the change note or final response.
