package com.topic11.cs426.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.topic11.cs426.core.navigation.AssetsScreen
import com.topic11.cs426.core.navigation.InspectionScreen
import com.topic11.cs426.core.navigation.IssuesScreen
import com.topic11.cs426.core.navigation.ReportsScreen
import com.topic11.cs426.core.navigation.TemplatesScreen
import com.topic11.cs426.core.designsystem.StatusTone
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.InspectionSummary
import com.topic11.cs426.domain.usecase.ObserveInspectionSummariesUseCase
import kotlinx.coroutines.flow.map

internal class DashboardPresenter(
    private val observeInspectionSummaries: ObserveInspectionSummariesUseCase,
    private val navigator: Navigator,
) : Presenter<DashboardState> {
    @Composable
    override fun present(): DashboardState {
        val presenterModels = remember(observeInspectionSummaries) {
            observeInspectionSummaries()
                .map { summaries ->
                    DashboardPresenterModel(
                        isLoaded = true,
                        overview = summaries.toOverviewUi(),
                        inspections = summaries.map { inspection -> inspection.toUiModel() },
                    )
                }
        }
        val presenterModel by presenterModels.collectAsState(initial = DashboardPresenterModel())

        val eventSink = remember(navigator) {
            { event: DashboardEvent ->
                when (event) {
                    is DashboardEvent.InspectionSelected -> {
                        navigator.goTo(InspectionScreen(event.inspectionId.value))
                    }
                    DashboardEvent.AssetsSelected -> navigator.goTo(AssetsScreen)
                    DashboardEvent.TemplatesSelected -> navigator.goTo(TemplatesScreen)
                    DashboardEvent.IssuesSelected -> navigator.goTo(IssuesScreen)
                    DashboardEvent.ReportsSelected -> navigator.goTo(ReportsScreen)
                }
                Unit
            }
        }

        return when {
            !presenterModel.isLoaded -> DashboardState.Loading
            presenterModel.inspections.isEmpty() -> DashboardState.Empty(
                overview = presenterModel.overview,
                eventSink = eventSink,
            )
            else -> DashboardState.Content(
                overview = presenterModel.overview,
                inspections = presenterModel.inspections,
                eventSink = eventSink,
            )
        }
    }
}

private data class DashboardPresenterModel(
    val isLoaded: Boolean = false,
    val overview: DashboardOverviewUi = DashboardOverviewUi(
        totalInspections = 0,
        inProgressInspections = 0,
        syncPendingInspections = 0,
    ),
    val inspections: List<InspectionSummaryUi> = emptyList(),
)

private fun List<InspectionSummary>.toOverviewUi(): DashboardOverviewUi {
    return DashboardOverviewUi(
        totalInspections = size,
        inProgressInspections = count { inspection ->
            inspection.status == InspectionStatus.IN_PROGRESS
        },
        syncPendingInspections = count { inspection ->
            inspection.status == InspectionStatus.SYNC_PENDING
        },
    )
}

private fun InspectionSummary.toUiModel(): InspectionSummaryUi {
    return InspectionSummaryUi(
        id = id,
        title = title,
        statusLabel = status.displayLabel(),
        statusTone = status.statusTone(),
        completedItems = completedItems,
        totalItems = totalItems,
        progressFraction = progressFraction.coerceIn(0f, 1f),
    )
}

private fun InspectionStatus.displayLabel(): String {
    return when (this) {
        InspectionStatus.NOT_STARTED -> "Not started"
        InspectionStatus.IN_PROGRESS -> "In progress"
        InspectionStatus.COMPLETED -> "Completed"
        InspectionStatus.SYNC_PENDING -> "Sync pending"
    }
}

private fun InspectionStatus.statusTone(): StatusTone {
    return when (this) {
        InspectionStatus.NOT_STARTED -> StatusTone.Neutral
        InspectionStatus.IN_PROGRESS -> StatusTone.InProgress
        InspectionStatus.COMPLETED -> StatusTone.Success
        InspectionStatus.SYNC_PENDING -> StatusTone.Warning
    }
}
