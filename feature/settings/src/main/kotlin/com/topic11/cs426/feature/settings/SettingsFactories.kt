package com.topic11.cs426.feature.settings

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.topic11.cs426.core.navigation.SettingsScreen
import com.topic11.cs426.domain.usecase.ObserveThemeModeUseCase
import com.topic11.cs426.domain.usecase.SetThemeModeUseCase

class SettingsPresenterFactory(
    private val observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? {
        return when (screen) {
            SettingsScreen -> SettingsPresenter(
                observeThemeMode = observeThemeMode,
                setThemeMode = setThemeMode,
                navigator = navigator,
            )
            else -> null
        }
    }
}

class SettingsUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? {
        return when (screen) {
            SettingsScreen -> ui<SettingsState> { state, modifier ->
                SettingsUi(state = state, modifier = modifier)
            }
            else -> null
        }
    }
}
