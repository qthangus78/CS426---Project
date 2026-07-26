# FieldFlow Demo Script

Use this script for a short seminar demonstration of the current FieldFlow app. Runtime testing remains manual; this script intentionally separates implemented demo behavior from implemented-but-not-wired infrastructure.

## 1. Launch

Open FieldFlow from Android Studio or an installed debug APK. The first screen should be the Dashboard in the Slack Circuit shell.

## 2. Dashboard

Show the FieldFlow header, overview metrics, continue-inspection card, status filters, and deterministic inspection summaries:

- Computer Lab I.44;
- Projector P-204;
- Laboratory A2 Safety Check.

Explain the runtime path:

```text
DashboardUi
-> DashboardPresenter
-> ObserveInspectionSummariesUseCase
-> InspectionRepository
-> DemoInspectionRepository
-> DashboardState
-> DashboardUi
```

Point out that Dashboard depends on Domain use cases, not Data or Room.

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

Explain that validation, scoring, completion, and critical-failure issue creation are Domain use-case behavior. The current app binding persists this workflow through deterministic demo repositories for seminar repeatability.

## 4. Feature Boundaries

From Dashboard quick access, open Assets, Templates, Issues, and Reports.

For Assets, Templates, and Issues, show that each destination is registered, navigable, and explicitly marked as not implemented in the runtime UI.

For Reports, show that report history and PDF/JSON export are not presented as working runtime features. The Domain report model and exporter port exist, but concrete PDF/JSON adapters and user-visible export flow are not wired.

## 5. Offline-First Infrastructure

Show source or tests rather than claiming runtime proof:

```text
RoomInspectionRepository
-> FieldFlowDatabase
-> InspectionDao / CatalogDao / IssueDao / SyncDao
```

Point out:

- Room schema version 2 is present;
- Room repository and DAO tests cover draft recovery and pending sync state;
- `:app` still uses demo repositories, so Android Studio runtime testing does not prove Room persistence until the composition root is changed.

## 6. Architecture Explanation

Trace the boundary:

```text
feature -> domain <- data
```

Use these examples:

- `:feature:inspection` calls `CompleteInspectionUseCase`;
- `:domain` owns `InspectionRepository`, `IssueRepository`, and business rules;
- `:data` implements `RoomInspectionRepository`;
- `:app` assembles Circuit factories and the current repository bindings.

## 7. Close With Scope

Implemented for seminar:

- modular Clean Architecture boundary;
- pure Kotlin Domain rules and ports;
- Circuit Dashboard and Inspection workflow;
- deterministic demo runtime;
- Room/Data infrastructure with unit tests;
- honest placeholder boundaries for unfinished feature UIs.

Not implemented as runtime app behavior:

- production backend/authentication;
- real QR/GPS/push/AI;
- concrete PDF/JSON export flow;
- real photo picker/evidence upload flow;
- Room repository wiring in `:app`.
