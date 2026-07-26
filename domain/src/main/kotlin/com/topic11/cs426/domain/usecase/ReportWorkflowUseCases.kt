package com.topic11.cs426.domain.usecase

import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.ReportCandidate
import com.topic11.cs426.domain.model.ReportExportError
import com.topic11.cs426.domain.model.ReportExportResult
import com.topic11.cs426.domain.model.ReportFormat
import com.topic11.cs426.domain.model.ReportGenerationResult
import com.topic11.cs426.domain.model.ReportHistoryEntry
import com.topic11.cs426.domain.repository.InspectionRepository
import com.topic11.cs426.domain.repository.ReportExporter
import com.topic11.cs426.domain.repository.ReportRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObserveReportCandidatesUseCase(
    private val inspectionRepository: InspectionRepository,
) {
    operator fun invoke(): Flow<List<ReportCandidate>> =
        inspectionRepository.observeInspectionSummaries()
            .map { summaries ->
                summaries
                    .filter { summary -> summary.status.isReportEligible() }
                    .map { summary ->
                        ReportCandidate(
                            inspectionId = summary.id,
                            title = summary.title,
                            status = summary.status,
                            completedItems = summary.completedItems,
                            totalItems = summary.totalItems,
                        )
                    }
            }
            .distinctUntilChanged()
}

class ObserveReportHistoryUseCase(
    private val reportRepository: ReportRepository,
) {
    operator fun invoke(): Flow<List<ReportHistoryEntry>> =
        reportRepository.observeExportHistory()
}

class ExportInspectionReportUseCase(
    private val generateInspectionReport: GenerateInspectionReportUseCase,
    private val reportExporter: ReportExporter,
    private val reportRepository: ReportRepository,
) {
    suspend operator fun invoke(
        inspectionId: InspectionId,
        format: ReportFormat,
    ): ReportExportResult {
        val report = when (val generated = generateInspectionReport(inspectionId)) {
            is ReportGenerationResult.Success -> generated.report
            is ReportGenerationResult.Failed -> {
                return ReportExportResult.Failed(
                    error = ReportExportError.REPORT_GENERATION_FAILED,
                    generationError = generated.error,
                )
            }
        }

        val artifact = try {
            reportExporter.export(report, format)
        } catch (failure: Throwable) {
            if (failure is CancellationException) throw failure
            return ReportExportResult.Failed(ReportExportError.EXPORT_FAILED)
        }

        val entry = ReportHistoryEntry(
            id = report.id,
            inspectionId = inspectionId,
            format = format,
            generatedAtMillis = report.generatedAtMillis,
            displayFilename = artifact.displayFilename,
            storageKey = artifact.storageKey,
            mimeType = artifact.mimeType,
            sizeBytes = artifact.sizeBytes,
        )

        return try {
            reportRepository.saveExport(entry)
            ReportExportResult.Success(entry)
        } catch (failure: Throwable) {
            if (failure is CancellationException) throw failure
            ReportExportResult.Failed(ReportExportError.HISTORY_PERSISTENCE_FAILED)
        }
    }
}
