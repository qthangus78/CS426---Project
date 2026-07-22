# FieldFlow

FieldFlow is an Android inspection workflow project for asset and facility inspections. The current repository status is **Architecture Bootstrap**, not a finished product.

Architecture name: **Circuit-Based Feature-Modular Clean Architecture**.

Project proposal: [docs/FieldFlow_Project_Proposal.pdf](docs/FieldFlow_Project_Proposal.pdf)

## Current State

Implemented:

- multi-module Android architecture;
- pure Kotlin `:domain` bootstrap with inspection summary models, repository port, and observe use cases;
- `:data` fake inspection repository with deterministic sample inspection summaries;
- Slack Circuit application shell, typed screens, Presenter/UI factories, and manual composition root in `:app`;
- polished Dashboard presentation with a derived continue-inspection hero, overview metrics, status filters, quick actions, and a local About FieldFlow dialog;
- Dashboard-to-Inspection read-only vertical slice;
- navigable placeholder boundaries for Assets, Templates, Issues, and Reports, with Reports presented as an honest future-milestone screen;
- focused unit tests for Domain, Data, Dashboard Presenter, Inspection Presenter, and Reports Presenter;
- Compose instrumentation smoke coverage for startup, Dashboard, Inspection, placeholder navigation, and Back.

Placeholder:

- Assets;
- Templates;
- Issues;
- Reports export and report-history workflow;
- full Inspection checklist workflow.

Not implemented:

- Settings screen, settings navigation, or persisted preferences;
- Room database, DAOs, entities, migrations, and production local source of truth;
- offline-first production persistence, draft recovery, and synchronization queue;
- inspection validation, weighted scoring, evidence capture, and evidence storage;
- maintenance issue lifecycle;
- asset management and template editing;
- PDF or JSON report exporter implementation;
- backend integration or authentication.

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

Dependency rules:

- feature modules must not depend on `:data`;
- feature modules must not import `FakeInspectionRepository`, DAOs, Room entities, file adapters, or report exporters;
- `:domain` must not import Android, Compose, Circuit, Room, app, feature, or data code;
- `:app` is the composition root and the only module that assembles concrete implementations.

More detail: [docs/architecture/MODULE_GRAPH.md](docs/architecture/MODULE_GRAPH.md)

## Working Flow

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

The Dashboard shows deterministic fake inspection summaries and navigates to a read-only Inspection placeholder. Quick access routes open placeholder boundaries only; they do not claim asset, template, issue, or report functionality is complete.

## Build Prerequisites

- JDK 21. The repository contains `gradle/gradle-daemon-jvm.properties` with `toolchainVersion=21`.
- Android SDK with platform `android-36.1`.
- Gradle wrapper `9.4.1`.
- AGP `9.2.1`, Kotlin `2.2.10`, Compose BOM `2026.02.01`.
- Slack Circuit `0.33.1`, selected for compatibility with the current Kotlin toolchain.

If Gradle cannot write to the user Gradle cache in a restricted environment, use a workspace-local cache:

```bash
env GRADLE_USER_HOME=.gradle ./gradlew projects --no-daemon
```

## Commands

Project discovery:

```bash
env GRADLE_USER_HOME=.gradle ./gradlew projects --no-daemon
```

Clean build:

```bash
env GRADLE_USER_HOME=.gradle ./gradlew clean --no-daemon --stacktrace
```

Unit tests:

```bash
env GRADLE_USER_HOME=.gradle ./gradlew \
  :domain:test \
  :data:testDebugUnitTest \
  :feature:dashboard:testDebugUnitTest \
  :feature:inspection:testDebugUnitTest \
  :feature:reports:testDebugUnitTest \
  --no-daemon \
  --stacktrace
```

Full local validation:

```bash
env GRADLE_USER_HOME=.gradle ./gradlew lintDebug test assembleDebug --no-daemon --stacktrace
```

Install and connected instrumentation tests:

```bash
env GRADLE_USER_HOME=.gradle ./gradlew :app:installDebug --no-daemon --stacktrace
env GRADLE_USER_HOME=.gradle ./gradlew connectedDebugAndroidTest --no-daemon --stacktrace
```

## Team Ownership

| Module or area | Normal approver | Responsibility |
| --- | --- | --- |
| root Gradle, `:app`, `:core:navigation` | Thắng | Circuit foundation, composition root, integration |
| `:feature:inspection` | Thắng | Inspection presentation slice |
| `:domain` | Huy | Domain models, use cases, business rules, tests |
| `:data`, `:core:database` | Lĩnh | Repositories, future Room, mappings, offline-first behavior |
| `:feature:dashboard`, `:feature:reports`, `:core:designsystem`, README/docs/demo | Linh | Dashboard, Reports UI boundary, design system, documentation |
| `:feature:assets`, `:feature:templates`, `:feature:issues` | Assigned later | Placeholder boundaries for future feature owners |

Detailed ownership rules: [docs/architecture/TEAM_OWNERSHIP.md](docs/architecture/TEAM_OWNERSHIP.md)

## Demo Docs

- [docs/demo/DEMO_SCRIPT.md](docs/demo/DEMO_SCRIPT.md)
- [docs/demo/MANUAL_TEST_CHECKLIST.md](docs/demo/MANUAL_TEST_CHECKLIST.md)
- [docs/architecture/PRESENTATION_STRUCTURE.md](docs/architecture/PRESENTATION_STRUCTURE.md)
