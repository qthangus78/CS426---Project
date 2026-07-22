# FieldFlow

FieldFlow is an Android inspection workflow project for asset and facility inspections. The current repository status is **Architecture Bootstrap**, not a finished product.

Architecture name: **FIELDFlow Architecture Bootstrap**

This milestone creates a clean multi-module foundation with Domain contracts, fake Data, Circuit presentation contracts, typed navigation, manual dependency injection, placeholder feature screens, and a small Dashboard-to-Inspection vertical slice.

Room and offline-first persistence are planned but not implemented in the Architecture Bootstrap.

Project proposal: [docs/FieldFlow_Project_Proposal.pdf](docs/FieldFlow_Project_Proposal.pdf)

## Module Graph

```text
:app
  -> :data
  -> :domain
  -> :core:navigation
  -> :core:designsystem
  -> :feature:dashboard
  -> :feature:inspection
  -> :feature:assets
  -> :feature:templates
  -> :feature:issues
  -> :feature:reports

:feature:* -> :domain, :core:navigation, :core:designsystem
:data      -> :domain
:core:database has no app or feature dependency
:domain is pure Kotlin/JVM
:core:testing is pure Kotlin/JVM and test-only from consumers
```

Forbidden boundaries:

- feature modules must not depend on `:data`;
- feature modules must not import `FakeInspectionRepository`, DAOs, Room entities, or file adapters;
- `:domain` must not import Android, Compose, Circuit, Room, app, feature, or data packages;
- `:app` is the composition root and the only module that assembles concrete implementations.

More detail: [docs/architecture/MODULE_GRAPH.md](docs/architecture/MODULE_GRAPH.md)

## Current Working Flow

The working bootstrap slice is:

```text
DashboardUi
-> DashboardPresenter
-> ObserveInspectionSummariesUseCase
-> InspectionRepository
-> FakeInspectionRepository
-> DashboardState
-> DashboardUi
-> Navigator
-> InspectionScreen
-> InspectionPresenter
-> ObserveInspectionUseCase
-> InspectionUi
```

The Dashboard shows deterministic fake inspection summaries and navigates to a read-only Inspection placeholder. Assets, Templates, Issues, and Reports are navigable placeholders only.

## Build Prerequisites

- JDK 21. The repository contains `gradle/gradle-daemon-jvm.properties` with `toolchainVersion=21`.
- Android SDK with platform `android-36.1`.
- Gradle wrapper `9.4.1`.
- AGP `9.2.1`, Kotlin `2.2.10`, Compose BOM `2026.02.01`.
- Slack Circuit `0.33.1`, selected as the newest compatible stable release for this Kotlin toolchain.

If Gradle cannot write to the user Gradle cache in a restricted environment, use a workspace-local cache:

```bash
GRADLE_USER_HOME=.gradle ./gradlew projects --no-daemon
```

## Build Commands

```bash
./gradlew projects --no-daemon
./gradlew lintDebug test assembleDebug --no-daemon --stacktrace
./gradlew :domain:test --no-daemon
./gradlew :data:testDebugUnitTest --no-daemon
./gradlew connectedDebugAndroidTest --no-daemon --stacktrace
```

## Team Ownership

| Module or area | Normal approver | Responsibility |
| --- | --- | --- |
| root Gradle, `:app`, `:core:navigation` | Thắng | Circuit foundation, composition root, integration |
| `:feature:inspection` | Thắng | Inspection presentation slice |
| `:domain` | Huy | Domain models, use cases, business rules, tests |
| `:data`, `:core:database` | Lĩnh | Repositories, future Room, mappings, offline-first behavior |
| `:feature:dashboard`, `:feature:reports`, `:core:designsystem`, docs | Linh | Dashboard, reports UI boundary, design system, documentation |
| `:feature:assets`, `:feature:templates`, `:feature:issues` | Assigned later | Placeholder boundaries for future feature owners |

Detailed ownership rules: [docs/architecture/TEAM_OWNERSHIP.md](docs/architecture/TEAM_OWNERSHIP.md)

## Agent Environment

For AI-agent work, use the v0.2 repo instructions and harness files in this order:

1. [AGENTS.md](AGENTS.md)
2. [docs/agent/README.md](docs/agent/README.md)
3. [docs/agent/CONTEXT.md](docs/agent/CONTEXT.md)
4. [docs/agent/EVALUATION.md](docs/agent/EVALUATION.md)
5. [docs/agent/SOURCES.md](docs/agent/SOURCES.md)
6. [scripts/agent/verify.ps1](scripts/agent/verify.ps1)

The idea is to keep agent work contract-first, scoped to the owning module, and verified with the narrowest useful check before falling back to a full build.

When updating the agent environment itself, compare against the official sources in `docs/agent/SOURCES.md` and keep `AGENTS.md` concise enough to stay useful across future tasks.
