package com.topic11.cs426.domain.model

data class EvidenceReference(
    val id: EvidenceId,
    val inspectionId: InspectionId,
    val checklistItemId: ChecklistItemId,
    val uriString: String,
    val mimeType: String? = null,
    val createdAtMillis: Long,
) {
    init {
        require(uriString.isNotBlank()) { "Evidence URI cannot be blank." }
    }
}
