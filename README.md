# FieldFlow

FieldFlow is an Android inspection-workflow project for asset and facility inspections. The repository is a functional prototype: it contains a complete Domain workflow, a Circuit-based inspection UI, and an implemented Room/Data foundation. The app runtime still uses deterministic in-memory demo repositories while the Room-backed graph waits for composition-root integration.

Architecture: **Circuit-Based Feature-Modular Clean Architecture**.

Project proposal: [docs/FieldFlow_Project_Proposal.pdf](docs/FieldFlow_Project_Proposal.pdf). The checked-in source and architecture docs, not the proposal, describe current implementation.

## Current State

Implemented:

- multi-module Android architecture with `:app` as the composition root;
- pure Kotlin `:domain` models, ports, validation, scoring, inspection lifecycle, draft, completion, issue, reporting, and scheduling use cases;
- Slack Circuit screens, presenter/UI factories, Dashboard, and an editable Inspection workflow with draft save, validation, review, completion, notes, and evidence references;
- Room database version 2 with exported schemas, explicit migration, DAOs, draft recovery and pending-sync tests;
- Data-layer Room summary repository/mapping, sample-data seeder, Android-managed evidence storage, and deterministic fake remote-sync adapter;
- deterministic `DemoInspectionRepository`, `DemoTemplateRepository`, and `DemoIssueRepository` used by the current app composition;
- unit coverage for Domain, Data, Room database, Dashboard, Inspection, and Reports; plus Compose navigation smoke tests.

Not yet integrated or implemented end-to-end:

- wiring Room-backed repositories, database seeding, evidence storage, and sync into `FieldFlowCompositionRoot`;
- complete Room implementations for every Domain port and real background/remote synchronization;
- asset, template, and issue management UIs;
- report history and PDF/JSON export adapters;
- settings, authentication, and backend integration.

## Module Graph

```text
:app
  -> :data, :domain, :core:database, :core:navigation, :core:designsystem
  -> :feature:dashboard, :feature:inspection, :feature:assets
  -> :feature:templates, :feature:issues, :feature:reports

:feature:* -> :domain, :core:navigation, :core:designsystem
:data      -> :domain, :core:database
:core:database -> Room Android APIs
:core:testing -> :domain (test-only consumer dependency)
:domain is pure Kotlin/JVM
```

Feature modules must not depend on `:data` or `:core:database`. `:domain` must not import Android, Compose, Circuit, Room, app, feature, or data code. See [docs/architecture/MODULE_GRAPH.md](docs/architecture/MODULE_GRAPH.md) for the full boundary rules.

## Runtime Flow

```text
DashboardUi -> DashboardPresenter -> ObserveInspectionSummariesUseCase
    -> InspectionRepository -> DemoInspectionRepository (current app binding)

InspectionUi -> InspectionPresenter -> draft/validate/complete Domain use cases
    -> DemoInspectionRepository + DemoTemplateRepository + DemoIssueRepository

Room/Data foundation -> `:core:database` DAOs -> `:data` mappings/adapters
    -> ready for composition-root integration
```

## Build Prerequisites

- JDK 21 (`gradle/gradle-daemon-jvm.properties` pins toolchain 21).
- Android SDK platform `android-36.1`.
- Gradle wrapper `9.4.1`.
- AGP `9.2.1`, Kotlin `2.2.10`, Compose BOM `2026.02.01`, Room `2.8.4`, and Circuit `0.33.1`.

## Commands

PowerShell:

```powershell
.\gradlew.bat projects --no-daemon
.\gradlew.bat :domain:test :data:testDebugUnitTest :feature:dashboard:testDebugUnitTest :feature:inspection:testDebugUnitTest :feature:reports:testDebugUnitTest --no-daemon
.\gradlew.bat lintDebug test assembleDebug --no-daemon
.\gradlew.bat connectedDebugAndroidTest --no-daemon
```

Use `scripts/agent/verify.ps1` to select the narrowest repository-approved check:

```powershell
.\scripts\agent\verify.ps1 -Path README.md docs\architecture\MODULE_GRAPH.md
.\scripts\agent\verify.ps1 -Full
```

## Team Ownership

| Module or area | Normal approver | Responsibility |
| --- | --- | --- |
| root Gradle, `:app`, `:core:navigation`, `:feature:inspection` | Thang | integration, Circuit foundation, composition root, inspection UI |
| `:domain` | Huy | business contracts, validation, scoring, lifecycle, tests |
| `:data`, `:core:database` | Linh | persistence, adapters, mappings, evidence, sync, database tests |
| `:feature:dashboard`, `:feature:reports`, `:core:designsystem`, docs/demo | Linh | dashboard, reports boundary, design system, documentation |
| `:feature:assets`, `:feature:templates`, `:feature:issues` | Assigned later | future feature UI boundaries |

Detailed ownership rules: [docs/architecture/TEAM_OWNERSHIP.md](docs/architecture/TEAM_OWNERSHIP.md).

## Related Docs

- [Data schema](docs/architecture/DATA_SCHEMA.md)
- [Presentation structure](docs/architecture/PRESENTATION_STRUCTURE.md)
- [Demo script](docs/demo/DEMO_SCRIPT.md)
- [Manual test checklist](docs/demo/MANUAL_TEST_CHECKLIST.md)
