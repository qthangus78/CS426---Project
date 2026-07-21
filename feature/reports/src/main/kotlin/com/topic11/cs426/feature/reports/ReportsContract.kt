package com.topic11.cs426.feature.reports

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Immutable
internal data class ReportsState(
    val title: String = "Reports",
    val message: String = "Not implemented yet.",
    val details: String = "Report generation and export wait for future Domain ports and Data adapters.",
    val futureResponsibilities: List<String> = defaultReportResponsibilities,
    val eventSink: (ReportsEvent) -> Unit,
) : CircuitUiState

internal sealed interface ReportsEvent : CircuitUiEvent {
    data object BackSelected : ReportsEvent
}

private val defaultReportResponsibilities = listOf(
    "inspection report presentation",
    "completed-inspection eligibility",
    "PDF and JSON exporters behind Domain ports",
    "export and sharing status",
)
