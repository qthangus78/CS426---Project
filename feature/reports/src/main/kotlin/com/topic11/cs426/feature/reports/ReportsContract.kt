package com.topic11.cs426.feature.reports

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.topic11.cs426.core.designsystem.StatusTone
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.ReportFormat
import com.topic11.cs426.domain.model.ReportHistoryEntry

internal sealed interface ReportsState : CircuitUiState {
    data object Loading : ReportsState

    @Immutable
    data class Empty(
        val eventSink: (ReportsEvent) -> Unit,
    ) : ReportsState

    @Immutable
    data class Content(
        val candidates: List<ReportCandidateUi>,
        val history: List<ReportHistoryItemUi>,
        val query: String,
        val hasNoSearchResults: Boolean,
        val actionMessage: String?,
        val eventSink: (ReportsEvent) -> Unit,
    ) : ReportsState

    @Immutable
    data class Error(
        val message: String,
        val eventSink: (ReportsEvent) -> Unit,
    ) : ReportsState
}

internal sealed interface ReportDetailState : CircuitUiState {
    data object Loading : ReportDetailState

    @Immutable
    data class Error(
        val title: String,
        val message: String,
        val eventSink: (ReportDetailEvent) -> Unit,
    ) : ReportDetailState

    @Immutable
    data class Content(
        val report: ReportDetailUi,
        val exportState: ReportExportUiState,
        val eventSink: (ReportDetailEvent) -> Unit,
    ) : ReportDetailState
}

internal sealed interface ReportsEvent : CircuitUiEvent {
    data object BackSelected : ReportsEvent
    data object RetrySelected : ReportsEvent
    data class CandidateSelected(val inspectionId: InspectionId) : ReportsEvent
    data class OpenHistorySelected(val entry: ReportHistoryEntry) : ReportsEvent
    data class ShareHistorySelected(val entry: ReportHistoryEntry) : ReportsEvent
    data class SearchQueryChanged(val query: String) : ReportsEvent
    data object SearchCleared : ReportsEvent
}

internal sealed interface ReportDetailEvent : CircuitUiEvent {
    data object BackSelected : ReportDetailEvent
    data object RetrySelected : ReportDetailEvent
    data class ExportSelected(val format: ReportFormat) : ReportDetailEvent
    data object OpenLastExportSelected : ReportDetailEvent
    data object ShareLastExportSelected : ReportDetailEvent
}

@Immutable
internal data class ReportCandidateUi(
    val inspectionId: InspectionId,
    val title: String,
    val statusLabel: String,
    val statusTone: StatusTone,
    val progressLabel: String,
)

@Immutable
internal data class ReportHistoryItemUi(
    val entry: ReportHistoryEntry,
    val title: String,
    val formatLabel: String,
    val generatedLabel: String,
    val sizeLabel: String,
)

@Immutable
internal data class ReportDetailUi(
    val inspectionId: InspectionId,
    val assetName: String,
    val templateName: String,
    val summary: String,
    val scoreLabel: String,
    val completedLabel: String,
    val generatedLabel: String,
    val sections: List<ReportSectionUi>,
    val issues: List<ReportIssueUi>,
)

@Immutable
internal data class ReportSectionUi(
    val title: String,
    val items: List<ReportItemUi>,
)

@Immutable
internal data class ReportItemUi(
    val title: String,
    val semanticsLabel: String,
    val answerLabel: String,
    val noteLabel: String?,
    val evidenceLabel: String?,
)

@Immutable
internal data class ReportIssueUi(
    val title: String,
    val severityLabel: String,
    val statusLabel: String,
)

internal sealed interface ReportExportUiState {
    data object Idle : ReportExportUiState

    data class Exporting(val format: ReportFormat) : ReportExportUiState

    data class Succeeded(
        val entry: ReportHistoryEntry,
        val message: String,
    ) : ReportExportUiState

    data class Failed(val message: String) : ReportExportUiState
}

interface ReportActionHandler {
    fun open(entry: ReportHistoryEntry): ReportActionResult

    fun share(entry: ReportHistoryEntry): ReportActionResult
}

sealed interface ReportActionResult {
    data object Started : ReportActionResult

    data class Failed(val message: String) : ReportActionResult
}

object NoOpReportActionHandler : ReportActionHandler {
    override fun open(entry: ReportHistoryEntry): ReportActionResult =
        ReportActionResult.Failed("Report file could not be opened.")

    override fun share(entry: ReportHistoryEntry): ReportActionResult =
        ReportActionResult.Failed("Report file could not be shared.")
}
