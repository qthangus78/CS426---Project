package com.topic11.cs426.feature.dashboard

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionSummary

@Immutable
data class DashboardState(
    val isLoading: Boolean,
    val inspections: List<InspectionSummary>,
    val eventSink: (DashboardEvent) -> Unit,
) : CircuitUiState

sealed interface DashboardEvent : CircuitUiEvent {
    data class InspectionSelected(val inspectionId: InspectionId) : DashboardEvent

    data object AssetsSelected : DashboardEvent

    data object TemplatesSelected : DashboardEvent

    data object IssuesSelected : DashboardEvent

    data object ReportsSelected : DashboardEvent
}
