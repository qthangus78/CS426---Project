package com.topic11.cs426.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "locations",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("parent_id")],
)
data class LocationEntity(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "parent_id") val parentId: String?,
)

@Entity(
    tableName = "assets",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["location_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["code"], unique = true),
        Index("location_id"),
        Index("next_inspection_due_at_ms"),
    ],
)
data class AssetEntity(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    val code: String?,
    @ColumnInfo(name = "location_id") val locationId: String,
    @ColumnInfo(name = "next_inspection_due_at_ms") val nextInspectionDueAtMillis: Long?,
)

@Entity(
    tableName = "inspection_templates",
    indices = [Index(value = ["template_id", "version"], unique = true)],
)
data class InspectionTemplateEntity(
    @androidx.room.PrimaryKey
    @ColumnInfo(name = "revision_id")
    val revisionId: String,
    @ColumnInfo(name = "template_id") val templateId: String,
    val version: Int,
    val name: String,
    @ColumnInfo(name = "recurrence_interval_days") val recurrenceIntervalDays: Int?,
)

@Entity(
    tableName = "inspection_sections",
    foreignKeys = [
        ForeignKey(
            entity = InspectionTemplateEntity::class,
            parentColumns = ["revision_id"],
            childColumns = ["template_revision_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("template_revision_id"),
        Index(value = ["template_revision_id", "position"], unique = true),
    ],
)
data class InspectionSectionEntity(
    @androidx.room.PrimaryKey val id: String,
    @ColumnInfo(name = "template_revision_id") val templateRevisionId: String,
    val title: String,
    val position: Int,
)

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = InspectionSectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["section_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("section_id"),
        Index(value = ["section_id", "position"], unique = true),
    ],
)
data class ChecklistItemEntity(
    @androidx.room.PrimaryKey val id: String,
    @ColumnInfo(name = "section_id") val sectionId: String,
    val title: String,
    val description: String?,
    val position: Int,
    @ColumnInfo(name = "is_required") val isRequired: Boolean,
    @ColumnInfo(name = "is_critical") val isCritical: Boolean,
    val weight: Double,
    @ColumnInfo(name = "answer_type") val answerType: String,
    @ColumnInfo(name = "choice_options_json") val choiceOptionsJson: String?,
)

@Entity(
    tableName = "inspections",
    foreignKeys = [
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["asset_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = InspectionTemplateEntity::class,
            parentColumns = ["revision_id"],
            childColumns = ["template_revision_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = InspectionSectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["current_section_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("asset_id"),
        Index("template_revision_id"),
        Index("current_section_id"),
        Index("lifecycle_status"),
        Index("sync_status"),
        Index("updated_at_ms"),
    ],
)
data class InspectionEntity(
    @androidx.room.PrimaryKey val id: String,
    @ColumnInfo(name = "asset_id") val assetId: String,
    @ColumnInfo(name = "template_revision_id") val templateRevisionId: String,
    @ColumnInfo(name = "lifecycle_status") val lifecycleStatus: String,
    @ColumnInfo(name = "sync_status") val syncStatus: String,
    @ColumnInfo(name = "current_section_id") val currentSectionId: String?,
    @ColumnInfo(name = "started_at_ms") val startedAtMillis: Long,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMillis: Long,
    @ColumnInfo(name = "completed_at_ms") val completedAtMillis: Long?,
    @ColumnInfo(name = "earned_weight") val earnedWeight: Double?,
    @ColumnInfo(name = "total_weight") val totalWeight: Double?,
)

@Entity(
    tableName = "inspection_answers",
    primaryKeys = ["inspection_id", "checklist_item_id"],
    foreignKeys = [
        ForeignKey(
            entity = InspectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["inspection_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ChecklistItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["checklist_item_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("checklist_item_id")],
)
data class InspectionAnswerEntity(
    @ColumnInfo(name = "inspection_id") val inspectionId: String,
    @ColumnInfo(name = "checklist_item_id") val checklistItemId: String,
    @ColumnInfo(name = "answer_type") val answerType: String,
    @ColumnInfo(name = "value_text") val valueText: String?,
    @ColumnInfo(name = "value_number") val valueNumber: Double?,
    @ColumnInfo(name = "value_boolean") val valueBoolean: Boolean?,
    val unit: String?,
    val note: String?,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMillis: Long,
)

@Entity(
    tableName = "evidence",
    foreignKeys = [
        ForeignKey(
            entity = InspectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["inspection_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ChecklistItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["checklist_item_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("inspection_id"), Index("checklist_item_id")],
)
data class EvidenceEntity(
    @androidx.room.PrimaryKey val id: String,
    @ColumnInfo(name = "inspection_id") val inspectionId: String,
    @ColumnInfo(name = "checklist_item_id") val checklistItemId: String?,
    @ColumnInfo(name = "storage_key") val storageKey: String,
    @ColumnInfo(name = "mime_type") val mimeType: String?,
    @ColumnInfo(name = "created_at_ms") val createdAtMillis: Long,
)

@Entity(
    tableName = "maintenance_issues",
    foreignKeys = [
        ForeignKey(
            entity = InspectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["inspection_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["asset_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = ChecklistItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["checklist_item_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("inspection_id"),
        Index("asset_id"),
        Index("checklist_item_id"),
        Index("severity"),
        Index("status"),
    ],
)
data class MaintenanceIssueEntity(
    @androidx.room.PrimaryKey val id: String,
    @ColumnInfo(name = "inspection_id") val inspectionId: String,
    @ColumnInfo(name = "asset_id") val assetId: String,
    @ColumnInfo(name = "checklist_item_id") val checklistItemId: String?,
    val severity: String,
    val title: String,
    val description: String,
    val status: String,
    @ColumnInfo(name = "created_at_ms") val createdAtMillis: Long,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMillis: Long,
)

@Entity(
    tableName = "pending_sync",
    indices = [
        Index(
            value = ["aggregate_type", "aggregate_id", "operation", "payload_version"],
            unique = true,
        ),
        Index(value = ["state", "updated_at_ms"]),
    ],
)
data class PendingSyncEntity(
    @androidx.room.PrimaryKey val id: String,
    @ColumnInfo(name = "aggregate_type") val aggregateType: String,
    @ColumnInfo(name = "aggregate_id") val aggregateId: String,
    val operation: String,
    @ColumnInfo(name = "payload_version") val payloadVersion: Int,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    val state: String,
    @ColumnInfo(name = "attempt_count") val attemptCount: Int,
    @ColumnInfo(name = "last_error_code") val lastErrorCode: String?,
    @ColumnInfo(name = "created_at_ms") val createdAtMillis: Long,
    @ColumnInfo(name = "updated_at_ms") val updatedAtMillis: Long,
)
