# FieldFlow Agent Context Pack (v0.3)

## Project Snapshot

FieldFlow is a multi-module Android inspection workflow app.

- `:app` composes the current runtime, which opens Room with migrations, schedules sample-data seeding asynchronously, binds Room-backed repositories, and owns Android open/share wiring for report exports.
- `:domain` is pure Kotlin and contains the inspection workflow contracts and rules.
- `:data` implements the outer data boundary: Room mapping/repository foundations, seeding, evidence storage, and fake sync.
- `:core:database` owns Room version 3, entities, DAOs, migrations, schemas, and database tests.
- `:feature:inspection` provides an editable draft/review/validate/complete workflow.
- Assets, Templates, Issues, and Reports now have Room-backed product workflows. Backend sync, authentication, full template aggregate authoring, and scheduled/cloud reporting remain out of scope.

Do not leak Room/Data implementation details into feature modules. `FieldFlowCompositionRoot` is the only place allowed to assemble app runtime repository bindings, and `FieldFlowApplication` owns it for the process lifetime. Repository-adapter selection is not a product UI mode.

## Default Read Set

- `README.md`
- `AGENTS.md`
- `docs/architecture/MODULE_GRAPH.md`
- `docs/architecture/TEAM_OWNERSHIP.md`
- this file
- `docs/agent/EVALUATION.md`

Add `core/database/README.md` and `docs/architecture/DATA_SCHEMA.md` for data or persistence work. Read proposal material only when the task needs future product intent rather than current behavior.

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

Domain work: `domain/src/main/**`, `domain/src/test/**`, and relevant `core/testing/**` fixtures.

Data or database work: `data/src/main/**`, `data/src/test/**`, `core/database/**`, and affected Domain ports/use cases.

Inspection or app integration: `feature/inspection/**`, `core/navigation/**`, `app/**`, and the contracts consumed by the slice.

Dashboard or report UI: `feature/dashboard/**`, `feature/reports/**`, and `core/designsystem/**`.

Agent-environment work: `AGENTS.md`, `docs/agent/**`, `scripts/agent/**`, and official sources in `docs/agent/SOURCES.md`.

## Stop Conditions

Stop and reassess if a task crosses module boundaries without a settled contract, leaks framework types into Domain, introduces a generic folder, duplicates an existing persistence capability, or treats a placeholder UI as implemented business functionality.
