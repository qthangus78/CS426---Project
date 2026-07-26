# FieldFlow Clean Architecture Seminar Guide

## 1. What FieldFlow Is

FieldFlow is an offline-first Android inspection and maintenance app for facilities, labs, classrooms, and equipment. The intended users are inspectors, maintenance coordinators, and students demonstrating an architecture that can support real inspection workflows.

The main workflow is:

1. Choose an asset or location.
2. Select an inspection template.
3. Complete checklist items with pass, fail, not-applicable, measured values, notes, and evidence references.
4. Save a draft, review validation, complete the inspection, calculate a score, create maintenance issues for failures, and prepare report data.

The domain is meaningful because the app is not just CRUD. It owns rules such as required checklist answers, critical-failure evidence, weighted scoring, lifecycle status, next inspection scheduling, and report eligibility.

## 2. Architecture Name

FieldFlow uses **Circuit-Based Feature-Modular Clean Architecture**.

This is a combination of complementary patterns:

- Clean Architecture defines system boundaries and dependency direction.
- Feature modularization enforces ownership and compile-time isolation.
- Slack Circuit structures the Presentation Layer.
- Offline-first Data adapters handle Room, persistence, file storage, and synchronization concerns.

Circuit is not a replacement for Clean Architecture. Circuit organizes screens, presenters, state, events, and Compose UI inside the outer Presentation Layer. Clean Architecture still controls which layer owns business rules and which layer may depend on implementation details.

## 3. High-Level Module Diagram

```mermaid
flowchart TD
    app[":app\ncomposition root"]

    dashboard[":feature:dashboard"]
    assets[":feature:assets"]
    templates[":feature:templates"]
    inspection[":feature:inspection"]
    issues[":feature:issues"]
    reports[":feature:reports"]

    navigation[":core:navigation\nScreen contracts"]
    designsystem[":core:designsystem\nshared UI"]
    testing[":core:testing\ntest fixtures"]

    domain[":domain\nmodels, use cases, ports"]
    data[":data\nrepository adapters, seeding, sync, evidence storage"]
    database[":core:database\nRoom entities, DAOs, migrations"]

    app --> dashboard
    app --> assets
    app --> templates
    app --> inspection
    app --> issues
    app --> reports
    app --> domain

    dashboard --> navigation
    dashboard --> designsystem
    dashboard --> domain

    assets --> navigation
    assets --> designsystem

    templates --> navigation
    templates --> designsystem

    inspection --> navigation
    inspection --> designsystem
    inspection --> domain

    issues --> navigation
    issues --> designsystem

    reports --> navigation
    reports --> designsystem

    data --> domain
    data --> database
    testing --> domain
```

The current runtime app assembles Room-backed repositories in `app/src/main/java/com/topic11/cs426/FieldFlowCompositionRoot.kt`: `RoomInspectionRepository`, `RoomTemplateRepository`, `RoomAssetRepository`, and `AndroidEvidenceStore`. Sample catalog and inspection data is seeded asynchronously at startup. The root is created and held by `FieldFlowApplication`, so it lives for the whole process and is no longer closed and reopened when the Activity is recreated.

## 4. Dependency Rule

The intended dependency rule is:

```text
Feature -> Domain <- Data
```

Source-code dependencies point inward because Domain owns business policy. Presentation can ask Domain to perform work, and Data can implement Domain ports, but Domain does not import Android, Compose, Circuit, Room, Data, or feature code.

Evidence:

- `domain/build.gradle.kts` is a Kotlin/JVM module, not an Android module.
- `feature/inspection/build.gradle.kts` depends on `:domain`, `:core:navigation`, and `:core:designsystem`, not `:data`.
- `data/build.gradle.kts` depends on `:domain` and `:core:database`.
- `domain/src/main/kotlin/com/topic11/cs426/domain/repository/InspectionRepository.kt` defines the port that both the Room adapter and `FakeInspectionRepository` implement.

## 5. Responsibility of Every Module

`:app` is the composition root. It owns `FieldFlowApplication.kt`, `MainActivity.kt`, `FieldFlowCompositionRoot.kt`, `PendingSyncDrain.kt`, concrete Room repository selection, asynchronous sample seeding, the app-scoped pending-sync drain loop, use-case construction, and Circuit factory registration. `FieldFlowApplication` creates the composition root lazily and holds it for the process lifetime; `MainActivity` only reads it.

`:domain` owns business models, use cases, and repository/export/storage ports. Important files include `InspectionSession.kt`, `InspectionTemplate.kt`, `InspectionScore.kt`, `ValidateInspectionUseCase.kt`, `CompleteInspectionUseCase.kt`, and `GenerateInspectionReportUseCase.kt`.

`:data` owns infrastructure adapters for Domain ports. Important files include `RoomInspectionRepository.kt`, `RoomAssetRepository.kt`, `RoomTemplateRepository.kt`, `RoomIssueRepository.kt`, `RoomReportRepository.kt`, `FieldFlowSampleDataSeeder.kt`, `AndroidEvidenceStore.kt`, report exporters, and `FakeRemoteSyncAdapter.kt`.

`:core:database` owns the Room boundary: `FieldFlowDatabase.kt`, `DatabaseEntities.kt`, DAOs, migrations, schema JSON, and database tests.

`:core:navigation` owns typed Circuit screen contracts in `FieldFlowScreens.kt` so features can navigate without importing each other.

`:core:designsystem` owns shared Compose UI primitives such as `FieldFlowTheme.kt`, `FieldFlowTopAppBar.kt`, `InspectionSummaryCard.kt`, `LoadingContent.kt`, and `EmptyState.kt`.

`:core:testing` owns reusable fakes and fixtures used by tests, such as `RecordingInspectionRepository.kt` and `InspectionTestFixtures.kt`.

`:feature:dashboard` owns Dashboard presentation: `DashboardPresenter.kt`, `DashboardUi.kt`, Circuit factories, and Dashboard-specific components.

`:feature:inspection` owns Inspection presentation: `InspectionPresenter.kt`, `InspectionUi.kt`, contracts, and Circuit factories.

`:feature:assets` owns the Room-backed asset management workflow: list, detail, add/edit, location association, validation, and start-inspection handoff through Domain use cases.

`:feature:locations` owns the Room-backed location management workflow: list/search, detail, add/edit, validation, and Back behavior.

`:feature:templates` owns the Room-backed template workflow: list, detail with sections and checklist items, add template with an initial checklist item, metadata-only editing for existing templates, validation, and start-inspection handoff. Complete multi-section/item authoring remains deferred so existing checklist aggregates are not lost.

`:feature:settings` owns the Appearance settings UI. `:app` owns the Android preference adapter and applies the selected theme mode at the root `FieldFlowTheme`.

`:feature:issues` owns the issue lifecycle workspace: Room-backed issue list, filters, detail, and Domain-validated status transitions.

`:feature:reports` owns the reports workspace: completed-inspection candidates, generated report detail, persisted export history, JSON/PDF export actions, and open/share events.

## 6. One Complete End-to-End Flow

The current runtime inspection completion flow is implemented through Domain use cases and Room repository adapters:

```mermaid
sequenceDiagram
    participant UI as InspectionUi.kt
    participant Presenter as InspectionPresenter.kt
    participant Save as SaveInspectionDraftUseCase
    participant Complete as CompleteInspectionUseCase
    participant Port as InspectionRepository port
    participant Adapter as RoomInspectionRepository
    participant Database as FieldFlowDatabase / InspectionDao

    UI->>Presenter: InspectionEvent.AnswerChanged / NoteChanged / EvidenceAdded
    Presenter->>Presenter: update draft InspectionSession
    UI->>Presenter: InspectionEvent.SaveDraftSelected
    Presenter->>Save: invoke(session)
    Save->>Port: saveDraft(session)
    Port->>Adapter: current app binding
    Adapter->>Database: transaction
    Database-->>Adapter: Flow update
    Adapter-->>Presenter: mapped Domain model
    UI->>Presenter: InspectionEvent.CompleteSelected
    Presenter->>Save: persist latest draft
    Presenter->>Complete: invoke(inspectionId)
    Complete->>Port: getInspection(inspectionId)
    Complete->>Complete: validate, score, schedule next inspection
    Complete->>Complete: create critical failure issues
    Complete->>Port: markCompleted(...)
    Complete-->>Presenter: CompleteInspectionResult.Success
    Presenter-->>UI: InspectionState.Completed
```

The repository path is:

```text
RoomInspectionRepository
-> FieldFlowDatabase
-> InspectionDao / CatalogDao / IssueDao / SyncDao
-> Flow
-> Domain model mapper
```

## 7. Business Rules in Domain

Implemented Domain rules include:

- Required checklist answers in `ValidateInspectionUseCase.kt`.
- Critical-failure evidence requirements in `ValidateInspectionUseCase.kt`.
- Weighted score calculation and Not Applicable exclusion in `CalculateInspectionScoreUseCase.kt`.
- Completion orchestration in `CompleteInspectionUseCase.kt`.
- Maintenance issue creation for failed items in `CreateMaintenanceIssueUseCase.kt`.
- Recurrence scheduling in `ScheduleNextInspectionUseCase.kt`.
- Report eligibility for completed inspections in `GenerateInspectionReportUseCase.kt`.

These rules do not belong in UI, Presenter, DAO, or repository adapters because they are business policy. If they lived in Compose UI, they would be hard to test without Android. If they lived in DAO SQL, the app would duplicate or lose behavior when the storage adapter changes.

Evidence tests live in `domain/src/test/kotlin/com/topic11/cs426/domain/DomainBusinessRulesTest.kt`.

## 8. Ports and Adapters

Domain ports include:

- `InspectionRepository.kt`;
- `TemplateRepository.kt`;
- `AssetRepository.kt`;
- `IssueRepository.kt`;
- `EvidenceStore.kt`;
- `ReportExporter.kt`.

Adapters include:

- `RoomInspectionRepository.kt`, `RoomTemplateRepository.kt`, and `RoomAssetRepository.kt` in `:data` for the current Room-backed runtime.
- `FakeInspectionRepository.kt` and `FakeRemoteSyncAdapter.kt` in `:data` for deterministic tests and sync simulation.
- `AndroidEvidenceStore.kt` and `EvidenceFileStorage.kt` in `:data` for Android file-backed evidence storage infrastructure.

The important point is replacement. `:feature:inspection` uses `CompleteInspectionUseCase`, not `RoomInspectionRepository`. The current Room-backed binding happens in `:app`, with Domain and feature modules unchanged.

## 9. Slack Circuit Presentation

Slack Circuit structures presentation as:

```text
Screen -> Presenter -> immutable UiState -> UiEvent -> Compose UI
```

Example from Inspection:

- Screen: `InspectionScreen` in `core/navigation/src/main/kotlin/com/topic11/cs426/core/navigation/FieldFlowScreens.kt`.
- Presenter: `InspectionPresenter.kt`.
- State/events: `InspectionContracts.kt`.
- UI: `InspectionUi.kt`.
- Factories: `InspectionFactories.kt`.

The Presenter observes Domain flows, calls use cases, reacts to UI events, and emits immutable state. Compose renders state and emits events through `eventSink`; it does not construct repositories or call DAOs.

## 10. Feature-First Modularization

Feature modules own presentation for their slice. This helps FieldFlow because separate team members can work on Dashboard, Inspection, Reports, Issues, Templates, and Assets without sharing one large app package.

The compile-time boundary matters: a feature cannot import `:data` unless its Gradle file adds that dependency. Current feature Gradle files do not do that, so repository implementation details stay outside presentation.

Team ownership is documented in `docs/architecture/TEAM_OWNERSHIP.md`.

## 11. Offline-First Architecture

The offline-first runtime is built around Room as the local source of truth:

- Room local schema: `core/database/src/main/kotlin/com/topic11/cs426/core/database/FieldFlowDatabase.kt`.
- Entities: `core/database/src/main/kotlin/com/topic11/cs426/core/database/entity/DatabaseEntities.kt`.
- DAOs: `InspectionDao.kt`, `CatalogDao.kt`, `IssueDao.kt`, and `SyncDao.kt`.
- Repository adapters: `data/src/main/kotlin/com/topic11/cs426/data/RoomInspectionRepository.kt`, `RoomTemplateRepository.kt`, and `RoomAssetRepository.kt`.
- Mappers: `InspectionSessionMapper.kt` and `InspectionSummaryMapper.kt`.
- Migration and persistence tests: `FieldFlowMigrationTest.kt`, `DraftRecoveryTest.kt`, and `PendingSyncTest.kt`.

`FieldFlowCompositionRoot.kt` opens the Room database, schedules sample seeding on an app-scoped IO coroutine, and passes Room-backed adapters into Domain use cases. It also starts `PendingSyncDrain` on that same app scope, which collects `SyncDao.observeRetryableCommands()` and hands each command to `FakeRemoteSyncAdapter`; a failure re-emits from the DAO flow immediately, so the loop paces retries with exponential backoff and stops claiming a command after `DEFAULT_MAX_SYNC_ATTEMPTS`. Without that consumer a completed inspection stayed `PENDING` forever and the Dashboard "Sync pending" tile only counted up. The architecture supports Room as local source of truth, Flow observation, persisted drafts, and pending synchronization state. Manual Android Studio testing is still required to confirm process-restart behavior on the owner-selected runtime device.

## 12. Testing Strategy

FieldFlow uses several levels of tests:

- Pure Domain tests: `DomainBusinessRulesTest.kt`, `ObserveInspectionSummariesUseCaseTest.kt`, `ObserveInspectionUseCaseTest.kt`, and workflow use-case tests.
- Database and DAO tests: `FieldFlowDatabaseTest.kt`, `DraftRecoveryTest.kt`, `PendingSyncTest.kt`, and `FieldFlowMigrationTest.kt`.
- Data adapter tests: `RoomInspectionRepositoryTest.kt`, `RoomCatalogRepositoriesTest.kt`, `RoomReportRepositoryTest.kt`, `InspectionSummaryMapperTest.kt`, `FieldFlowSampleDataSeederTest.kt`, `AndroidEvidenceStoreTest.kt`, report exporter tests, and `FakeRemoteSyncAdapterTest.kt`.
- Presenter tests: `DashboardPresenterTest.kt`, `InspectionPresenterTest.kt`, `IssuesPresenterTest.kt`, and `ReportsPresenterTest.kt`.
- Composition-root tests in `:app`: `SampleDataSeedingCoordinatorTest.kt`, `PendingSyncDrainTest.kt` for the drain loop's backoff and attempt budget, and `PendingSyncDrainEndToEndTest.kt`, which runs the real database, DAO flow, and adapter to assert a queued completion reaches `SYNCED` and stops reporting `SYNC_PENDING`.
- Runtime UI/instrumentation coverage is limited to `app/src/androidTest/java/com/topic11/cs426/FieldFlowNavigationSmokeTest.kt` and should be completed manually with `docs/demo/FINAL_MANUAL_ACCEPTANCE_CHECKLIST.md`.

The strongest tests are Domain, DAO, mapper, repository, and Presenter tests. Manual runtime acceptance remains necessary because this review does not run an emulator.

## 13. Why This Architecture Is Better for FieldFlow

Compared with a monolithic Android app or single `:app` module, FieldFlow's structure has more setup cost but stronger boundaries. A single module would be faster at the start, but imports between UI, Room, and business rules would be easier to mix accidentally.

Compared with traditional MVVM where ViewModels call repositories or DAOs directly, FieldFlow has more use cases and interfaces. That is acceptable here because inspection validation, scoring, evidence requirements, draft state, issue creation, and report eligibility are business rules that need pure tests and adapter replacement.

Compared with a simple layered architecture without Gradle modules, FieldFlow has stronger enforcement. Packages and naming can express intent, but Gradle module dependencies prevent many accidental imports at compile time.

This architecture is not universally superior. A tiny CRUD prototype might be better as a single app module with ViewModels and a local repository. FieldFlow benefits from stronger boundaries because it has non-trivial rules, offline-first storage, multiple feature teams, report/evidence adapters, and future replacement needs.

## 14. Benefits Demonstrated by the Repository

Separation of concerns: `ValidateInspectionUseCase.kt` and `CalculateInspectionScoreUseCase.kt` contain rules while `InspectionUi.kt` renders state.

Testability: Domain tests run on the JVM without Android.

Maintainability: Dashboard and Inspection presenters depend on use cases, not Room or files.

Scalability: each feature module has its own Gradle boundary and package.

Replaceability: `InspectionRepository.kt` is implemented by test adapters and by `RoomInspectionRepository.kt`.

Parallel team development: `docs/architecture/TEAM_OWNERSHIP.md` maps feature and layer ownership.

Compile-time safety: feature build files do not depend on `:data`.

UI-framework isolation: Domain has no Circuit or Compose imports.

Database isolation: Room entities and DAOs are contained in `:core:database` and mapped in `:data`.

## 15. Costs and Trade-Offs

The costs are real:

- more Gradle modules;
- more interfaces;
- mapping between Room records and Domain models;
- more composition-root wiring;
- more tests and fixtures;
- higher learning curve for Circuit plus Clean Architecture;
- risk of over-engineering if the app stayed small.

For FieldFlow, the trade-off is acceptable because the app's central value is business policy and offline behavior, not just screens over tables. The seminar also benefits because the codebase can demonstrate dependency inversion, replaceable adapters, and testable Domain rules.

## 16. What Would Be Simpler for a Tiny App

A tiny CRUD prototype could use one Android module, Compose screens, ViewModels, and a repository that directly uses Room. That would reduce files and Gradle configuration.

FieldFlow chose a larger architecture because the proposal includes inspections, validation, scoring, evidence, issue creation, offline drafts, pending sync, and report/export boundaries. Those concerns become harder to keep clean in a tiny structure as the app grows.

## 17. Demo Script for Architecture

Use this 5-8 minute architecture demonstration:

1. Open Dashboard and show inspection summaries.
2. Open an inspection and change answers.
3. Save draft, review, trigger validation, add required evidence, and complete.
4. Trace `InspectionUi.kt -> InspectionPresenter.kt -> CompleteInspectionUseCase.kt -> InspectionRepository.kt -> RoomInspectionRepository`.
5. Show `DomainBusinessRulesTest.kt` proving validation/scoring rules without Android.
6. Show `feature/inspection/build.gradle.kts` has no `:data` dependency.
7. Show `FieldFlowCompositionRoot.kt` as the place where the Room adapter is selected.
8. Explain offline-first persistence and asynchronous sample seeding.
9. End with trade-offs: more modules and mapping, but clearer boundaries and testable rules.

## 18. Suggested Speaker Division

Four-person split:

1. Product and workflow: proposal scope, users, Dashboard, Inspection journey.
2. Clean Architecture and Domain: dependency rule, use cases, ports, business-rule tests.
3. Data and offline-first: Room schema, DAOs, repository adapter, migrations, draft and sync tests.
4. Presentation and demo: Slack Circuit, feature modules, navigation, manual runtime scope, trade-offs.

## 19. Likely Questions and Strong Answers

**Why not put everything in one app module?**
That would be faster initially, but FieldFlow has multiple features, offline persistence, evidence/report adapters, and business rules. Separate modules make accidental imports harder and improve team ownership.

**Is this just MVVM?**
No. Circuit Presenters are part of presentation, similar in responsibility to ViewModels, but the architecture boundary is broader. Business rules are in Domain use cases, and repositories are ports owned by Domain.

**Why use Circuit instead of Android ViewModel/navigation?**
Circuit gives typed screens, presenters, UI factories, and one-way state/event flow. FieldFlow still uses Clean Architecture underneath; Circuit only structures the Presentation Layer.

**Why define repository interfaces in Domain?**
Because Domain owns the business need: observe inspections, save drafts, complete inspections, load templates, create issues, export reports, and store evidence. Data only supplies implementations.

**Why is Room not referenced in Domain?**
Room is infrastructure. Domain should be testable on the JVM and should not change if storage moves from Room to another adapter.

**Why not call DAO directly from Presenter?**
That would couple UI workflow to database details and spread validation/scoring decisions into presentation. The Presenter should call use cases and render state.

**Does Clean Architecture add too much boilerplate?**
For a tiny app, maybe. For FieldFlow, the extra ports, mappers, and modules pay for themselves because validation, scoring, offline state, evidence, and reports need stable boundaries.

**How does offline-first work?**
The implemented data path uses Room as local source of truth, DAOs for reads/writes, repository mapping to Domain models, and Flow observation. The current runtime app selects the Room adapters in `FieldFlowCompositionRoot.kt`; manual testing should confirm restart behavior on the owner-selected device.

**How can adapters be replaced?**
Replace the concrete repository construction in `FieldFlowCompositionRoot.kt`. Features and Domain keep the same use cases and ports.

**How do Gradle modules enforce the architecture?**
If `:feature:inspection` does not declare `implementation(project(":data"))`, it cannot compile imports from Data. The same applies to Domain purity through its Kotlin/JVM build.

**What happens when a business rule changes?**
Change the Domain use case and Domain tests first. Presentation should mostly keep rendering state and emitting events, while Data should keep mapping and persistence behavior.

**What are the architecture's limitations?**
Real backend synchronization, authentication, report scheduling, email delivery, push notifications, AI-generated summaries, full multi-section template authoring, and active/archive lifecycle controls for assets/templates remain out of scope. Evidence capture is limited to the implemented reference/storage path, and final PDF opening/rendering still requires manual Android Studio or device confirmation.

## 20. Final Architecture Summary

FieldFlow demonstrates Circuit-Based Feature-Modular Clean Architecture: feature modules own UI, Domain owns inspection rules and ports, Data implements replaceable adapters, Room is isolated behind repositories, and `:app` assembles the runtime graph. The architecture is valuable because FieldFlow has real business rules and offline-first concerns, but the seminar should clearly state which product workflows are implemented, which parts remain limited, and which adapters are code-level fallbacks.
