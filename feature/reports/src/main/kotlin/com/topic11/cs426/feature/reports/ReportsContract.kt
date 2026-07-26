package com.topic11.cs426.feature.reports

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Immutable
internal data class ReportsState(
    val topBarTitle: String = "Reports",
    val title: String = "Inspection reports",
    val message: String = "No completed reports are available yet.",
    val details: String = "Complete inspections to make report summaries available here.",
    val futureCapabilities: List<ReportCapabilityUi> = defaultReportCapabilities,
    val eventSink: (ReportsEvent) -> Unit,
) : CircuitUiState

@Immutable
internal data class ReportCapabilityUi(
    val title: String,
    val description: String,
)

internal sealed interface ReportsEvent : CircuitUiEvent {
    data object BackSelected : ReportsEvent
}

private val defaultReportCapabilities = listOf(
    ReportCapabilityUi(
        title = "Completed inspection summaries",
        description = "Present report-ready inspection results from completed local inspections.",
    ),
    ReportCapabilityUi(
        title = "Report eligibility",
        description = "Show which completed inspections are ready for export.",
    ),
    ReportCapabilityUi(
        title = "PDF export",
        description = "Create a portable inspection report for sharing.",
    ),
    ReportCapabilityUi(
        title = "JSON export",
        description = "Export structured inspection data for record keeping.",
    ),
    ReportCapabilityUi(
        title = "Export and sharing status",
        description = "Show export progress and sharing results.",
    ),
)
