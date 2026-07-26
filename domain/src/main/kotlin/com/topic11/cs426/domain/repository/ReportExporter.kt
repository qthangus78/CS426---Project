package com.topic11.cs426.domain.repository

import com.topic11.cs426.domain.model.InspectionReport

interface ReportExporter {
    suspend fun export(report: InspectionReport): ExportedReport
}

data class ExportedReport(
    val reportId: String,
    val storageKey: String,
    val mimeType: String,
) {
    init {
        require(storageKey.isNotBlank()) { "Exported report storage key cannot be blank." }
        require(mimeType.isNotBlank()) { "Exported report MIME type cannot be blank." }
    }
}
