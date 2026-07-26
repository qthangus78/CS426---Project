package com.topic11.cs426.domain.model

data class EvidenceReference(
    val id: EvidenceId,
    val inspectionId: InspectionId,
    val checklistItemId: ChecklistItemId,
    val storageKey: String,
    val mimeType: String? = null,
    val createdAtMillis: Long,
) {
    init {
        require(storageKey.isNotBlank()) { "Evidence storage key cannot be blank." }
    }
}
