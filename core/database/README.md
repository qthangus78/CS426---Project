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
