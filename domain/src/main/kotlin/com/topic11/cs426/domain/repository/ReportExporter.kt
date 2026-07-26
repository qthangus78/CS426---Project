package com.topic11.cs426.domain.repository

import com.topic11.cs426.domain.model.InspectionReport
import com.topic11.cs426.domain.model.ReportExportArtifact
import com.topic11.cs426.domain.model.ReportFormat

interface ReportExporter {
    suspend fun export(
        report: InspectionReport,
        format: ReportFormat,
    ): ReportExportArtifact
}
