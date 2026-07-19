package com.topic11.cs426.feature.assets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.topic11.cs426.core.designsystem.FeaturePlaceholder
import com.topic11.cs426.core.navigation.AssetsScreen

@Immutable
internal data class AssetsState(
    val eventSink: (AssetsEvent) -> Unit,
) : CircuitUiState

internal sealed interface AssetsEvent : CircuitUiEvent {
    data object BackSelected : AssetsEvent
}

internal class AssetsPresenter(
    private val navigator: Navigator,
) : Presenter<AssetsState> {
    @Composable
    override fun present(): AssetsState {
        val eventSink = remember(navigator) {
            { event: AssetsEvent ->
                when (event) {
                    AssetsEvent.BackSelected -> navigator.pop()
                }
                Unit
            }
        }
        return AssetsState(eventSink = eventSink)
    }
}

class AssetsPresenterFactory : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? {
        return when (screen) {
            AssetsScreen -> AssetsPresenter(navigator)
            else -> null
        }
    }
}

class AssetsUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? {
        return when (screen) {
            AssetsScreen -> ui<AssetsState> { state, modifier ->
                AssetsUi(state = state, modifier = modifier)
            }
            else -> null
        }
    }
}

@Composable
internal fun AssetsUi(
    state: AssetsState,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
) {
    FeaturePlaceholder(
        title = "Assets",
        message = "Not implemented yet.",
        futureResponsibilities = listOf(
            "locations",
            "rooms",
            "devices",
            "next inspection date",
        ),
        onBackClick = { state.eventSink(AssetsEvent.BackSelected) },
        modifier = modifier,
    )
}
