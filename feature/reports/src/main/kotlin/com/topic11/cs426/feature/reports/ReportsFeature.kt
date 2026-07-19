package com.topic11.cs426.feature.reports

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.topic11.cs426.core.designsystem.FeaturePlaceholder
import com.topic11.cs426.core.navigation.ReportsScreen

@Immutable
internal data class ReportsState(
    val eventSink: (ReportsEvent) -> Unit,
) : CircuitUiState

internal sealed interface ReportsEvent : CircuitUiEvent {
    data object BackSelected : ReportsEvent
}

internal class ReportsPresenter(
    private val navigator: Navigator,
) : Presenter<ReportsState> {
    @Composable
    override fun present(): ReportsState {
        val eventSink = remember(navigator) {
            { event: ReportsEvent ->
                when (event) {
                    ReportsEvent.BackSelected -> navigator.pop()
                }
                Unit
            }
        }
        return ReportsState(eventSink = eventSink)
    }
}

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

@Composable
internal fun ReportsUi(
    state: ReportsState,
    modifier: Modifier = Modifier,
) {
    FeaturePlaceholder(
        title = "Reports",
        message = "Not implemented yet.",
        futureResponsibilities = listOf(
            "report presentation",
            "PDF and JSON exporters behind Domain ports",
        ),
        onBackClick = { state.eventSink(ReportsEvent.BackSelected) },
        modifier = modifier,
    )
}
