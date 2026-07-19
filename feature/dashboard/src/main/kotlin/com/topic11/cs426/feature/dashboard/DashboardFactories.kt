package com.topic11.cs426.feature.dashboard

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.topic11.cs426.core.navigation.DashboardScreen
import com.topic11.cs426.domain.usecase.ObserveInspectionSummariesUseCase

class DashboardPresenterFactory(
    private val observeInspectionSummaries: ObserveInspectionSummariesUseCase,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? {
        return when (screen) {
            DashboardScreen -> DashboardPresenter(
                observeInspectionSummaries = observeInspectionSummaries,
                navigator = navigator,
            )
            else -> null
        }
    }
}

class DashboardUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? {
        return when (screen) {
            DashboardScreen -> ui<DashboardState> { state, modifier ->
                DashboardUi(
                    state = state,
                    modifier = modifier,
                )
            }
            else -> null
        }
    }
}
