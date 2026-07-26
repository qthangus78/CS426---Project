package com.topic11.cs426.feature.settings

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.topic11.cs426.domain.model.ThemeMode

@Immutable
internal sealed interface SettingsState : CircuitUiState {
    data object Loading : SettingsState

    @Immutable
    data class Content(
        val selectedThemeMode: ThemeMode,
        val isSaving: Boolean,
        val errorMessage: String?,
        val eventSink: (SettingsEvent) -> Unit,
    ) : SettingsState

    @Immutable
    data class Error(
        val message: String,
        val eventSink: (SettingsEvent) -> Unit,
    ) : SettingsState
}

internal sealed interface SettingsEvent : CircuitUiEvent {
    data object BackSelected : SettingsEvent
    data object RetrySelected : SettingsEvent
    data class ThemeModeSelected(val mode: ThemeMode) : SettingsEvent
}
