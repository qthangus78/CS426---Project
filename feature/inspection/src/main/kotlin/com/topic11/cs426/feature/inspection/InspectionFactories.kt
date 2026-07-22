package com.topic11.cs426.feature.inspection

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.topic11.cs426.core.navigation.InspectionScreen

// TODO Phase 2: add ObserveInspectionSessionUseCase to constructor once domain ships.
class InspectionPresenterFactory : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? {
        return when (screen) {
            is InspectionScreen -> InspectionPresenter(
                screen = screen,
                navigator = navigator,
            )
            else -> null
        }
    }
}

class InspectionUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? {
        return when (screen) {
            is InspectionScreen -> ui<InspectionState> { state, modifier ->
                InspectionUi(
                    state = state,
                    modifier = modifier,
                )
            }
            else -> null
        }
    }
}
