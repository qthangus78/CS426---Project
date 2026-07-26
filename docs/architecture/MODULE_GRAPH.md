# FieldFlow Module Graph

Architecture: **Circuit-Based Feature-Modular Clean Architecture**.

## Current Dependencies

```text
:app
  -> :data, :domain, :core:database, :core:navigation, :core:designsystem
  -> :feature:dashboard, :feature:assets, :feature:templates
  -> :feature:inspection, :feature:issues, :feature:reports
  -> :feature:locations, :feature:settings

:feature:* -> :domain, :core:navigation, :core:designsystem
:data      -> :domain, :core:database
:core:navigation -> Circuit Screen contracts
:core:designsystem -> Compose Material 3
:core:database -> Room Android APIs
:core:testing -> :domain and Coroutines Flow; consumed from tests only
:domain -> Kotlin standard library and Coroutines Flow
```

## Responsibilities

- `:app`: Android application, Circuit assembly, initial navigation stack, Android open/share bridge, appearance preference adapter, and concrete dependency assembly. The current runtime opens Room, applies migrations, schedules sample-data seeding asynchronously, and binds Room-backed repositories plus report exporters.
- `:domain`: pure Kotlin IDs, models, repository ports, validation, scoring, inspection lifecycle, issue, report, draft, appearance preference, and scheduling use cases.
- `:data`: persistence-to-Domain mapping, Room repositories, sample-data seeding, evidence-file adapter, report file exporters, and deterministic fake remote synchronization.
- `:core:navigation`: Parcelable Circuit `Screen` contracts shared across features.
- `:core:database`: Room database version 3, entities, DAOs, migrations, exported schemas, and database tests. It owns storage models, never Domain models.
- `:core:designsystem`: shared Compose theme and reusable UI primitives.
- `:core:testing`: Domain fakes and fixtures for test source sets.
- `:feature:dashboard`: inspection summary dashboard.
- `:feature:inspection`: editable inspection workflow presentation: answer, note, evidence-reference, section progress, draft, review, validation, and completion states.
- `:feature:assets`: Room-backed asset list, detail, add/edit, location association, validation, and start-inspection handoff.
- `:feature:locations`: Room-backed location list, search, detail, add/edit, validation, and non-destructive location management.
- `:feature:templates`: Room-backed template list, detail with sections/items, add template with an initial checklist item, metadata editing that preserves existing sections/items, validation, and start-inspection handoff.
- `:feature:issues`: Room-backed issue list, filters, detail, and Domain-validated lifecycle status updates.
- `:feature:reports`: completed-inspection report candidates, generated report detail, persisted export history, JSON/PDF export actions, and open/share events.
- `:feature:settings`: appearance settings presentation for System, Light, and Dark theme modes.

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

`FieldFlowCompositionRoot` assembles Domain use cases, feature factories, the Circuit instance, the app-scoped Room database, report exporters, appearance preferences, and Android open/share handling. It binds Room-backed inspection, template, asset, issue, and report repositories for the current runtime while keeping Room/Data implementations out of feature modules. Sample data seeding runs on an app-scoped IO coroutine. Evidence storage is consumed by the inspection workflow; fake synchronization remains a Data-layer capability until app workflows consume it.
