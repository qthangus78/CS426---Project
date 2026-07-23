package com.topic11.cs426.domain.model

data class InspectionReport(
    val id: ReportId,
    val inspectionId: InspectionId,
    val summary: String,
    val score: InspectionScore,
    val issues: List<MaintenanceIssue>,
    val generatedAtMillis: Long,
) {
    init {
        require(summary.isNotBlank()) { "Report summary cannot be blank." }
    }
}
