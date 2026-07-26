package com.topic11.cs426.data.mapping

import com.topic11.cs426.core.database.entity.MaintenanceIssueEntity
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.ChecklistItemId
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.IssueId
import com.topic11.cs426.domain.model.IssueSeverity
import com.topic11.cs426.domain.model.MaintenanceIssue
import com.topic11.cs426.domain.model.MaintenanceIssueStatus

fun MaintenanceIssueEntity.toDomain() = MaintenanceIssue(
    id = IssueId(id),
    inspectionId = InspectionId(inspectionId),
    assetId = AssetId(assetId),
    checklistItemId = checklistItemId?.let(::ChecklistItemId),
    severity = enumValue("issue severity", id, severity),
    title = title,
    description = description.ifEmpty { null },
    status = enumValue("issue status", id, status),
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
)

fun MaintenanceIssue.toEntity(updatedAtMillis: Long = this.updatedAtMillis) =
    MaintenanceIssueEntity(
        id = id.value,
        inspectionId = inspectionId.value,
        assetId = assetId.value,
        checklistItemId = checklistItemId?.value,
        severity = severity.name,
        title = title,
        description = description.orEmpty(),
        status = status.name,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
    )

private inline fun <reified T : Enum<T>> enumValue(
    label: String,
    id: String,
    value: String,
): T = try {
    enumValueOf(value)
} catch (failure: IllegalArgumentException) {
    throw PersistenceMappingException("Unknown $label for $id: $value")
}
