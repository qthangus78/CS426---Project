# FieldFlow

FieldFlow is an Android inspection-workflow project for asset and facility inspections. The repository is a functional prototype: it contains a complete Domain workflow, a Circuit-based inspection UI, and an app runtime backed by the implemented Room/Data foundation.

Architecture: **Circuit-Based Feature-Modular Clean Architecture**.

Project proposal: [docs/FieldFlow_Project_Proposal.pdf](docs/FieldFlow_Project_Proposal.pdf). The checked-in source and architecture docs, not the proposal, describe current implementation.

## Current State

Implemented:

- multi-module Android architecture with `:app` as the composition root;
- pure Kotlin `:domain` models, ports, validation, scoring, inspection lifecycle, draft, completion, issue, reporting, and scheduling use cases;
- Slack Circuit screens, presenter/UI factories, Dashboard, and an editable Inspection workflow with draft save, validation, review, completion, notes, and evidence references;
- Room-backed Assets workflow with list, detail, add/edit, location association, validation, persistence, and start-inspection handoff;
- Room-backed Locations workflow with list, search, detail, add/edit, validation, persistence, and safe non-destructive handling for asset associations;
- Room-backed Templates workflow with list, detail, section/checklist display, add template with an initial checklist item, metadata editing that preserves existing sections/items, validation, persistence, and start-inspection handoff;
- Room-backed Issues workflow with list filters, detail, Domain-validated lifecycle transitions, persisted status updates, and inspection/asset context;
- Room-backed Reports workflow with completed-inspection candidates, generated report detail, persisted export history, JSON export, PDF export, and app-layer open/share actions;
- Settings workflow with System, Light, and Dark appearance modes persisted through an app-layer preference adapter and applied at the app theme root;
- Room database version 3 with exported schemas, explicit migrations, DAOs, draft recovery, report-history, and pending-sync tests;
- Data-layer Room summary repository/mapping, sample-data seeder, Android-managed evidence storage, report file exporters, and deterministic fake remote-sync adapter;
- `FieldFlowCompositionRoot` opens `FieldFlowDatabase` with migrations, schedules sample-data seeding asynchronously, and binds Room-backed inspection, template, asset, issue, and report repositories;
- unit coverage for Domain, Data, Room database, Dashboard, Assets, Templates, Inspection, Issues, and Reports; plus Compose navigation smoke tests.

Remaining future scope:

- runtime wiring for fake/background synchronization;
- full multi-section template authoring and active/archive lifecycle controls for assets/templates;
- authentication, backend integration, report scheduling, production remote synchronization, notifications, and cloud delivery.

## Module Graph

```text
:app
  -> :data, :domain, :core:database, :core:navigation, :core:designsystem
  -> :feature:dashboard, :feature:inspection, :feature:assets
  -> :feature:locations, :feature:templates, :feature:issues, :feature:reports
  -> :feature:settings

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
    -> InspectionRepository -> RoomInspectionRepository -> FieldFlowDatabase

InspectionUi -> InspectionPresenter -> draft/validate/complete Domain use cases
    -> RoomInspectionRepository + RoomTemplateRepository + RoomAssetRepository

IssuesUi -> IssuesPresenter -> issue Domain use cases
    -> IssueRepository -> RoomIssueRepository -> FieldFlowDatabase

ReportsUi -> ReportsPresenter -> report Domain use cases
    -> Room repositories + JSON/PDF exporters + app open/share bridge

SettingsUi -> SettingsPresenter -> appearance preference use cases
    -> app preference adapter -> FieldFlowTheme

Room/Data foundation -> `:core:database` DAOs -> `:data` mappings/adapters
    -> integrated in `FieldFlowCompositionRoot` for product runtime
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
.\gradlew.bat :domain:test :data:testDebugUnitTest :core:database:testDebugUnitTest :app:testDebugUnitTest :feature:dashboard:testDebugUnitTest :feature:assets:testDebugUnitTest :feature:locations:testDebugUnitTest :feature:templates:testDebugUnitTest :feature:inspection:testDebugUnitTest :feature:issues:testDebugUnitTest :feature:reports:testDebugUnitTest :feature:settings:testDebugUnitTest --no-daemon
.\gradlew.bat lintDebug test assembleDebug --no-daemon
.\gradlew.bat connectedDebugAndroidTest --no-daemon
.\gradlew.bat :app:ciDeviceDebugAndroidTest --no-daemon
```

`connectedDebugAndroidTest` needs an emulator or device you started yourself. `:app:ciDeviceDebugAndroidTest`
runs the same suite on the Gradle-managed device declared in `app/build.gradle.kts` (Pixel 6, API 35,
`aosp-atd`), downloading and booting it on demand — this is the exact device CI uses, so a local pass and
a CI pass mean the same thing. Reports land in `app/build/reports/androidTests/managedDevice/`.

Use `scripts/agent/verify.ps1` to select the narrowest repository-approved check:

```powershell
.\scripts\agent\verify.ps1 -Path README.md docs\architecture\MODULE_GRAPH.md
.\scripts\agent\verify.ps1 -Full
```

## Continuous Integration

`.github/workflows/android-ci.yml` runs on every push and pull request to `main`, in two parallel jobs:

| Job | What it runs | Why |
| --- | --- | --- |
| Build and lint | `lintDebug test assembleDebug :app:assembleDebugAndroidTest` | Fast gate. Includes compiling the instrumented suite so a test that no longer builds fails in minutes, not after an emulator boot. |
| Instrumented tests | `:app:ciDeviceDebugAndroidTest` on the managed device | Runs the navigation smoke tests on a real emulator, so regressions in the app runtime cannot land unnoticed. |

The instrumented job caches the system image and AVD, and uploads its HTML report as a build artifact on
every run so a failure can be read without re-running the job.

## Team Ownership

| Module or area | Normal approver | Responsibility |
| --- | --- | --- |
| root Gradle, `:app`, `:core:navigation`, `:feature:inspection` | Thang | integration, Circuit foundation, composition root, inspection UI |
| `:domain` | Huy | business contracts, validation, scoring, lifecycle, tests |
| `:data`, `:core:database` | Linh | persistence, adapters, mappings, evidence, sync, database tests |
| `:feature:dashboard`, `:feature:reports`, `:core:designsystem`, docs/demo | Linh | dashboard, reports workflow, design system, documentation |
| `:feature:assets`, `:feature:locations`, `:feature:templates`, `:feature:issues`, `:feature:settings` | Assigned later | assets, locations, templates, issue lifecycle, and settings workflows |

Detailed ownership rules: [docs/architecture/TEAM_OWNERSHIP.md](docs/architecture/TEAM_OWNERSHIP.md).

## Related Docs

- [Data schema](docs/architecture/DATA_SCHEMA.md)
- [Presentation structure](docs/architecture/PRESENTATION_STRUCTURE.md)
- [Demo script](docs/demo/DEMO_SCRIPT.md)
- [Manual test checklist](docs/demo/MANUAL_TEST_CHECKLIST.md)
