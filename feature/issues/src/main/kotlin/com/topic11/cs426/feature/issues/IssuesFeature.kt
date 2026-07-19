package com.topic11.cs426.feature.issues

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
import com.topic11.cs426.core.navigation.IssuesScreen

@Immutable
internal data class IssuesState(
    val eventSink: (IssuesEvent) -> Unit,
) : CircuitUiState

internal sealed interface IssuesEvent : CircuitUiEvent {
    data object BackSelected : IssuesEvent
}

internal class IssuesPresenter(
    private val navigator: Navigator,
) : Presenter<IssuesState> {
    @Composable
    override fun present(): IssuesState {
        val eventSink = remember(navigator) {
            { event: IssuesEvent ->
                when (event) {
                    IssuesEvent.BackSelected -> navigator.pop()
                }
                Unit
            }
        }
        return IssuesState(eventSink = eventSink)
    }
}

class IssuesPresenterFactory : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? {
        return when (screen) {
            IssuesScreen -> IssuesPresenter(navigator)
            else -> null
        }
    }
}

class IssuesUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? {
        return when (screen) {
            IssuesScreen -> ui<IssuesState> { state, modifier ->
                IssuesUi(state = state, modifier = modifier)
            }
            else -> null
        }
    }
}

@Composable
internal fun IssuesUi(
    state: IssuesState,
    modifier: Modifier = Modifier,
) {
    FeaturePlaceholder(
        title = "Issues",
        message = "Not implemented yet.",
        futureResponsibilities = listOf(
            "issue severity",
            "lifecycle",
            "issue creation from failed inspection items",
        ),
        onBackClick = { state.eventSink(IssuesEvent.BackSelected) },
        modifier = modifier,
    )
}
