# Presentation Structure

FieldFlow uses Slack Circuit for presentation inside the broader Circuit-Based Feature-Modular Clean Architecture.

## Screen

A Circuit `Screen` is the typed navigation contract for a destination. Screen contracts live in `:core:navigation` so feature modules can agree on routes without depending on each other or on `:app`.

Current examples:

- `DashboardScreen`;
- `InspectionScreen`;
- `AssetsScreen`;
- `TemplatesScreen`;
- `IssuesScreen`;
- `ReportsScreen`.

## Presenter

A Presenter produces immutable UI state and handles UI events. Presenters may call Domain use cases and may use `Navigator` for screen transitions.

Presenters must not:

- construct repositories;
- import Data implementations;
- know Room, DAOs, files, PDF exporters, or backend details;
- duplicate Domain business rules.

## UiState

`CircuitUiState` is an immutable snapshot of what the Compose UI needs. For Dashboard, the current state separates loading, content, and empty cases so the UI does not infer data readiness from nullable values.

## UiEvent

`CircuitUiEvent` represents user intent. Compose UI emits events through `eventSink`; the Presenter decides whether that intent triggers navigation or another state change.

## Compose UI

Compose UI renders state and emits events. It should not call repositories, construct use cases, perform validation rules, or know adapter details.

## Domain Use Case Dependency

Feature Presenters depend on Domain use cases. The Dashboard Presenter calls `ObserveInspectionSummariesUseCase`; it does not know whether the data comes from fake memory data, Room, files, or a future backend.

## Navigation Responsibility

Feature Presenters own feature-level navigation decisions such as opening `InspectionScreen` or placeholder screens. `:app` owns Circuit assembly and the initial navigation stack.

## Why Features Do Not Know Data

Feature modules depend inward on `:domain`; `:data` also depends inward on `:domain`. This creates the intended boundary:

```text
feature -> domain <- data
```

Because features do not depend on `:data`, replacing `FakeInspectionRepository` with a future Room repository happens in `:app` composition without rewriting Dashboard or Reports presentation code.
