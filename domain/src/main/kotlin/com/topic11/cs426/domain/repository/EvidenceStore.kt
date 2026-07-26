package com.topic11.cs426.domain.repository

import com.topic11.cs426.domain.model.ChecklistItemId
import com.topic11.cs426.domain.model.EvidenceReference
import com.topic11.cs426.domain.model.InspectionId

interface EvidenceStore {
    suspend fun persist(source: EvidenceSource): EvidenceReference

    suspend fun delete(reference: EvidenceReference)
}

data class EvidenceSource(
    val inspectionId: InspectionId,
    val checklistItemId: ChecklistItemId,
    val sourceReference: String,
    val mimeType: String? = null,
) {
    init {
        require(sourceReference.isNotBlank()) { "Evidence source reference cannot be blank." }
    }
}
