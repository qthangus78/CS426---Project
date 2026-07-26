package com.topic11.cs426.data.mapping

import com.topic11.cs426.core.database.entity.ReportExportEntity
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.ReportFormat
import com.topic11.cs426.domain.model.ReportHistoryEntry
import com.topic11.cs426.domain.model.ReportId

fun ReportExportEntity.toDomain(): ReportHistoryEntry =
    ReportHistoryEntry(
        id = ReportId(id),
        inspectionId = InspectionId(inspectionId),
        format = enumValue("report format", id, format),
        generatedAtMillis = generatedAtMillis,
        displayFilename = displayFilename,
        storageKey = storageKey,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
    )

fun ReportHistoryEntry.toEntity(): ReportExportEntity =
    ReportExportEntity(
        id = id.value,
        inspectionId = inspectionId.value,
        format = format.name,
        generatedAtMillis = generatedAtMillis,
        displayFilename = displayFilename,
        storageKey = storageKey,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
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
