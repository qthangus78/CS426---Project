package com.topic11.cs426.domain.repository

import com.topic11.cs426.domain.model.InspectionReport

interface ReportExporter {
    suspend fun export(report: InspectionReport): ExportedReport
}

data class ExportedReport(
    val reportId: String,
    val uriString: String,
    val mimeType: String,
)
