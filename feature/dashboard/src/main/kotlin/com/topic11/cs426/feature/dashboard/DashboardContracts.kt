package com.topic11.cs426.feature.dashboard

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.topic11.cs426.core.designsystem.StatusTone
import com.topic11.cs426.domain.model.InspectionId

@Immutable
sealed interface DashboardState : CircuitUiState {
    data object Loading : DashboardState

    @Immutable
    data class Content(
        val inspections: List<InspectionSummaryUi>,
        val eventSink: (DashboardEvent) -> Unit,
    ) : DashboardState

    @Immutable
    data class Empty(
        val eventSink: (DashboardEvent) -> Unit,
    ) : DashboardState
}

@Immutable
data class InspectionSummaryUi(
    val id: InspectionId,
    val title: String,
    val statusLabel: String,
    val statusTone: StatusTone,
    val completedItems: Int,
    val totalItems: Int,
    val progressFraction: Float,
)

sealed interface DashboardEvent : CircuitUiEvent {
    data class InspectionSelected(val inspectionId: InspectionId) : DashboardEvent

    data object AssetsSelected : DashboardEvent

    data object TemplatesSelected : DashboardEvent

    data object IssuesSelected : DashboardEvent

    data object ReportsSelected : DashboardEvent
}
