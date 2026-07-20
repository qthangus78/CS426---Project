# FieldFlow Agent Context Pack (v0.2)

## Project Snapshot

FieldFlow is an Android multi-module app for inspection workflows.

Core shape:

- `app` assembles concrete dependencies;
- `domain` holds business rules and ports;
- `data` implements repositories and adapters;
- `core/*` holds shared navigation, UI, database, and test helpers;
- `feature/*` holds presentation slices.

Current bootstrap status:

- the working vertical slice is Dashboard to read-only Inspection;
- data uses deterministic fake repositories;
- Room, offline sync, evidence storage, and report export are target architecture items, not completed implementation.

## Default Read Set

Use this set for most tasks:

- `README.md`
- `docs/FieldFlow_Project_Proposal.pdf`
- `docs/architecture/MODULE_GRAPH.md`
- `docs/architecture/TEAM_OWNERSHIP.md`
- `AGENTS.md`
- this file
- `docs/agent/EVALUATION.md`

For agent-environment changes, also read `docs/agent/SOURCES.md`.

## Task Template

```text
Goal:
Context:
Owner module:
Allowed files:
Forbidden files:
Expected behavior:
Constraints:
Verification:
Done when:
```

## Task-Specific Read Set

Domain work:

- `domain/src/main/**`
- `domain/src/test/**`
- `core/testing/**` when fixtures or fake ports are needed

Data work:

- `data/src/main/**`
- `data/src/test/**`
- `core/database/**`

Inspection or app integration:

- `feature/inspection/**`
- `core/navigation/**`
- `app/**`

Dashboard or report UI:

- `feature/dashboard/**`
- `feature/reports/**`
- `core/designsystem/**`

Agent environment work:

- `AGENTS.md`
- `docs/agent/**`
- `scripts/agent/**`
- official sources listed in `docs/agent/SOURCES.md`

## Stop Conditions

Stop and reassess if:

- the task crosses module boundaries without a settled contract;
- a change would leak framework types into `domain`;
- a new folder name feels generic rather than capability-based;
- the verification scope is unclear.
- an external source conflicts with current repo docs. In that case, preserve current repo behavior and record the source gap.

## Useful Extra Ideas

- Keep one short ADR for non-trivial architecture changes.
- Put a small checklist in every task when the change is cross-module.
- Use subagents for planning, implementation, and verification instead of one large prompt.
- Prefer change-specific context over dumping the entire repo every time.
