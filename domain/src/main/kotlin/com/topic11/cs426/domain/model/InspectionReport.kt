package com.topic11.cs426.domain.model

data class InspectionReport(
    val id: ReportId,
    val inspectionId: InspectionId,
    val assetName: String,
    val templateName: String,
    val summary: String,
    val score: InspectionScore,
    val completedAtMillis: Long,
    val sections: List<InspectionReportSection>,
    val issues: List<MaintenanceIssue>,
    val generatedAtMillis: Long,
) {
    init {
        require(assetName.isNotBlank()) { "Report asset name cannot be blank." }
        require(templateName.isNotBlank()) { "Report template name cannot be blank." }
        require(summary.isNotBlank()) { "Report summary cannot be blank." }
    }
}

data class InspectionReportSection(
    val id: SectionId,
    val title: String,
    val items: List<InspectionReportItem>,
) {
    init {
        require(title.isNotBlank()) { "Report section title cannot be blank." }
    }
}

data class InspectionReportItem(
    val id: ChecklistItemId,
    val title: String,
    val required: Boolean,
    val critical: Boolean,
    val weight: Int,
    val answer: ChecklistAnswerValue?,
    val note: String?,
    val evidenceIds: List<EvidenceId>,
) {
    init {
        require(title.isNotBlank()) { "Report item title cannot be blank." }
        require(weight >= 0) { "Report item weight cannot be negative." }
    }
}
