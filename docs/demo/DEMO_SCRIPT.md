# FieldFlow Demo Script

Use this script for a short seminar demonstration of the current FieldFlow app. Runtime testing remains manual; this script intentionally separates runtime-wired behavior from placeholders and infrastructure that is not exposed in the product UI.

## 1. Launch

Open FieldFlow from Android Studio or an installed debug APK. The first screen should be the Dashboard in the Slack Circuit shell.

## 2. Dashboard

Show the FieldFlow header, overview metrics, continue-inspection card, status filters, and seeded inspection summaries:

- Computer Lab I.44;
- Projector P-204;
- Laboratory A2 Safety Check.

Explain the runtime path:

```text
DashboardUi
-> DashboardPresenter
-> ObserveInspectionSummariesUseCase
-> InspectionRepository
-> RoomInspectionRepository
-> FieldFlowDatabase / InspectionDao
-> DashboardState
-> DashboardUi
```

Point out that Dashboard depends on Domain use cases, not Data or Room. `:app` selects the Room-backed repository at the composition root, and sample catalog/inspection data is seeded asynchronously during startup so `MainActivity.onCreate` is not blocked by database work.

## 3. Inspection Workflow

Open `Computer Lab I.44`.

Demonstrate:

- checklist sections and items;
- Pass, Fail, Not Applicable, and measured answers;
- note editing;
- adding an evidence reference label;
- Save draft;
- Review;
- validation errors when required answers or critical-failure evidence are missing;
- Complete after validation passes;
- completion score and issue creation summary.

Explain that validation, scoring, completion, and critical-failure issue creation are Domain use-case behavior. The current app binding persists this workflow through Room-backed repositories.

## 4. Feature Boundaries

From Dashboard quick access, open Assets, Templates, Issues, and Reports.

For Assets, Templates, and Issues, show that each destination is registered, navigable, and explicitly marked as not implemented in the runtime UI.

For Reports, show that report history and PDF/JSON export are not presented as working runtime features. The Domain report model and exporter port exist, but concrete PDF/JSON adapters and user-visible export flow are not wired.

## 5. Offline-First Runtime

Show the runtime source path and tests:

```text
RoomInspectionRepository
-> FieldFlowDatabase
-> InspectionDao / CatalogDao / IssueDao / SyncDao
```

Point out:

- Room schema version 2 is present;
- Room repository and DAO tests cover draft recovery and pending sync state;
- `:app` binds Room repositories in `FieldFlowCompositionRoot.kt`;
- Android Studio manual testing should still confirm process-restart draft recovery and pending-sync visibility on a device or emulator selected by the repository owner.

## 6. Architecture Explanation

Trace the boundary:

```text
feature -> domain <- data
```

Use these examples:

- `:feature:inspection` calls `CompleteInspectionUseCase`;
- `:domain` owns `InspectionRepository`, `IssueRepository`, and business rules;
- `:data` implements `RoomInspectionRepository`;
- `:app` assembles Circuit factories and the current Room-backed repository bindings.

## 7. Close With Scope

Implemented for seminar:

- modular Clean Architecture boundary;
- pure Kotlin Domain rules and ports;
- Circuit Dashboard and Inspection workflow;
- Room-backed runtime persistence with asynchronous sample seeding;
- Room/Data infrastructure with unit tests;
- honest placeholder boundaries for unfinished feature UIs.

Not implemented as runtime app behavior:

- production backend/authentication;
- real QR/GPS/push/AI;
- concrete PDF/JSON export flow;
- real photo picker/evidence upload flow;
- cloud synchronization or backend authentication.

`DemoRepositories.kt` remains as a code-level fallback for deterministic adapter-swap experiments and tests. It is not selected by the normal product runtime and must not be exposed as a product UI setting.
