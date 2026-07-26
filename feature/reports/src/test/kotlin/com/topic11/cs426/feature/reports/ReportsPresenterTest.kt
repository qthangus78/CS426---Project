package com.topic11.cs426.feature.reports

import app.cash.turbine.ReceiveTurbine
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import com.topic11.cs426.core.navigation.DashboardScreen
import com.topic11.cs426.core.navigation.ReportDetailScreen
import com.topic11.cs426.core.navigation.ReportsScreen
import com.topic11.cs426.core.testing.FakeIssueRepository
import com.topic11.cs426.core.testing.FakeTemplateRepository
import com.topic11.cs426.core.testing.InspectionTestFixtures
import com.topic11.cs426.core.testing.RecordingInspectionRepository
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.InspectionReport
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.IssueId
import com.topic11.cs426.domain.model.IssueSeverity
import com.topic11.cs426.domain.model.MaintenanceIssue
import com.topic11.cs426.domain.model.MaintenanceIssueStatus
import com.topic11.cs426.domain.model.ReportExportArtifact
import com.topic11.cs426.domain.model.ReportFormat
import com.topic11.cs426.domain.model.ReportHistoryEntry
import com.topic11.cs426.domain.model.ReportId
import com.topic11.cs426.domain.repository.ReportExporter
import com.topic11.cs426.domain.repository.ReportRepository
import com.topic11.cs426.domain.usecase.ExportInspectionReportUseCase
import com.topic11.cs426.domain.usecase.GenerateInspectionReportUseCase
import com.topic11.cs426.domain.usecase.ObserveReportCandidatesUseCase
import com.topic11.cs426.domain.usecase.ObserveReportHistoryUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsPresenterTest {
    @Test
    fun `list presents report candidates and opens detail`() = runTest {
        val inspectionRepository = completedInspectionRepository()
        val reportRepository = RecordingReportRepository()
        val navigator = FakeNavigator(DashboardScreen, ReportsScreen)
        val presenter = reportsPresenter(inspectionRepository, reportRepository, navigator)

        presenter.test {
            assertEquals(ReportsState.Loading, awaitItem())
            val content = awaitItem() as ReportsState.Content

            assertEquals(listOf(InspectionTestFixtures.computerLab.id), content.candidates.map { it.inspectionId })
            content.eventSink(ReportsEvent.CandidateSelected(InspectionTestFixtures.computerLab.id))

            assertEquals(
                ReportDetailScreen(InspectionTestFixtures.computerLab.id.value),
                navigator.awaitNextScreen(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `list presents export history`() = runTest {
        val reportRepository = RecordingReportRepository().apply {
            saveExport(historyEntry())
        }
        val presenter = reportsPresenter(
            inspectionRepository = RecordingInspectionRepository(initialSummaries = emptyList()),
            reportRepository = reportRepository,
            navigator = FakeNavigator(DashboardScreen, ReportsScreen),
        )

        presenter.test {
            awaitItem()
            val content = awaitItem() as ReportsState.Content

            assertEquals(listOf("fieldflow-report.pdf"), content.history.map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `detail presents generated report with issue data`() = runTest {
        val issueRepository = FakeIssueRepository().apply { addIssue(issue()) }
        val screen = ReportDetailScreen(InspectionTestFixtures.computerLab.id.value)
        val presenter = reportDetailPresenter(
            issueRepository = issueRepository,
            reportRepository = RecordingReportRepository(),
            navigator = FakeNavigator(DashboardScreen, ReportsScreen, screen),
            screen = screen,
        )

        presenter.test {
            val content = awaitDetailContent()

            assertEquals("Computer Lab I.44", content.report.assetName)
            assertEquals(listOf("Critical failure: Fire extinguisher present"), content.report.issues.map { it.title })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `detail exports json and enables open share actions`() = runTest {
        val reportRepository = RecordingReportRepository()
        val actionHandler = RecordingReportActionHandler()
        val screen = ReportDetailScreen(InspectionTestFixtures.computerLab.id.value)
        val presenter = reportDetailPresenter(
            issueRepository = FakeIssueRepository(),
            reportRepository = reportRepository,
            actionHandler = actionHandler,
            navigator = FakeNavigator(DashboardScreen, ReportsScreen, screen),
            screen = screen,
        )

        presenter.test {
            val content = awaitDetailContent()

            content.eventSink(ReportDetailEvent.ExportSelected(ReportFormat.JSON))
            advanceUntilIdle()
            val exported = awaitExportSuccess()
            exported.eventSink(ReportDetailEvent.OpenLastExportSelected)
            exported.eventSink(ReportDetailEvent.ShareLastExportSelected)

            assertEquals(ReportFormat.JSON, reportRepository.entries.single().format)
            assertEquals(1, actionHandler.openCalls)
            assertEquals(1, actionHandler.shareCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `back event pops reports screen`() = runTest {
        val navigator = FakeNavigator(DashboardScreen, ReportsScreen)
        val presenter = reportsPresenter(
            inspectionRepository = RecordingInspectionRepository(initialSummaries = emptyList()),
            reportRepository = RecordingReportRepository(),
            navigator = navigator,
        )

        presenter.test {
            var state = awaitItem()
            while (state !is ReportsState.Empty) {
                state = awaitItem()
            }

            (state as ReportsState.Empty).eventSink(ReportsEvent.BackSelected)
            assertEquals(ReportsScreen, navigator.awaitPop().poppedScreen)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun reportsPresenter(
        inspectionRepository: RecordingInspectionRepository,
        reportRepository: RecordingReportRepository,
        navigator: FakeNavigator,
    ) = ReportsPresenter(
        observeReportCandidates = ObserveReportCandidatesUseCase(inspectionRepository),
        observeReportHistory = ObserveReportHistoryUseCase(reportRepository),
        reportActionHandler = NoOpReportActionHandler,
        navigator = navigator,
    )

    private fun reportDetailPresenter(
        issueRepository: FakeIssueRepository,
        reportRepository: RecordingReportRepository,
        actionHandler: ReportActionHandler = NoOpReportActionHandler,
        navigator: FakeNavigator,
        screen: ReportDetailScreen,
    ): ReportDetailPresenter {
        val inspectionRepository = completedInspectionRepository()
        val generateReport = GenerateInspectionReportUseCase(
            inspectionRepository = inspectionRepository,
            templateRepository = FakeTemplateRepository(
                templates = mapOf(InspectionTestFixtures.templateId to InspectionTestFixtures.sampleTemplate),
            ),
            issueRepository = issueRepository,
            clock = { 3_000L },
            idFactory = { ReportId("report-test") },
        )
        return ReportDetailPresenter(
            screen = screen,
            generateInspectionReport = generateReport,
            exportInspectionReport = ExportInspectionReportUseCase(
                generateInspectionReport = generateReport,
                reportExporter = RecordingReportExporter(),
                reportRepository = reportRepository,
            ),
            reportActionHandler = actionHandler,
            navigator = navigator,
        )
    }

    private fun completedInspectionRepository(): RecordingInspectionRepository {
        val session = InspectionTestFixtures
            .createSampleSession(status = InspectionStatus.COMPLETED)
            .copy(completedAtMillis = 2_000L)
        return RecordingInspectionRepository(
            initialSummaries = listOf(
                com.topic11.cs426.domain.model.InspectionSummary(
                    id = session.id,
                    title = session.assetName,
                    status = InspectionStatus.COMPLETED,
                    completedItems = 2,
                    totalItems = 2,
                ),
            ),
        ).apply { addSession(session) }
    }

    private suspend fun ReceiveTurbine<ReportDetailState>.awaitDetailContent(): ReportDetailState.Content {
        var state = awaitItem()
        while (state !is ReportDetailState.Content) {
            state = awaitItem()
        }
        return state
    }

    private suspend fun ReceiveTurbine<ReportDetailState>.awaitExportSuccess(): ReportDetailState.Content {
        var state = awaitDetailContent()
        while (state.exportState !is ReportExportUiState.Succeeded) {
            state = awaitDetailContent()
        }
        return state
    }

    private fun issue() = MaintenanceIssue(
        id = IssueId("issue-critical"),
        inspectionId = InspectionTestFixtures.computerLab.id,
        assetId = AssetId("asset-1"),
        severity = IssueSeverity.CRITICAL,
        title = "Critical failure: Fire extinguisher present",
        status = MaintenanceIssueStatus.OPEN,
        createdAtMillis = 1_000L,
    )

    private fun historyEntry() = ReportHistoryEntry(
        id = ReportId("report-history"),
        inspectionId = InspectionTestFixtures.computerLab.id,
        format = ReportFormat.PDF,
        generatedAtMillis = 3_000L,
        displayFilename = "fieldflow-report.pdf",
        storageKey = "reports/fieldflow-report.pdf",
        mimeType = "application/pdf",
        sizeBytes = 2_048L,
    )

    private class RecordingReportExporter : ReportExporter {
        override suspend fun export(
            report: InspectionReport,
            format: ReportFormat,
        ): ReportExportArtifact =
            ReportExportArtifact(
                storageKey = "reports/report-test.${format.name.lowercase()}",
                displayFilename = "report-test.${format.name.lowercase()}",
                mimeType = if (format == ReportFormat.PDF) "application/pdf" else "application/json",
                sizeBytes = 100L,
            )
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

    private class RecordingReportActionHandler : ReportActionHandler {
        var openCalls = 0
        var shareCalls = 0

        override fun open(entry: ReportHistoryEntry): ReportActionResult {
            openCalls += 1
            return ReportActionResult.Started
        }

        override fun share(entry: ReportHistoryEntry): ReportActionResult {
            shareCalls += 1
            return ReportActionResult.Started
        }
    }
}
