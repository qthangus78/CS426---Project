package com.topic11.cs426.domain

import com.topic11.cs426.core.testing.FakeIssueRepository
import com.topic11.cs426.core.testing.FakeTemplateRepository
import com.topic11.cs426.core.testing.InspectionTestFixtures
import com.topic11.cs426.core.testing.RecordingInspectionRepository
import com.topic11.cs426.domain.model.InspectionReport
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.ReportExportArtifact
import com.topic11.cs426.domain.model.ReportExportError
import com.topic11.cs426.domain.model.ReportExportResult
import com.topic11.cs426.domain.model.ReportFormat
import com.topic11.cs426.domain.model.ReportHistoryEntry
import com.topic11.cs426.domain.model.ReportId
import com.topic11.cs426.domain.repository.ReportExporter
import com.topic11.cs426.domain.repository.ReportRepository
import com.topic11.cs426.domain.usecase.ExportInspectionReportUseCase
import com.topic11.cs426.domain.usecase.GenerateInspectionReportUseCase
import com.topic11.cs426.domain.usecase.ObserveReportCandidatesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportWorkflowUseCaseTest {
    private val fixtures = InspectionTestFixtures

    @Test
    fun `report candidates include only completed inspections`() = runTest {
        val repository = RecordingInspectionRepository(
            initialSummaries = listOf(
                fixtures.computerLab,
                fixtures.projector.copy(status = InspectionStatus.COMPLETED),
            ),
        )
        val useCase = ObserveReportCandidatesUseCase(repository)

        val candidates = useCase().first()

        assertEquals(listOf(fixtures.projector.id), candidates.map { it.inspectionId })
    }

    @Test
    fun `successful export persists history after file creation`() = runTest {
        val inspectionRepository = RecordingInspectionRepository().apply {
            addSession(
                fixtures.createSampleSession(status = InspectionStatus.COMPLETED)
                    .copy(completedAtMillis = 2_000L),
            )
        }
        val reportRepository = RecordingReportRepository()
        val exporter = RecordingReportExporter()
        val useCase = ExportInspectionReportUseCase(
            generateInspectionReport = reportUseCase(inspectionRepository),
            reportExporter = exporter,
            reportRepository = reportRepository,
        )

        val result = useCase(fixtures.computerLab.id, ReportFormat.JSON)

        assertTrue(result is ReportExportResult.Success)
        val success = result as ReportExportResult.Success
        assertEquals(ReportFormat.JSON, success.entry.format)
        assertEquals(listOf(success.entry), reportRepository.entries)
        assertEquals(listOf(ReportFormat.JSON), exporter.formats)
    }

    @Test
    fun `failed export is not recorded in history`() = runTest {
        val inspectionRepository = RecordingInspectionRepository().apply {
            addSession(fixtures.createSampleSession(status = InspectionStatus.COMPLETED))
        }
        val reportRepository = RecordingReportRepository()
        val useCase = ExportInspectionReportUseCase(
            generateInspectionReport = reportUseCase(inspectionRepository),
            reportExporter = RecordingReportExporter(shouldFail = true),
            reportRepository = reportRepository,
        )

        val result = useCase(fixtures.computerLab.id, ReportFormat.PDF)

        assertEquals(
            ReportExportResult.Failed(ReportExportError.EXPORT_FAILED),
            result,
        )
        assertEquals(emptyList<ReportHistoryEntry>(), reportRepository.entries)
    }

    private fun reportUseCase(
        inspectionRepository: RecordingInspectionRepository,
    ) = GenerateInspectionReportUseCase(
        inspectionRepository = inspectionRepository,
        templateRepository = FakeTemplateRepository(
            templates = mapOf(fixtures.templateId to fixtures.sampleTemplate),
        ),
        issueRepository = FakeIssueRepository(),
        clock = { 3_000L },
        idFactory = { ReportId("report-test") },
    )

    private class RecordingReportExporter(
        private val shouldFail: Boolean = false,
    ) : ReportExporter {
        val formats = mutableListOf<ReportFormat>()

        override suspend fun export(
            report: InspectionReport,
            format: ReportFormat,
        ): ReportExportArtifact {
            formats.add(format)
            if (shouldFail) {
                error("export failed")
            }
            return ReportExportArtifact(
                storageKey = "reports/report-test.${format.name.lowercase()}",
                displayFilename = "report-test.${format.name.lowercase()}",
                mimeType = if (format == ReportFormat.PDF) "application/pdf" else "application/json",
                sizeBytes = 10L,
            )
        }
    }

    private class RecordingReportRepository : ReportRepository {
        private val history = MutableStateFlow<List<ReportHistoryEntry>>(emptyList())
        val entries: List<ReportHistoryEntry>
            get() = history.value

        override fun observeExportHistory(): Flow<List<ReportHistoryEntry>> = history

        override suspend fun saveExport(entry: ReportHistoryEntry) {
            history.value = listOf(entry) + history.value
        }
    }
}
