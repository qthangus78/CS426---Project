package com.topic11.cs426.feature.reports

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.topic11.cs426.core.navigation.ReportsScreen

class ReportsUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? {
        return when (screen) {
            ReportsScreen -> ui<ReportsState> { state, modifier ->
                ReportsUi(state = state, modifier = modifier)
            }
            else -> null
        }
    }
}
