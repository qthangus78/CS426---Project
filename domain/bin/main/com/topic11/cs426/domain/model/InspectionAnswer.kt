package com.topic11.cs426.domain.model

data class InspectionAnswer(
    val inspectionId: InspectionId,
    val checklistItemId: ChecklistItemId,
    val value: ChecklistAnswerValue? = null,
    val note: String? = null,
    val evidenceIds: List<EvidenceId> = emptyList(),
    val updatedAtMillis: Long,
)
