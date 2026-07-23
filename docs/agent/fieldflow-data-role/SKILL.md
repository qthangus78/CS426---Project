---
name: fieldflow-data-role
description: Implement, review, or plan work owned by the FieldFlow Data role in `:data` and `:core:database`, including fake repositories, Room persistence, DAOs, mappings, local source of truth, draft recovery, evidence storage, pending synchronization, fake remote synchronization, migrations, repository tests, and app composition handoff. Use for any FieldFlow task assigned to the Data owner or touching data-layer implementations and persistence boundaries.
---

# FieldFlow Data Role

Own Data-layer work without leaking persistence or adapter details into Domain or feature modules. Treat repository docs and current code as the source of truth; treat the proposal as future scope until a milestone explicitly enables it.

## Establish Scope

1. Read `AGENTS.md`, `README.md`, `docs/architecture/MODULE_GRAPH.md`, `docs/architecture/TEAM_OWNERSHIP.md`, `docs/agent/CONTEXT.md`, and `docs/agent/EVALUATION.md`.
2. Inspect `data/src/main/**`, `data/src/test/**`, `core/database/**`, and the relevant repository ports in `domain/src/main/**`.
3. Identify the approved milestone. Do not add Room, file storage, PDF, networking, synchronization, or authentication merely because the proposal mentions it.
4. Write a compact brief with Goal, Owner module, Allowed files, Required contract, Expected behavior, Verification, and Done when.
5. Stop and request a contract decision when Data cannot implement the behavior through an existing Domain port.

## Preserve Boundaries

- Keep business rules and repository ports in `:domain`.
- Keep repository implementations, mappings, orchestration, and storage adapters in `:data`.
- Keep Room databases, entities, DAOs, and migrations in `:core:database`.
- Make `:data` depend inward on `:domain`; never make Domain depend on Data.
- Never let a feature import Data implementations, Room entities, DAOs, file adapters, or sync adapters.
- Assemble concrete implementations only in `:app`.
- Do not import `FakeInspectionRepository` outside `:app` or `:data` tests.
- Do not expose Android, Room, file-path, worker, or network types through Domain contracts.
- Prefer capability-specific packages and names; do not create `utils`, `helpers`, `common`, `services`, or `misc`.

## Execute Data Tasks

Select only the task or smallest connected task set authorized by the current milestone.

### Task 1 - Maintain Fake Repositories

1. Implement the existing Domain repository port with deterministic sample data.
2. Use stable IDs and predictable ordering so demos and tests remain repeatable.
3. Return reactive data through the contract's `Flow` APIs.
4. Cover list emission, lookup success, lookup failure, and any state transition introduced by the contract.
5. Avoid production persistence, clocks, random values, networking, and Android storage.

Done when the fake satisfies the current Domain contract and `:data` tests prove deterministic behavior.

### Task 2 - Design Persistence Models

1. Derive storage needs from approved Domain behavior, not directly from UI state.
2. Define Room entities with stable keys, relationships, indexes, and explicit persisted status values.
3. Keep entities separate from Domain models; do not add Room annotations to Domain.
4. Decide transaction boundaries for aggregate writes such as inspection, answers, evidence metadata, and pending sync records.
5. Record a short ADR or change note when schema ownership or lifecycle decisions are non-obvious.

Done when the schema supports the approved use cases without leaking database structure through repository ports.

### Task 3 - Set Up Room and DAOs

1. Add Room only after the persistence milestone is approved.
2. Place the database, entities, DAOs, converters, and migrations in `:core:database`.
3. Expose observable reads as `Flow` and provide the minimum write operations required by repository implementations.
4. Use database transactions for multi-table state changes that must be atomic.
5. Test queries, relationships, replacement behavior, and transaction outcomes with an in-memory database.

Done when DAO behavior is verified independently and `:core:database` has no app or feature dependency.

### Task 4 - Implement Mappings

1. Put persistence-to-Domain and Domain-to-persistence mappings in `:data` unless database-only conversion is clearly internal to `:core:database`.
2. Map every field explicitly, including IDs, nullable values, enum/status values, timestamps, and child collections.
3. Define safe behavior for unknown persisted enum values and malformed legacy data.
4. Keep mapping functions narrow and capability-named.
5. Add round-trip or direction-specific tests for meaningful mappings.

Done when Room models cannot escape into Domain or feature callers and mapping edge cases are tested.

### Task 5 - Implement the Production Repository

1. Implement the Domain port in `:data` using DAO and adapter dependencies supplied to the constructor.
2. Make Room the canonical source observed by upper layers.
3. Perform writes locally inside the required transaction, then let database changes drive new `Flow` emissions.
4. Keep validation and scoring in Domain use cases; Data handles persistence and adapter coordination only.
5. Test repository behavior with controlled DAO/database dependencies.

Done when callers can swap fake and production implementations without changing Domain, presenters, or UI.

### Task 6 - Support Local Source of Truth

1. Read from Room through one repository data path.
2. Write locally before scheduling or simulating remote work.
3. Never ask UI code to reconcile database and remote results.
4. Preserve useful local data when remote operations fail.
5. Test offline reads, local writes, and subsequent reactive emissions.

Done when the core workflow remains usable without connectivity and exposes one consistent state.

### Task 7 - Persist and Recover Drafts

1. Persist answers, progress, current section, lifecycle state, and recovery metadata required by the Domain contract.
2. Save aggregate state atomically where partial writes would create an invalid draft.
3. Reconstruct the Domain model through mappings when the app restarts.
4. Define handling for missing, obsolete, or invalid drafts with the Domain owner.
5. Test process-restart recovery using newly created repository/database instances.

Done when a user can resume the same approved draft state after application recreation.

### Task 8 - Implement Evidence Storage

1. Implement the Domain evidence port in an outer Android-aware layer owned by Data.
2. Copy selected content into app-managed storage and persist stable metadata or references.
3. Coordinate file and database operations so failures do not leave silent broken references.
4. Define deletion, cleanup, duplicate, missing-file, and permission-failure behavior.
5. Test metadata mapping and failure paths; use scoped integration tests for filesystem behavior.

Done when Domain and features operate on evidence references without knowing file paths or Android storage APIs.

### Task 9 - Model Pending Synchronization

1. Persist synchronization commands or records with stable IDs and explicit states.
2. Support `Pending -> Syncing -> Synced` and `Pending -> Syncing -> Failed -> Retry`.
3. Make retries idempotent and preserve local data across failure or process death.
4. Store only the payload/reference needed to reproduce an approved operation.
5. Test state transitions, duplicate prevention, failure, and restart recovery.

Done when pending work is durable, observable, and safe to retry.

### Task 10 - Implement Fake Synchronization

1. Add a deterministic adapter that can simulate success, delay, and failure without a real backend.
2. Inject outcomes or a scenario policy; do not use randomness in tests or demos.
3. Update durable sync state through repository/database operations.
4. Keep scheduling and Android worker details outside Domain.
5. Test success, failure, retry, and repeated execution.

Done when offline/sync demonstrations are repeatable and require no network dependency.

### Task 11 - Add and Evolve Migrations

1. Increase the database version for every released schema change.
2. Write an explicit migration that preserves valid user data.
3. Test migration from every supported prior schema version to the current version.
4. Avoid destructive migration except for disposable demo data and only with explicit approval.
5. Document any irreversible transformation or fallback behavior.

Done when supported existing databases open successfully and retain expected records.

### Task 12 - Integrate at the Composition Root

1. Coordinate with the App owner before changing `:app`.
2. Construct database, DAOs, adapters, and repository implementations in the composition root.
3. Replace the fake binding only for the approved runtime mode; retain deterministic fake mode when required for demos/tests.
4. Keep feature constructors dependent on Domain ports or use cases, not concrete Data classes.
5. Run full verification because app wiring crosses the integration boundary.

Done when implementation swapping changes only composition and both configured modes build.

### Task 13 - Verify and Hand Off

1. Add or update tests in the same owning module for every behavior change.
2. Run `./gradlew :data:testDebugUnitTest --no-daemon` for `:data`-only work.
3. Use `scripts/agent/verify.ps1 -Path <changed-files>` to select the narrowest repository-approved checks.
4. Run the full Android build and lint set when changing app wiring, Gradle, navigation, or shared contracts.
5. If dependencies cannot be downloaded, run `scripts/agent/verify.ps1 -Plan -Path <changed-files>` and report the selected commands.
6. Inspect the diff for boundary leaks, accidental generated files, unrelated edits, and proposal-only scope expansion.

Done when the scoped checks pass, the diff is reviewable, and the handoff distinguishes completed behavior from future tasks.

## Resolve Cross-Module Changes

When an existing Domain port is insufficient:

1. Describe the smallest capability missing from the port.
2. Agree on Domain types and failure semantics with the Domain owner.
3. Change and test the contract in `:domain` before implementing it in `:data`.
4. Update all implementations and composition wiring.
5. Request review from both Domain and Data owners; include the App owner when wiring changes.

Do not widen a repository into a generic gateway. Add only operations required by an approved use case.

## Finish Checklist

- Confirm current implementation and proposal target are clearly distinguished.
- Confirm features do not depend on `:data` or `:core:database`.
- Confirm Domain contains no Android, Room, Circuit, Compose, file, worker, or network details.
- Confirm local writes and failure paths cannot silently lose user data.
- Confirm deterministic fakes remain deterministic.
- Confirm behavior changes have tests and the correct verification command was run or planned.
- Report changed modules, completed task numbers, verification results, and remaining milestone-dependent work.
