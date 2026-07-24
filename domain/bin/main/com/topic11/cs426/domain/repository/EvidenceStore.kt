package com.topic11.cs426.domain.repository

import com.topic11.cs426.domain.model.EvidenceId
import com.topic11.cs426.domain.model.EvidenceReference

interface EvidenceStore {
    suspend fun persist(source: EvidenceSource): EvidenceReference

    suspend fun delete(reference: EvidenceReference)
}

data class EvidenceSource(
    val inspectionId: String,
    val checklistItemId: String,
    val uriString: String,
    val mimeType: String? = null,
)
