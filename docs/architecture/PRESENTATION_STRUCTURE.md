# Presentation Structure

FieldFlow uses Slack Circuit for presentation inside the broader Circuit-Based Feature-Modular Clean Architecture.

## Screen

A Circuit `Screen` is the typed navigation contract for a destination. Screen contracts live in `:core:navigation` so feature modules can agree on routes without depending on each other or on `:app`.

Current examples:

- `DashboardScreen`;
- `InspectionScreen`;
- `AssetsScreen`;
- `TemplatesScreen`;
- `AssetDetailScreen` and `AssetEditorScreen`;
- `TemplateDetailScreen` and `TemplateEditorScreen`;
- `IssuesScreen`;
- `IssueDetailScreen`;
- `ReportsScreen`;
- `ReportDetailScreen`.

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

Feature Presenters depend on Domain use cases. The Dashboard Presenter calls `ObserveInspectionSummariesUseCase`; it does not know whether the data comes from the current Room binding, a code-level fallback adapter, files, or a future backend.

## Navigation Responsibility

Feature Presenters own feature-level navigation decisions such as opening `InspectionScreen`, Assets detail/editor screens, Templates detail/editor screens, Issue details, Report details, or returning with Back. `:app` owns Circuit assembly and the initial navigation stack.

## Why Features Do Not Know Data

Feature modules depend inward on `:domain`; `:data` also depends inward on `:domain`. This creates the intended boundary:

```text
feature -> domain <- data
```

Because features do not depend on `:data`, wiring the implemented Room/Data foundation happens in `:app` composition without rewriting Dashboard or Inspection presentation code.

## Linh-Owned Presentation State

Dashboard currently renders:

- a FieldFlow brand area;
- a derived continue-inspection hero that prefers `IN_PROGRESS` and falls back to `SYNC_PENDING`;
- overview metrics calculated from the complete inspection summary list;
- quick actions for Assets, Locations, Templates, Issues, and Reports destinations;
- a Settings entry point in the top app bar;
- status filters that affect only the visible inspection list;
- loading, empty, content, and filtered-empty states;
- a local About FieldFlow dialog.

Inspection currently renders an editable workflow with section navigation, answer and note updates, evidence references, draft saving, review, validation errors, and completion feedback. The current app binding persists this workflow through Room-backed repositories.

Assets now renders a Room-backed product workflow with list, empty/error states, asset detail, add/edit, location selection, Domain validation, and a start-inspection dialog that reuses the existing inspection-creation use case.

Locations now renders a Room-backed product workflow with list search, empty/error states, detail, add/edit, Domain validation, and Back behavior. Deletion is intentionally omitted because assets use restrictive location foreign keys.

Templates now renders a Room-backed product workflow with list, empty/error states, template detail, section/checklist display, add template with one initial checklist item, metadata-only editing for existing templates, Domain validation, and a start-inspection dialog. Full multi-section/item authoring is intentionally deferred so edits do not drop existing checklist aggregates.

Issues now renders a Room-backed lifecycle workspace with list filters, issue detail, inspection/asset context, loading/empty/error states, and Domain-validated status actions.

Reports now renders completed-inspection candidates, generated report detail, persisted export history, JSON/PDF export actions, and app-layer Open/Share events. The feature depends only on Domain, navigation, and design system contracts; file storage and Android intents remain outside the feature module.

Settings now renders an Appearance workflow with System, Light, and Dark theme choices. The feature uses Domain preference use cases; the Android preference adapter and root theme application remain in `:app`.

## Shared Presentation Components

`:core:designsystem` owns shared Material 3 presentation primitives used across presentation areas, including `FieldFlowTheme`, `FieldFlowTopAppBar`, `StatusBadge`, `InspectionSummaryCard`, `LoadingContent`, and `EmptyState`.

`FieldFlowTheme` also owns the small typography refinement used for screen titles, product names, section titles, card titles, body copy, metadata, button text, and chip text. No external font dependency is required.

Dashboard-specific elements remain inside `:feature:dashboard`, including the continue-inspection hero, Dashboard overview, quick actions, filter row, and About dialog.

Linh-owned presentation uses the spacing scale `4dp`, `8dp`, `12dp`, `16dp`, `20dp`, `24dp`, and `32dp` for spacing and padding. Larger dimensions may still be used for stable touch targets, cards, previews, and illustrations.

## Settings Scope

Appearance settings are implemented. Additional settings such as sync preferences, notification preferences, local-data reset, account/profile settings, and demo-data mode remain future scope and are documented in [FUTURE_SETTINGS_SCOPE.md](FUTURE_SETTINGS_SCOPE.md).
