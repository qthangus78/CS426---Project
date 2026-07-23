package com.topic11.cs426.feature.reports

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Immutable
internal data class ReportsState(
    val topBarTitle: String = "Reports",
    val title: String = "Inspection reports",
    val message: String = "Report generation is a future milestone.",
    val details: String = "Completed inspection summaries and export adapters will appear here after Domain report ports and Data adapters are implemented.",
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
        description = "Present report-ready inspection results after the workflow is implemented.",
    ),
    ReportCapabilityUi(
        title = "Report eligibility",
        description = "Show which completed inspections can be exported after validation exists.",
    ),
    ReportCapabilityUi(
        title = "PDF export through a Domain port",
        description = "Keep document generation behind a replaceable boundary.",
    ),
    ReportCapabilityUi(
        title = "JSON export through a Domain port",
        description = "Support structured export without coupling UI to file details.",
    ),
    ReportCapabilityUi(
        title = "Export and sharing status",
        description = "Display future export progress honestly when a real implementation exists.",
    ),
)
