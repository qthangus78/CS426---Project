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

Dashboard uses this pattern for inspection selection, status filters, quick-access navigation, and the local About FieldFlow dialog visibility. The About dialog is Dashboard presentation state only; it does not create a Screen contract, navigation destination, repository dependency, or persisted setting.

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

## Linh-Owned Presentation State

Dashboard currently renders:

- a FieldFlow brand area;
- a derived continue-inspection hero that prefers `IN_PROGRESS` and falls back to `SYNC_PENDING`;
- overview metrics calculated from the complete inspection summary list;
- quick actions for existing placeholder boundaries;
- status filters that affect only the visible inspection list;
- loading, empty, content, and filtered-empty states;
- a local About FieldFlow dialog.

Reports currently renders an honest placeholder for future report capability. It shows no fake reports, no export progress, and no exporter or repository implementation. Future report generation requires Domain report contracts and Data/framework adapters before the screen can present real report history or export actions.

## Shared Presentation Components

`:core:designsystem` owns shared Material 3 presentation primitives used across presentation areas, including `FieldFlowTheme`, `FieldFlowTopAppBar`, `StatusBadge`, `InspectionSummaryCard`, `FeaturePlaceholder`, `LoadingContent`, and `EmptyState`.

`FieldFlowTheme` also owns the small typography refinement used for screen titles, product names, section titles, card titles, body copy, metadata, button text, and chip text. No external font dependency is required.

Dashboard-specific elements remain inside `:feature:dashboard`, including the continue-inspection hero, Dashboard overview, quick actions, filter row, and About dialog.

Linh-owned presentation uses the spacing scale `4dp`, `8dp`, `12dp`, `16dp`, `20dp`, `24dp`, and `32dp` for spacing and padding. Larger dimensions may still be used for stable touch targets, cards, previews, and illustrations.

## Future Settings

Settings are not implemented in the Architecture Bootstrap. A future Settings feature would require new screen contracts, app navigation/composition work, and persistence ownership. Proposed scope is documented in [FUTURE_SETTINGS_SCOPE.md](FUTURE_SETTINGS_SCOPE.md).
