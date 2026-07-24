# Database Module

`:core:database` is the future Android database boundary owned by Lĩnh.

Room and offline-first persistence are planned but not implemented in the Architecture Bootstrap.

Future responsibilities:

- Room database;
- DAOs;
- entities;
- migrations;
- local source of truth;
- draft recovery;
- pending synchronization records.

The proposed P0 persistence contract is documented in
[`docs/architecture/DATA_SCHEMA.md`](../../docs/architecture/DATA_SCHEMA.md).
Room entities and DAOs are intentionally deferred to the Room implementation
milestone so the schema can be reviewed before it becomes a database API.
## Schema migrations

`FieldFlowDatabase` is currently at version 2. Production builders must register
`FieldFlowMigrations.ALL`; destructive fallback is not enabled. Version 1 to 2 is an
explicit, data-preserving baseline migration because the persistence schema was stabilized
during version 1 development without a released structural change.

Exported schemas live under `schemas/` and are included as test assets for migration validation.
