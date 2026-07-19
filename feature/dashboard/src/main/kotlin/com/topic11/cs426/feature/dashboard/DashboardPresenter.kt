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
                .map { summaries -> DashboardPresenterModel(isLoaded = true, inspections = summaries) }
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

        return DashboardState(
            isLoading = !presenterModel.isLoaded,
            inspections = presenterModel.inspections,
            eventSink = eventSink,
        )
    }
}

private data class DashboardPresenterModel(
    val isLoaded: Boolean = false,
    val inspections: List<InspectionSummary> = emptyList(),
)
