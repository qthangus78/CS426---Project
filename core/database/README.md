# Database Module

`:core:database` is the Android Room persistence boundary owned by Linh.

It currently contains:

- `FieldFlowDatabase` version 2;
- Room entities and DAOs for catalog, inspection, issue, evidence, and pending-sync data;
- explicit, data-preserving migrations in `FieldFlowMigrations`;
- exported schemas in `schemas/` and migration/database tests;
- no dependency on `:app` or any feature module.

The schema source and mapping rules are documented in [docs/architecture/DATA_SCHEMA.md](../../docs/architecture/DATA_SCHEMA.md). Domain types and Room entities must remain separate; `:data` owns mapping between them.

The app composition root opens this database at runtime and keeps the Room dependency at the Android boundary. Do not add a feature dependency on this module to work around integration needs.
