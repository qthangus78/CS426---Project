# FieldFlow Demo Script

Use this script for a short seminar demonstration of the current FieldFlow app. Runtime testing remains manual; this script intentionally separates runtime-wired product behavior from infrastructure that is not exposed in the product UI.

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

From Dashboard quick access, open Assets, Locations, Templates, Issues, and Reports. Open Settings from the Dashboard top app bar.

For Assets, show the Room-backed list, open an asset detail screen, add or edit an asset, preserve the location association, and use Start inspection to enter the existing inspection workflow with a selected template.

For Locations, show the Room-backed list, search, location detail, add/edit behavior, and Back navigation. Deletion is intentionally omitted to preserve asset associations.

For Templates, show the Room-backed list, open template details, review sections and checklist items, add a template with an initial checklist item, and edit existing template metadata without changing the checklist aggregate. Full multi-section/item authoring and active/archive lifecycle controls are not part of the current runtime workflow.

For Issues, complete an inspection with a critical failure first, then show the Room-backed issue list, filters, issue detail, and available lifecycle status actions.

For Reports, show completed inspections ready to export, open a report detail screen, verify score/checklist/issue data, export JSON and PDF, then show the persisted export history plus Open and Share actions.

For Settings, switch between Use system setting, Light, and Dark. The selected mode is persisted through the app-layer preference adapter and applied at the root theme.

## 5. Offline-First Runtime

Show the runtime source path and tests:

```text
RoomInspectionRepository
-> FieldFlowDatabase
-> InspectionDao / CatalogDao / IssueDao / SyncDao
```

Point out:

- Room schema version 3 is present;
- Room repository and DAO tests cover draft recovery, issue updates, report history, and pending sync state;
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
- Room-backed Assets, Templates, Issues, and Reports workflows;
- Room-backed Locations workflow;
- Settings Appearance workflow with persisted System/Light/Dark theme mode;
- Room-backed runtime persistence with asynchronous sample seeding;
- Room/Data infrastructure with unit tests;
- app-layer open/share bridge for exported report artifacts.

Not implemented as runtime app behavior:

- production backend/authentication;
- real QR/GPS/push/AI;
- real camera/gallery evidence capture or upload flow;
- cloud synchronization, backend authentication, email delivery, report scheduling, push notifications, or AI-generated summaries.

`DemoRepositories.kt` remains as a code-level fallback for deterministic adapter-swap experiments and tests. It is not selected by the normal product runtime and must not be exposed as a product UI setting.
