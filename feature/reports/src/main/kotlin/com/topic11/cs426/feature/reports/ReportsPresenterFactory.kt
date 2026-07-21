package com.topic11.cs426.feature.reports

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.topic11.cs426.core.navigation.ReportsScreen

class ReportsPresenterFactory : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? {
        return when (screen) {
            ReportsScreen -> ReportsPresenter(navigator)
            else -> null
        }
    }
}
