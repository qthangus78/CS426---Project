package com.topic11.cs426.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.topic11.cs426.core.designsystem.StatusTone
import com.topic11.cs426.core.navigation.AssetsScreen
import com.topic11.cs426.core.navigation.InspectionScreen
import com.topic11.cs426.core.navigation.IssuesScreen
import com.topic11.cs426.core.navigation.ReportsScreen
import com.topic11.cs426.core.navigation.TemplatesScreen
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
                    val inspections = summaries.map { inspection -> inspection.toUiModel() }
                    DashboardPresenterModel(
                        isLoaded = true,
                        overview = summaries.toOverviewUi(),
                        inspections = inspections,
                        heroInspection = inspections.selectHeroInspection(),
                    )
                }
        }
        val presenterModel by presenterModels.collectAsState(initial = DashboardPresenterModel())
        var selectedFilter by remember { mutableStateOf(InspectionFilterUi.ALL) }
        var isAboutVisible by remember { mutableStateOf(false) }

        val eventSink = remember(navigator) {
            { event: DashboardEvent ->
                when (event) {
                    DashboardEvent.AboutDismissed -> {
                        isAboutVisible = false
                    }
                    DashboardEvent.AboutSelected -> {
                        isAboutVisible = true
                    }
                    is DashboardEvent.FilterSelected -> {
                        selectedFilter = event.filter
                    }
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
                selectedFilter = selectedFilter,
                isAboutVisible = isAboutVisible,
                eventSink = eventSink,
            )
            else -> DashboardState.Content(
                overview = presenterModel.overview,
                heroInspection = presenterModel.heroInspection,
                selectedFilter = selectedFilter,
                filteredInspections = presenterModel.inspections.filterBy(selectedFilter),
                isAboutVisible = isAboutVisible,
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
    val heroInspection: InspectionSummaryUi? = null,
    val inspections: List<InspectionSummaryUi> = emptyList(),
)

private fun List<InspectionSummaryUi>.selectHeroInspection(): InspectionSummaryUi? {
    return firstOrNull { inspection ->
        inspection.filter == InspectionFilterUi.IN_PROGRESS
    } ?: firstOrNull { inspection ->
        inspection.filter == InspectionFilterUi.SYNC_PENDING
    }
}

private fun List<InspectionSummaryUi>.filterBy(filter: InspectionFilterUi): List<InspectionSummaryUi> {
    return when (filter) {
        InspectionFilterUi.ALL -> this
        InspectionFilterUi.IN_PROGRESS,
        InspectionFilterUi.NOT_STARTED,
        InspectionFilterUi.SYNC_PENDING -> filter { inspection -> inspection.filter == filter }
    }
}

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
        filter = status.filter(),
    )
}

private fun InspectionStatus.displayLabel(): String {
    return when (this) {
        InspectionStatus.NOT_STARTED -> "Not started"
        InspectionStatus.IN_PROGRESS -> "In progress"
        InspectionStatus.REVIEWING -> "Reviewing"
        InspectionStatus.COMPLETED -> "Completed"
        InspectionStatus.SYNC_PENDING -> "Sync pending"
    }
}

private fun InspectionStatus.statusTone(): StatusTone {
    return when (this) {
        InspectionStatus.NOT_STARTED -> StatusTone.Neutral
        InspectionStatus.IN_PROGRESS -> StatusTone.InProgress
        InspectionStatus.REVIEWING -> StatusTone.InProgress
        InspectionStatus.COMPLETED -> StatusTone.Success
        InspectionStatus.SYNC_PENDING -> StatusTone.Warning
    }
}

private fun InspectionStatus.filter(): InspectionFilterUi? {
    return when (this) {
        InspectionStatus.NOT_STARTED -> InspectionFilterUi.NOT_STARTED
        InspectionStatus.IN_PROGRESS -> InspectionFilterUi.IN_PROGRESS
        InspectionStatus.REVIEWING -> InspectionFilterUi.IN_PROGRESS
        InspectionStatus.SYNC_PENDING -> InspectionFilterUi.SYNC_PENDING
        InspectionStatus.COMPLETED -> null
    }
}
