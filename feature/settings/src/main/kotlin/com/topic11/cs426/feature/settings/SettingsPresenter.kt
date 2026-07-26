package com.topic11.cs426.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.topic11.cs426.domain.model.ThemeMode
import com.topic11.cs426.domain.usecase.ObserveThemeModeUseCase
import com.topic11.cs426.domain.usecase.SetThemeModeUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class SettingsPresenter(
    private val observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val navigator: Navigator,
) : Presenter<SettingsState> {
    @Composable
    override fun present(): SettingsState {
        var retryToken by remember { mutableIntStateOf(0) }
        var isSaving by remember { mutableStateOf(false) }
        var saveError by remember { mutableStateOf<String?>(null) }
        val coroutineScope = rememberCoroutineScope()

        val eventSink = remember(navigator, coroutineScope, setThemeMode) {
            { event: SettingsEvent ->
                when (event) {
                    SettingsEvent.BackSelected -> navigator.pop()
                    SettingsEvent.RetrySelected -> {
                        saveError = null
                        retryToken += 1
                    }
                    is SettingsEvent.ThemeModeSelected -> {
                        if (!isSaving) {
                            coroutineScope.launch {
                                try {
                                    isSaving = true
                                    saveError = null
                                    setThemeMode(event.mode)
                                    isSaving = false
                                } catch (exception: Exception) {
                                    if (exception is CancellationException) throw exception
                                    isSaving = false
                                    saveError = "Appearance setting could not be saved."
                                }
                            }
                        }
                    }
                }
                Unit
            }
        }

        val observedThemeMode by remember(observeThemeMode, retryToken) {
            observeThemeMode()
                .map<ThemeMode, ObservedThemeMode> { ObservedThemeMode.Loaded(it) }
                .catch { emit(ObservedThemeMode.Failed) }
        }.collectAsState(initial = ObservedThemeMode.Loading)

        return when (val observed = observedThemeMode) {
            ObservedThemeMode.Loading -> SettingsState.Loading
            ObservedThemeMode.Failed -> SettingsState.Error(
                message = "Appearance settings could not be loaded.",
                eventSink = eventSink,
            )
            is ObservedThemeMode.Loaded -> SettingsState.Content(
                selectedThemeMode = observed.mode,
                isSaving = isSaving,
                errorMessage = saveError,
                eventSink = eventSink,
            )
        }
    }
}

private sealed interface ObservedThemeMode {
    data object Loading : ObservedThemeMode
    data object Failed : ObservedThemeMode
    data class Loaded(val mode: ThemeMode) : ObservedThemeMode
}
