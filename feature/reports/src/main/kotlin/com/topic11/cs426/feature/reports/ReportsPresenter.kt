package com.topic11.cs426.feature.reports

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.topic11.cs426.core.designsystem.StatusTone
import com.topic11.cs426.core.navigation.ReportDetailScreen
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionReport
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.IssueSeverity
import com.topic11.cs426.domain.model.MaintenanceIssueStatus
import com.topic11.cs426.domain.model.ReportCandidate
import com.topic11.cs426.domain.model.ReportExportError
import com.topic11.cs426.domain.model.ReportExportResult
import com.topic11.cs426.domain.model.ReportFormat
import com.topic11.cs426.domain.model.ReportGenerationError
import com.topic11.cs426.domain.model.ReportGenerationResult
import com.topic11.cs426.domain.model.ReportHistoryEntry
import com.topic11.cs426.domain.usecase.ExportInspectionReportUseCase
import com.topic11.cs426.domain.usecase.GenerateInspectionReportUseCase
import com.topic11.cs426.domain.usecase.ObserveReportCandidatesUseCase
import com.topic11.cs426.domain.usecase.ObserveReportHistoryUseCase
import com.topic11.cs426.domain.usecase.toReportLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal class ReportsPresenter(
    private val observeReportCandidates: ObserveReportCandidatesUseCase,
    private val observeReportHistory: ObserveReportHistoryUseCase,
    private val reportActionHandler: ReportActionHandler,
    private val navigator: Navigator,
) : Presenter<ReportsState> {
    @Composable
    override fun present(): ReportsState {
        var retryToken by remember { mutableIntStateOf(0) }
        var actionMessage by remember { mutableStateOf<String?>(null) }
        var query by remember { mutableStateOf("") }

        val eventSink = remember(navigator, reportActionHandler) {
            { event: ReportsEvent ->
                when (event) {
                    ReportsEvent.BackSelected -> navigator.pop()
                    ReportsEvent.RetrySelected -> retryToken += 1
                    is ReportsEvent.CandidateSelected -> {
                        navigator.goTo(ReportDetailScreen(event.inspectionId.value))
                    }
                    is ReportsEvent.OpenHistorySelected -> {
                        actionMessage = reportActionHandler.open(event.entry).messageOrNull()
                    }
                    is ReportsEvent.ShareHistorySelected -> {
                        actionMessage = reportActionHandler.share(event.entry).messageOrNull()
                    }
                    is ReportsEvent.SearchQueryChanged -> query = event.query
                    ReportsEvent.SearchCleared -> query = ""
                }
                Unit
            }
        }

        val observedReports by remember(
            observeReportCandidates,
            observeReportHistory,
            retryToken,
        ) {
            combine(
                observeReportCandidates(),
                observeReportHistory(),
            ) { candidates, history ->
                ObservedReports.Loaded(candidates, history) as ObservedReports
            }.catch { emit(ObservedReports.Failed) }
        }.collectAsState(initial = ObservedReports.Loading)

        return when (val observed = observedReports) {
            ObservedReports.Loading -> ReportsState.Loading
            ObservedReports.Failed -> ReportsState.Error(
                message = "Reports could not be loaded.",
                eventSink = eventSink,
            )
            is ObservedReports.Loaded -> {
                val candidates = observed.candidates.map { it.toUi() }
                val history = observed.history.map { it.toUi() }
                val filteredCandidates = candidates.filterReportCandidates(query)
                val filteredHistory = history.filterReportHistory(query)
                if (candidates.isEmpty() && history.isEmpty()) {
                    ReportsState.Empty(eventSink)
                } else {
                    ReportsState.Content(
                        candidates = filteredCandidates,
                        history = filteredHistory,
                        query = query,
                        hasNoSearchResults = query.isNotBlank() &&
                            filteredCandidates.isEmpty() &&
                            filteredHistory.isEmpty(),
                        actionMessage = actionMessage,
                        eventSink = eventSink,
                    )
                }
            }
        }
    }
}

private sealed interface ObservedReports {
    data object Loading : ObservedReports
    data object Failed : ObservedReports
    data class Loaded(
        val candidates: List<ReportCandidate>,
        val history: List<ReportHistoryEntry>,
    ) : ObservedReports
}

internal class ReportDetailPresenter(
    private val screen: ReportDetailScreen,
    private val generateInspectionReport: GenerateInspectionReportUseCase,
    private val exportInspectionReport: ExportInspectionReportUseCase,
    private val reportActionHandler: ReportActionHandler,
    private val navigator: Navigator,
) : Presenter<ReportDetailState> {
    @Composable
    override fun present(): ReportDetailState {
        val inspectionId = remember(screen.inspectionId) { InspectionId(screen.inspectionId) }
        val coroutineScope = rememberCoroutineScope()
        var retryToken by remember { mutableIntStateOf(0) }
        var reportResult by remember(inspectionId) {
            mutableStateOf<ReportGenerationResult?>(null)
        }
        var exportState by remember(inspectionId) {
            mutableStateOf<ReportExportUiState>(ReportExportUiState.Idle)
        }
        var lastExport by remember(inspectionId) {
            mutableStateOf<ReportHistoryEntry?>(null)
        }

        LaunchedEffect(inspectionId, retryToken) {
            reportResult = null
            reportResult = generateInspectionReport(inspectionId)
            exportState = ReportExportUiState.Idle
            lastExport = null
        }

        val eventSink = remember(
            inspectionId,
            navigator,
            coroutineScope,
            exportInspectionReport,
            reportActionHandler,
            exportState,
            lastExport,
        ) {
            eventSink@ { event: ReportDetailEvent ->
                when (event) {
                    ReportDetailEvent.BackSelected -> navigator.pop()
                    ReportDetailEvent.RetrySelected -> retryToken += 1
                    is ReportDetailEvent.ExportSelected -> {
                        if (exportState is ReportExportUiState.Exporting) {
                            return@eventSink
                        }
                        coroutineScope.launch {
                            try {
                                exportState = ReportExportUiState.Exporting(event.format)
                                when (val result = exportInspectionReport(inspectionId, event.format)) {
                                    is ReportExportResult.Success -> {
                                        lastExport = result.entry
                                        exportState = ReportExportUiState.Succeeded(
                                            entry = result.entry,
                                            message = "${event.format.displayLabel()} export saved.",
                                        )
                                    }
                                    is ReportExportResult.Failed -> {
                                        exportState = ReportExportUiState.Failed(result.message())
                                    }
                                }
                            } catch (exception: Exception) {
                                if (exception is CancellationException) throw exception
                                exportState = ReportExportUiState.Failed("Report could not be exported.")
                            }
                        }
                    }
                    ReportDetailEvent.OpenLastExportSelected -> {
                        val entry = lastExport ?: return@eventSink
                        reportActionHandler.open(entry).messageOrNull()?.let { message ->
                            exportState = ReportExportUiState.Failed(message)
                        }
                    }
                    ReportDetailEvent.ShareLastExportSelected -> {
                        val entry = lastExport ?: return@eventSink
                        reportActionHandler.share(entry).messageOrNull()?.let { message ->
                            exportState = ReportExportUiState.Failed(message)
                        }
                    }
                }
                Unit
            }
        }

        return when (val result = reportResult) {
            null -> ReportDetailState.Loading
            is ReportGenerationResult.Success -> ReportDetailState.Content(
                report = result.report.toDetailUi(),
                exportState = exportState,
                eventSink = eventSink,
            )
            is ReportGenerationResult.Failed -> result.error.toDetailError(eventSink)
        }
    }
}

private fun ReportCandidate.toUi(): ReportCandidateUi =
    ReportCandidateUi(
        inspectionId = inspectionId,
        title = title,
        statusLabel = status.displayLabel(),
        statusTone = status.statusTone(),
        progressLabel = "$completedItems of $totalItems items",
    )

private fun ReportHistoryEntry.toUi(): ReportHistoryItemUi =
    ReportHistoryItemUi(
        entry = this,
        title = displayFilename,
        formatLabel = format.displayLabel(),
        generatedLabel = generatedAtMillis.dateLabel("Generated"),
        sizeLabel = sizeBytes.formatBytes(),
    )

private fun List<ReportCandidateUi>.filterReportCandidates(query: String): List<ReportCandidateUi> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return this
    return filter { candidate ->
        candidate.title.contains(normalized, ignoreCase = true) ||
            candidate.statusLabel.contains(normalized, ignoreCase = true) ||
            candidate.progressLabel.contains(normalized, ignoreCase = true) ||
            candidate.inspectionId.value.contains(normalized, ignoreCase = true)
    }
}

private fun List<ReportHistoryItemUi>.filterReportHistory(query: String): List<ReportHistoryItemUi> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return this
    return filter { history ->
        history.title.contains(normalized, ignoreCase = true) ||
            history.formatLabel.contains(normalized, ignoreCase = true) ||
            history.generatedLabel.contains(normalized, ignoreCase = true) ||
            history.sizeLabel.contains(normalized, ignoreCase = true)
    }
}

private fun InspectionReport.toDetailUi(): ReportDetailUi =
    ReportDetailUi(
        inspectionId = inspectionId,
        assetName = assetName,
        templateName = templateName,
        summary = summary,
        scoreLabel = "${score.earnedWeight}/${score.totalWeight}",
        completedLabel = completedAtMillis.dateLabel("Completed"),
        generatedLabel = generatedAtMillis.dateLabel("Generated"),
        sections = sections.map { section ->
            ReportSectionUi(
                title = section.title,
                items = section.items.map { item ->
                    val semantics = buildList {
                        if (item.required) add("Required")
                        if (item.critical) add("Critical")
                        add("Weight ${item.weight}")
                    }.joinToString(" - ")
                    ReportItemUi(
                        title = item.title,
                        semanticsLabel = semantics,
                        answerLabel = item.answer?.toReportLabel() ?: "No answer",
                        noteLabel = item.note?.takeIf { it.isNotBlank() }?.let { "Note: $it" },
                        evidenceLabel = if (item.evidenceIds.isEmpty()) {
                            null
                        } else {
                            "${item.evidenceIds.size} evidence reference(s)"
                        },
                    )
                },
            )
        },
        issues = issues.map { issue ->
            ReportIssueUi(
                title = issue.title,
                severityLabel = issue.severity.displayLabel(),
                statusLabel = issue.status.displayLabel(),
            )
        },
    )

private fun ReportGenerationError.toDetailError(
    eventSink: (ReportDetailEvent) -> Unit,
): ReportDetailState.Error =
    when (this) {
        ReportGenerationError.InspectionMissing -> ReportDetailState.Error(
            title = "Inspection not found",
            message = "This inspection may have been removed.",
            eventSink = eventSink,
        )
        is ReportGenerationError.NotEligible -> ReportDetailState.Error(
            title = "Report not ready",
            message = "Only completed inspections can be exported.",
            eventSink = eventSink,
        )
        ReportGenerationError.TemplateMissing -> ReportDetailState.Error(
            title = "Template unavailable",
            message = "The inspection template could not be loaded.",
            eventSink = eventSink,
        )
        ReportGenerationError.IssueLookupFailed -> ReportDetailState.Error(
            title = "Issues unavailable",
            message = "Issue data could not be loaded for this report.",
            eventSink = eventSink,
        )
    }

private fun ReportExportResult.Failed.message(): String =
    when (error) {
        ReportExportError.REPORT_GENERATION_FAILED -> when (generationError) {
            ReportGenerationError.InspectionMissing -> "Inspection could not be found."
            is ReportGenerationError.NotEligible -> "Only completed inspections can be exported."
            ReportGenerationError.TemplateMissing -> "Template data could not be loaded."
            ReportGenerationError.IssueLookupFailed -> "Issue data could not be loaded."
            null -> "Report could not be generated."
        }
        ReportExportError.EXPORT_FAILED -> "Report file could not be created."
        ReportExportError.HISTORY_PERSISTENCE_FAILED -> "Report was created but history could not be saved."
    }

private fun ReportActionResult.messageOrNull(): String? = when (this) {
    ReportActionResult.Started -> null
    is ReportActionResult.Failed -> message
}

private fun ReportFormat.displayLabel(): String = when (this) {
    ReportFormat.JSON -> "JSON"
    ReportFormat.PDF -> "PDF"
}

private fun InspectionStatus.displayLabel(): String = when (this) {
    InspectionStatus.COMPLETED -> "Completed"
    InspectionStatus.SYNC_PENDING -> "Pending sync"
    InspectionStatus.NOT_STARTED -> "Not started"
    InspectionStatus.IN_PROGRESS -> "In progress"
    InspectionStatus.REVIEWING -> "Reviewing"
}

private fun InspectionStatus.statusTone(): StatusTone = when (this) {
    InspectionStatus.COMPLETED -> StatusTone.Success
    InspectionStatus.SYNC_PENDING -> StatusTone.Warning
    InspectionStatus.NOT_STARTED -> StatusTone.Neutral
    InspectionStatus.IN_PROGRESS,
    InspectionStatus.REVIEWING,
    -> StatusTone.InProgress
}

private fun IssueSeverity.displayLabel(): String = when (this) {
    IssueSeverity.CRITICAL -> "Critical"
    IssueSeverity.MAJOR -> "Major"
    IssueSeverity.MINOR -> "Minor"
    IssueSeverity.OBSERVATION -> "Observation"
}

private fun MaintenanceIssueStatus.displayLabel(): String = when (this) {
    MaintenanceIssueStatus.OPEN -> "Open"
    MaintenanceIssueStatus.IN_PROGRESS -> "In progress"
    MaintenanceIssueStatus.RESOLVED -> "Resolved"
    MaintenanceIssueStatus.CLOSED -> "Closed"
}

private fun Long.dateLabel(prefix: String): String =
    "$prefix ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(this))}"

private fun Long.formatBytes(): String =
    if (this < 1024) {
        "$this B"
    } else {
        "${this / 1024} KB"
    }
