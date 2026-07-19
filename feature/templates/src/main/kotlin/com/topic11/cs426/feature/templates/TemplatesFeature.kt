package com.topic11.cs426.feature.templates

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
import com.topic11.cs426.core.navigation.TemplatesScreen

@Immutable
internal data class TemplatesState(
    val eventSink: (TemplatesEvent) -> Unit,
) : CircuitUiState

internal sealed interface TemplatesEvent : CircuitUiEvent {
    data object BackSelected : TemplatesEvent
}

internal class TemplatesPresenter(
    private val navigator: Navigator,
) : Presenter<TemplatesState> {
    @Composable
    override fun present(): TemplatesState {
        val eventSink = remember(navigator) {
            { event: TemplatesEvent ->
                when (event) {
                    TemplatesEvent.BackSelected -> navigator.pop()
                }
                Unit
            }
        }
        return TemplatesState(eventSink = eventSink)
    }
}

class TemplatesPresenterFactory : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? {
        return when (screen) {
            TemplatesScreen -> TemplatesPresenter(navigator)
            else -> null
        }
    }
}

class TemplatesUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? {
        return when (screen) {
            TemplatesScreen -> ui<TemplatesState> { state, modifier ->
                TemplatesUi(state = state, modifier = modifier)
            }
            else -> null
        }
    }
}

@Composable
internal fun TemplatesUi(
    state: TemplatesState,
    modifier: Modifier = Modifier,
) {
    FeaturePlaceholder(
        title = "Templates",
        message = "Not implemented yet.",
        futureResponsibilities = listOf(
            "sections",
            "checklist items",
            "answer types",
            "recurrence policy",
        ),
        onBackClick = { state.eventSink(TemplatesEvent.BackSelected) },
        modifier = modifier,
    )
}
