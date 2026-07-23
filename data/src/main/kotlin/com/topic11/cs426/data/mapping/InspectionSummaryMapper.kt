package com.topic11.cs426.data.mapping

import com.topic11.cs426.core.database.dao.InspectionSummaryRecord
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.InspectionSummary

fun InspectionSummaryRecord.toDomain(): InspectionSummary {
    validatePersistenceValues()

    return InspectionSummary(
        id = InspectionId(inspectionId),
        title = title,
        status = toDomainStatus(),
        completedItems = completedItems,
        totalItems = totalItems,
    )
}

private fun InspectionSummaryRecord.toDomainStatus(): InspectionStatus {
    return when (lifecycleStatus) {
        LIFECYCLE_NOT_STARTED -> InspectionStatus.NOT_STARTED
        LIFECYCLE_IN_PROGRESS,
        LIFECYCLE_REVIEWING,
        -> InspectionStatus.IN_PROGRESS

        LIFECYCLE_COMPLETED -> when (syncStatus) {
            SYNC_PENDING,
            SYNC_SYNCING,
            SYNC_FAILED,
            -> InspectionStatus.SYNC_PENDING

            SYNC_NOT_REQUIRED,
            SYNC_SYNCED,
            -> InspectionStatus.COMPLETED

            else -> mappingError("Unknown sync status: $syncStatus")
        }

        else -> mappingError("Unknown inspection lifecycle status: $lifecycleStatus")
    }
}

private fun InspectionSummaryRecord.validatePersistenceValues() {
    if (inspectionId.isBlank()) {
        mappingError("Inspection ID cannot be blank")
    }
    if (title.isBlank()) {
        mappingError("Inspection title cannot be blank for $inspectionId")
    }
    if (completedItems < 0 || totalItems < 0 || completedItems > totalItems) {
        mappingError(
            "Invalid progress for $inspectionId: completed=$completedItems, total=$totalItems",
        )
    }

    val knownSyncStatus = syncStatus in setOf(
        SYNC_NOT_REQUIRED,
        SYNC_PENDING,
        SYNC_SYNCING,
        SYNC_SYNCED,
        SYNC_FAILED,
    )
    if (!knownSyncStatus) {
        mappingError("Unknown sync status: $syncStatus")
    }
    if (lifecycleStatus != LIFECYCLE_COMPLETED && syncStatus != SYNC_NOT_REQUIRED) {
        mappingError(
            "Inspection $inspectionId cannot be $lifecycleStatus with sync status $syncStatus",
        )
    }
}

private fun mappingError(message: String): Nothing {
    throw PersistenceMappingException(message)
}

class PersistenceMappingException(message: String) : IllegalArgumentException(message)

private const val LIFECYCLE_NOT_STARTED = "NOT_STARTED"
private const val LIFECYCLE_IN_PROGRESS = "IN_PROGRESS"
private const val LIFECYCLE_REVIEWING = "REVIEWING"
private const val LIFECYCLE_COMPLETED = "COMPLETED"

private const val SYNC_NOT_REQUIRED = "NOT_REQUIRED"
private const val SYNC_PENDING = "PENDING"
private const val SYNC_SYNCING = "SYNCING"
private const val SYNC_SYNCED = "SYNCED"
private const val SYNC_FAILED = "FAILED"
