# FieldFlow Module Graph

Architecture: **Circuit-Based Feature-Modular Clean Architecture**.

## Current Dependencies

```text
:app
  -> :data, :domain, :core:database, :core:navigation, :core:designsystem
  -> :feature:dashboard, :feature:assets, :feature:templates
  -> :feature:inspection, :feature:issues, :feature:reports

:feature:* -> :domain, :core:navigation, :core:designsystem
:data      -> :domain, :core:database
:core:navigation -> Circuit Screen contracts
:core:designsystem -> Compose Material 3
:core:database -> Room Android APIs
:core:testing -> :domain and Coroutines Flow; consumed from tests only
:domain -> Kotlin standard library and Coroutines Flow
```

## Responsibilities

- `:app`: Android application, Circuit assembly, initial navigation stack, and concrete dependency assembly. The current runtime binds deterministic demo repositories; it is the only valid place to bind Room/Data implementations later.
- `:domain`: pure Kotlin IDs, models, repository ports, validation, scoring, inspection lifecycle, issue, report, draft, and scheduling use cases.
- `:data`: persistence-to-Domain mapping, Room summary repository, sample-data seeding, evidence-file adapter, and deterministic fake remote synchronization.
- `:core:navigation`: Parcelable Circuit `Screen` contracts shared across features.
- `:core:database`: Room database version 2, entities, DAOs, migrations, exported schemas, and database tests. It owns storage models, never Domain models.
- `:core:designsystem`: shared Compose theme and reusable UI primitives.
- `:core:testing`: Domain fakes and fixtures for test source sets.
- `:feature:dashboard`: inspection summary dashboard.
- `:feature:inspection`: editable inspection workflow presentation: answer, note, evidence-reference, section progress, draft, review, validation, and completion states.
- `:feature:assets`, `:feature:templates`, `:feature:issues`: navigable placeholder boundaries.
- `:feature:reports`: an honest placeholder for report history/export capability.

## Allowed Dependencies

- `:app` may depend on all feature modules, `:data`, `:domain`, `:core:database`, `:core:navigation`, and `:core:designsystem`.
- Feature modules may depend only on `:domain`, `:core:navigation`, and `:core:designsystem` from this project.
- `:data` may depend on `:domain` and `:core:database`.
- `:core:testing` is a test dependency only.
- `:domain` may use Coroutines Flow because repository ports are reactive.

## Forbidden Dependencies

- Feature modules must not depend on `:data` or `:core:database`.
- Feature modules must not import repositories, Room entities/DAOs, file adapters, sync adapters, or exporters.
- `:domain` must not import Android, Compose, Circuit, Room, app, feature, or data packages.
- `:core:database` must not depend on app or feature modules.
- Production source sets must not depend on `:core:testing`.
- Only `:app` may assemble concrete repository or adapter implementations.

## Composition Root

`FieldFlowCompositionRoot` assembles Domain use cases, feature factories, and the Circuit instance. At the current commit it binds `DemoInspectionRepository`, `DemoTemplateRepository`, and `DemoIssueRepository` for a deterministic runnable demo. The implemented Room/Data foundation is not yet wired here; that integration must remain a composition-root change and must not alter feature dependencies.
