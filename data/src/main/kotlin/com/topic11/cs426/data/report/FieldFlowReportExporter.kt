package com.topic11.cs426.data.report

import com.topic11.cs426.domain.model.InspectionReport
import com.topic11.cs426.domain.model.ReportExportArtifact
import com.topic11.cs426.domain.model.ReportFormat
import com.topic11.cs426.domain.repository.ReportExporter

class FieldFlowReportExporter(
    private val jsonReportExporter: JsonReportExporter,
    private val pdfReportExporter: PdfReportExporter,
) : ReportExporter {
    override suspend fun export(
        report: InspectionReport,
        format: ReportFormat,
    ): ReportExportArtifact =
        when (format) {
            ReportFormat.JSON -> jsonReportExporter.export(report)
            ReportFormat.PDF -> pdfReportExporter.export(report)
        }
}
