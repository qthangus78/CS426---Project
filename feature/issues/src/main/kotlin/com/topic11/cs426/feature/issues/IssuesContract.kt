package com.topic11.cs426.feature.issues

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.topic11.cs426.core.designsystem.StatusTone
import com.topic11.cs426.domain.model.IssueId
import com.topic11.cs426.domain.model.IssueSeverity
import com.topic11.cs426.domain.model.MaintenanceIssueStatus

internal sealed interface IssuesState : CircuitUiState {
    data object Loading : IssuesState

    @Immutable
    data class Empty(
        val filters: List<IssueFilterUi>,
        val selectedFilter: IssueFilterUi,
        val eventSink: (IssuesEvent) -> Unit,
    ) : IssuesState

    @Immutable
    data class Content(
        val issues: List<IssueListItemUi>,
        val filters: List<IssueFilterUi>,
        val selectedFilter: IssueFilterUi,
        val eventSink: (IssuesEvent) -> Unit,
    ) : IssuesState

    @Immutable
    data class Error(
        val message: String,
        val eventSink: (IssuesEvent) -> Unit,
    ) : IssuesState
}

internal sealed interface IssueDetailState : CircuitUiState {
    data object Loading : IssueDetailState

    @Immutable
    data class Missing(
        val eventSink: (IssueDetailEvent) -> Unit,
    ) : IssueDetailState

    @Immutable
    data class Content(
        val issue: IssueDetailItemUi,
        val allowedTransitions: List<IssueStatusActionUi>,
        val isSaving: Boolean,
        val updateMessage: String?,
        val eventSink: (IssueDetailEvent) -> Unit,
    ) : IssueDetailState
}

internal sealed interface IssuesEvent : CircuitUiEvent {
    data object BackSelected : IssuesEvent
    data object RetrySelected : IssuesEvent
    data class FilterSelected(val filter: IssueFilterUi) : IssuesEvent
    data class IssueSelected(val issueId: IssueId) : IssuesEvent
}

internal sealed interface IssueDetailEvent : CircuitUiEvent {
    data object BackSelected : IssueDetailEvent
    data class StatusChangeSelected(val status: MaintenanceIssueStatus) : IssueDetailEvent
}

internal enum class IssueFilterUi(
    val label: String,
) {
    All("All"),
    Active("Active"),
    Resolved("Resolved"),
    Critical("Critical"),
}

@Immutable
internal data class IssueListItemUi(
    val id: IssueId,
    val title: String,
    val severity: IssueSeverity,
    val severityLabel: String,
    val status: MaintenanceIssueStatus,
    val statusLabel: String,
    val statusTone: StatusTone,
    val assetLabel: String,
    val inspectionLabel: String,
    val createdLabel: String,
    val updatedLabel: String,
)

@Immutable
internal data class IssueDetailItemUi(
    val id: IssueId,
    val title: String,
    val description: String,
    val severityLabel: String,
    val status: MaintenanceIssueStatus,
    val statusLabel: String,
    val statusTone: StatusTone,
    val assetLabel: String,
    val inspectionLabel: String,
    val checklistItemLabel: String,
    val createdLabel: String,
    val updatedLabel: String,
)

@Immutable
internal data class IssueStatusActionUi(
    val status: MaintenanceIssueStatus,
    val label: String,
)

internal val issueFilters = IssueFilterUi.entries
