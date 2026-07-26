# Domain Module

`:domain` is a pure Kotlin/JVM module. It owns FieldFlow business contracts and use cases.

Implemented responsibilities include IDs, assets, locations, inspection templates, checklist answers,
inspection sessions, validation, weighted scoring, lifecycle completion, maintenance issue creation,
next-inspection scheduling, report modeling, and repository/export ports.

Boundary rules:

- no Android, Compose, Circuit, Room, Data, app, or feature imports;
- ports are owned here and implemented by outer adapters;
- business rules are tested with JVM unit tests under `domain/src/test`.
