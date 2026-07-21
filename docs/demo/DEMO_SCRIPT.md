# FieldFlow Demo Script

Use this script for a short seminar demonstration of the current Architecture Bootstrap.

## 1. Launch

Open FieldFlow on the emulator. Start on the Dashboard and point out that the app is running through the Slack Circuit shell, not a starter screen.

## 2. Dashboard

Show the FieldFlow title, the Architecture Bootstrap status, the inspection overview, and the deterministic inspection summaries:

- Computer Lab I.44;
- Projector P-204;
- Laboratory A2 Safety Check.

Explain that these summaries come through the current Domain use case and fake repository port implementation.

## 3. Inspection Summary Navigation

Tap `Computer Lab I.44`. Show the Inspection screen with status, item progress, and the message that the full checklist workflow is future work. Press Back and return to the Dashboard.

## 4. Placeholder Boundaries

From Quick access, open Assets, Templates, Issues, and Reports. For each screen, show that it is navigable, has Back behavior, and clearly says the feature is not implemented yet.

## 5. Architecture Explanation

Describe the implemented path:

```text
DashboardUi
-> DashboardPresenter
-> ObserveInspectionSummariesUseCase
-> InspectionRepository
-> FakeInspectionRepository
-> DashboardState
-> DashboardUi
```

Emphasize the boundary:

- Dashboard depends on Domain use cases, not Data;
- Data implements the Domain repository port;
- `:app` assembles concrete implementations;
- feature modules do not construct repositories.

## 6. Implemented vs Future Work

Implemented now:

- multi-module architecture;
- pure Kotlin Domain bootstrap;
- fake inspection repository;
- Circuit Dashboard-to-Inspection vertical slice;
- placeholder feature boundaries.

Future work:

- Room and offline-first persistence;
- inspection checklist workflow, validation, scoring, evidence, issues, and report exporters;
- backend/authentication.
