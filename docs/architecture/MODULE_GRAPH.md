# FieldFlow Module Graph

Architecture name: **FIELDFlow Architecture Bootstrap**

## Text Diagram

```text
:app
  -> :data
  -> :domain
  -> :core:navigation
  -> :core:designsystem
  -> :feature:dashboard
  -> :feature:assets
  -> :feature:templates
  -> :feature:inspection
  -> :feature:issues
  -> :feature:reports

:feature:dashboard
:feature:assets
:feature:templates
:feature:inspection
:feature:issues
:feature:reports
  -> :domain
  -> :core:navigation
  -> :core:designsystem

:data
  -> :domain

:core:navigation
  -> Circuit screen contracts

:core:designsystem
  -> Compose Material 3

:core:database
  -> Android library marker only

:domain
  -> Kotlin standard library
  -> Kotlin Coroutines Flow

:core:testing
  -> :domain
  -> Kotlin Coroutines Flow
```

## Responsibilities

- `:app`: Android application, manual composition root, Circuit instance, navigation stack, concrete dependency assembly.
- `:domain`: pure Kotlin models, repository ports, use cases, future business rules.
- `:data`: repository implementations. The bootstrap contains only `FakeInspectionRepository`.
- `:core:navigation`: typed Circuit screen contracts.
- `:core:database`: future Room database boundary. Room and offline-first persistence are planned but not implemented in the Architecture Bootstrap.
- `:core:designsystem`: shared theme and small reusable UI components.
- `:core:testing`: reusable test fixtures and fake ports for tests only.
- `:feature:dashboard`: initial root feature and inspection summary list.
- `:feature:inspection`: read-only inspection detail placeholder.
- `:feature:assets`, `:feature:templates`, `:feature:issues`, `:feature:reports`: navigable placeholder feature boundaries.

## Allowed Dependencies

- `:app` may depend on feature modules, `:data`, `:domain`, `:core:navigation`, and `:core:designsystem`.
- Feature modules may depend on `:domain`, `:core:navigation`, and `:core:designsystem`.
- `:data` may depend on `:domain`.
- `:core:testing` may be used through test configurations only.
- `:domain` may use Kotlin Coroutines Flow because repository ports expose reactive streams.

## Forbidden Dependencies

- Feature modules must not depend on `:data`.
- Feature modules must not import `FakeInspectionRepository`, DAOs, Room entities, file implementations, PDF implementations, or synchronization implementations.
- `:domain` must not import Android, Compose, Circuit, Room, app, feature, or data packages.
- `:core:database` must not depend on app or feature modules.
- Production source sets must not depend on `:core:testing`.
- No feature should construct concrete repository implementations.

## Composition Root

`FieldFlowCompositionRoot` in `:app` manually constructs:

- `FakeInspectionRepository`;
- `ObserveInspectionSummariesUseCase`;
- `ObserveInspectionUseCase`;
- feature Presenter factories;
- feature UI factories;
- the Circuit instance.

This keeps construction explicit for the seven-day project and avoids Hilt, Dagger, Koin, Metro, KSP, Anvil, or other code-generation integration risk during the bootstrap.
