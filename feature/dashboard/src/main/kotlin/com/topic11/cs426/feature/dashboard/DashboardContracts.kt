package com.topic11.cs426.feature.dashboard

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.topic11.cs426.core.designsystem.StatusTone
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.TemplateId

@Immutable
sealed interface DashboardState : CircuitUiState {
    data object Loading : DashboardState

    @Immutable
    data class Content(
        val overview: DashboardOverviewUi,
        val heroInspection: InspectionSummaryUi?,
        val selectedFilter: InspectionFilterUi,
        val filteredInspections: List<InspectionSummaryUi>,
        val isAboutVisible: Boolean,
        val startInspection: StartInspectionUi,
        val eventSink: (DashboardEvent) -> Unit,
    ) : DashboardState

    @Immutable
    data class Empty(
        val overview: DashboardOverviewUi,
        val selectedFilter: InspectionFilterUi,
        val isAboutVisible: Boolean,
        val startInspection: StartInspectionUi,
        val eventSink: (DashboardEvent) -> Unit,
    ) : DashboardState
}

enum class InspectionFilterUi(
    val label: String,
) {
    ALL("All"),
    IN_PROGRESS("In progress"),
    NOT_STARTED("Not started"),
    SYNC_PENDING("Sync pending"),
}

@Immutable
data class DashboardOverviewUi(
    val totalInspections: Int,
    val inProgressInspections: Int,
    val syncPendingInspections: Int,
)

@Immutable
data class InspectionSummaryUi(
    val id: InspectionId,
    val title: String,
    val statusLabel: String,
    val statusTone: StatusTone,
    val completedItems: Int,
    val totalItems: Int,
    val progressFraction: Float,
    val filter: InspectionFilterUi?,
)

@Immutable
data class StartInspectionUi(
    val isVisible: Boolean,
    val isCreating: Boolean,
    val assets: List<StartInspectionAssetUi>,
    val templates: List<StartInspectionTemplateUi>,
    val selectedAssetId: AssetId?,
    val selectedTemplateId: TemplateId?,
    val errorMessage: String?,
) {
    val canConfirm: Boolean
        get() = !isCreating && selectedAssetId != null && selectedTemplateId != null
}

@Immutable
data class StartInspectionAssetUi(
    val id: AssetId,
    val name: String,
    val subtitle: String?,
)

@Immutable
data class StartInspectionTemplateUi(
    val id: TemplateId,
    val name: String,
    val versionLabel: String,
)

sealed interface DashboardEvent : CircuitUiEvent {
    data class InspectionSelected(val inspectionId: InspectionId) : DashboardEvent

    data class FilterSelected(val filter: InspectionFilterUi) : DashboardEvent

    data object StartInspectionSelected : DashboardEvent

    data object StartInspectionDismissed : DashboardEvent

    data class StartInspectionAssetSelected(val assetId: AssetId) : DashboardEvent

    data class StartInspectionTemplateSelected(val templateId: TemplateId) : DashboardEvent

    data object StartInspectionConfirmed : DashboardEvent

    data object AboutSelected : DashboardEvent

    data object AboutDismissed : DashboardEvent

    data object AssetsSelected : DashboardEvent

    data object TemplatesSelected : DashboardEvent

    data object IssuesSelected : DashboardEvent

    data object ReportsSelected : DashboardEvent
}
