package com.topic11.cs426.feature.inspection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.topic11.cs426.core.navigation.InspectionScreen
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionSummary
import com.topic11.cs426.domain.usecase.ObserveInspectionUseCase
import kotlinx.coroutines.flow.map

internal class InspectionPresenter(
    private val screen: InspectionScreen,
    private val observeInspection: ObserveInspectionUseCase,
    private val navigator: Navigator,
) : Presenter<InspectionState> {
    @Composable
    override fun present(): InspectionState {
        val inspectionId = remember(screen.inspectionId) { InspectionId(screen.inspectionId) }
        val presenterModels = remember(inspectionId, observeInspection) {
            observeInspection(inspectionId)
                .map { inspection -> InspectionPresenterModel(isLoaded = true, inspection = inspection) }
        }
        val presenterModel by presenterModels.collectAsState(initial = InspectionPresenterModel())

        val eventSink = remember(navigator) {
            { event: InspectionEvent ->
                when (event) {
                    InspectionEvent.BackSelected -> navigator.pop()
                }
                Unit
            }
        }

        return InspectionState(
            isLoading = !presenterModel.isLoaded,
            inspection = presenterModel.inspection,
            eventSink = eventSink,
        )
    }
}

private data class InspectionPresenterModel(
    val isLoaded: Boolean = false,
    val inspection: InspectionSummary? = null,
)
