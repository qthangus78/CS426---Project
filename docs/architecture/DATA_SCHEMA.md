# FieldFlow Data Schema (P0)

Status: proposed persistence contract for Data review before Room implementation.

This document defines the P0 local persistence model owned by `:core:database`. It does not change the current Domain contract and does not add Room dependencies. Room entities, DAOs, and migrations are implemented in Task 3 after the Domain contract and dependency change are approved.

## Design Rules

- Store all IDs as non-blank `TEXT` values.
- Keep Room entities inside `:core:database`; never expose them to `:domain` or `:feature:*`.
- Map persistence models to Domain models in `:data`.
- Use the local database as the canonical source of truth.
- Store timestamps as UTC epoch milliseconds in `INTEGER` columns.
- Preserve the template revision used to start an inspection so later template edits cannot corrupt drafts or completed records.
- Persist inspection lifecycle and synchronization state separately.
- Store evidence metadata in the database while the file adapter owns the physical file.

## Status Values

### Inspection lifecycle

`NOT_STARTED`, `IN_PROGRESS`, `REVIEWING`, `COMPLETED`

### Synchronization

`NOT_REQUIRED`, `PENDING`, `SYNCING`, `SYNCED`, `FAILED`

The database must not use `SYNC_PENDING` as a lifecycle value. Until Domain exposes a separate sync status, the Data mapper presents Domain `SYNC_PENDING` when an inspection is locally `COMPLETED` and its synchronization state is `PENDING`, `SYNCING`, or `FAILED`.

This prevents a completed inspection from losing its business lifecycle merely because synchronization has not finished.

## Tables

### `locations`

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | TEXT | Primary key, non-blank |
| `name` | TEXT | Non-blank |
| `parent_id` | TEXT | Nullable foreign key to `locations.id` |

Index `parent_id` for location-tree queries. Use `ON DELETE RESTRICT` while assets reference a location.

### `assets`

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | TEXT | Primary key, non-blank |
| `name` | TEXT | Non-blank |
| `code` | TEXT | Nullable, unique when present |
| `location_id` | TEXT | Foreign key to `locations.id` |
| `next_inspection_due_at_ms` | INTEGER | Nullable |

Index `location_id` and `next_inspection_due_at_ms`.

### `inspection_templates`

| Column | Type | Constraints |
| --- | --- | --- |
| `revision_id` | TEXT | Primary key; stable internal revision key |
| `template_id` | TEXT | Non-blank logical Domain ID |
| `version` | INTEGER | Positive |
| `name` | TEXT | Non-blank |
| `recurrence_interval_days` | INTEGER | Nullable, positive when present |

Add a unique index on `(template_id, version)`. Treat a revision used by an inspection as immutable; create a new revision instead of updating its sections or items.

### `inspection_sections`

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | TEXT | Primary key |
| `template_revision_id` | TEXT | Foreign key to `inspection_templates.revision_id` |
| `title` | TEXT | Non-blank |
| `position` | INTEGER | Non-negative |

Add a unique index on `(template_revision_id, position)` and an index on `template_revision_id`. Use `ON DELETE CASCADE` only for unused template revisions.

### `checklist_items`

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | TEXT | Primary key |
| `section_id` | TEXT | Foreign key to `inspection_sections.id` |
| `title` | TEXT | Non-blank |
| `description` | TEXT | Nullable |
| `position` | INTEGER | Non-negative |
| `is_required` | INTEGER | Boolean `0` or `1` |
| `is_critical` | INTEGER | Boolean `0` or `1` |
| `weight` | REAL | Non-negative |
| `answer_type` | TEXT | Persisted answer-type value |
| `choice_options_json` | TEXT | Nullable; only for `SINGLE_CHOICE` |

Add a unique index on `(section_id, position)` and an index on `section_id`. The Data mapper must reject or explicitly handle unknown `answer_type` values.

### `inspections`

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | TEXT | Primary key |
| `asset_id` | TEXT | Foreign key to `assets.id` |
| `template_revision_id` | TEXT | Foreign key to `inspection_templates.revision_id` |
| `lifecycle_status` | TEXT | Explicit lifecycle value |
| `sync_status` | TEXT | Explicit synchronization value |
| `current_section_id` | TEXT | Nullable foreign key to `inspection_sections.id` |
| `started_at_ms` | INTEGER | Non-null |
| `updated_at_ms` | INTEGER | Non-null |
| `completed_at_ms` | INTEGER | Nullable |
| `earned_weight` | REAL | Nullable |
| `total_weight` | REAL | Nullable |

Index `asset_id`, `template_revision_id`, `lifecycle_status`, `sync_status`, and `updated_at_ms`. A completed inspection retains its template revision even when newer template versions exist.

### `inspection_answers`

| Column | Type | Constraints |
| --- | --- | --- |
| `inspection_id` | TEXT | Foreign key to `inspections.id` |
| `checklist_item_id` | TEXT | Foreign key to `checklist_items.id` |
| `answer_type` | TEXT | Discriminator matching the stored value |
| `value_text` | TEXT | Nullable; text or selected option ID |
| `value_number` | REAL | Nullable; numeric answer |
| `value_boolean` | INTEGER | Nullable boolean `0` or `1` |
| `unit` | TEXT | Nullable numeric unit |
| `note` | TEXT | Nullable |
| `updated_at_ms` | INTEGER | Non-null |

Use `(inspection_id, checklist_item_id)` as the composite primary key. Store exactly the value column required by `answer_type`; do not store UI labels. Delete answers with their inspection using `ON DELETE CASCADE`.

### `evidence`

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | TEXT | Primary key |
| `inspection_id` | TEXT | Foreign key to `inspections.id` |
| `checklist_item_id` | TEXT | Nullable foreign key to `checklist_items.id` |
| `storage_key` | TEXT | Non-blank opaque file-adapter key |
| `mime_type` | TEXT | Nullable |
| `created_at_ms` | INTEGER | Non-null |

Index `inspection_id` and `checklist_item_id`. Do not persist an Android `Uri` object. Deleting the database row and physical file requires coordinated cleanup in the Data adapter.

### `maintenance_issues`

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | TEXT | Primary key |
| `inspection_id` | TEXT | Foreign key to `inspections.id` |
| `asset_id` | TEXT | Foreign key to `assets.id` |
| `checklist_item_id` | TEXT | Nullable foreign key to `checklist_items.id` |
| `severity` | TEXT | Explicit persisted enum value |
| `title` | TEXT | Non-blank |
| `description` | TEXT | Non-blank |
| `status` | TEXT | Explicit persisted enum value |
| `created_at_ms` | INTEGER | Non-null |
| `updated_at_ms` | INTEGER | Non-null |

Index `inspection_id`, `asset_id`, `severity`, and `status`.

### `pending_sync`

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | TEXT | Primary key; idempotency key |
| `aggregate_type` | TEXT | P0 value such as `INSPECTION` or `ISSUE` |
| `aggregate_id` | TEXT | ID of the local aggregate |
| `operation` | TEXT | Explicit operation value |
| `payload_version` | INTEGER | Positive |
| `payload_json` | TEXT | Deterministic serialized command payload |
| `state` | TEXT | `PENDING`, `SYNCING`, `SYNCED`, or `FAILED` |
| `attempt_count` | INTEGER | Non-negative |
| `last_error_code` | TEXT | Nullable, no localized UI message |
| `created_at_ms` | INTEGER | Non-null |
| `updated_at_ms` | INTEGER | Non-null |

Add a unique index on `(aggregate_type, aggregate_id, operation, payload_version)` for P0 duplicate prevention. Index `(state, updated_at_ms)` for retry selection.

## Relationships

```text
locations 1 -> many assets
inspection_templates 1 -> many inspection_sections
inspection_sections 1 -> many checklist_items
assets 1 -> many inspections
inspection_templates revision 1 -> many inspections
inspections 1 -> many inspection_answers
inspections 1 -> many evidence
inspections 1 -> many maintenance_issues
inspections/issues 1 -> many pending_sync commands by aggregate reference
```

`pending_sync.aggregate_id` is an application-level reference rather than a database foreign key because it can reference more than one aggregate table.

## Transaction Boundaries

### Start inspection

Insert the inspection with the selected immutable template revision and first section. Do not copy UI state into the database.

### Save draft

In one transaction:

1. Upsert the inspection lifecycle, current section, and `updated_at_ms`.
2. Upsert changed answers.
3. Insert or update evidence metadata already persisted by the evidence adapter.

A failed transaction must leave the previous valid draft unchanged.

### Complete inspection

In one database transaction after Domain validation succeeds:

1. Persist final answers and score.
2. Set lifecycle to `COMPLETED` and set `completed_at_ms`.
3. Insert required maintenance issues.
4. Set sync status to `PENDING`.
5. Insert idempotent pending-sync records.

The physical evidence files must already be durable before this transaction begins.

### Synchronization transition

Update `pending_sync.state`, retry metadata, and the owning aggregate's `sync_status` together when possible. Remote failure must never delete or roll back valid local business data.

## Mapping Boundary

```text
Room entity/model (:core:database)
        <-> mapper (:data)
Domain model (:domain)
        <-> Presenter mapping
UI model (:feature:inspection)
```

- Database enum values require explicit mapping; do not use `enum.valueOf` without an unknown-value policy.
- `storage_key` maps to an opaque Domain evidence reference, never directly to UI file handling.
- `template_revision_id` remains internal; Domain receives the logical template ID and version required by its approved model.
- Room relations are assembled into aggregate persistence models before mapping to Domain.

## Deferred Beyond P0

- Production backend DTOs and conflict resolution.
- WorkManager scheduling.
- Authentication and user ownership columns.
- Soft-delete/tombstone protocol required by real remote synchronization.
- Report binary storage and polished PDF export.

These additions require a separate contract with the backend and must not reuse Room entities as network DTOs.
