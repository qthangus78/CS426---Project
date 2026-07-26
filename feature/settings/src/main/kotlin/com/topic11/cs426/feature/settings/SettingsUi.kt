package com.topic11.cs426.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.topic11.cs426.core.designsystem.EmptyState
import com.topic11.cs426.core.designsystem.FieldFlowTopAppBar
import com.topic11.cs426.core.designsystem.LoadingContent
import com.topic11.cs426.domain.model.ThemeMode

@Composable
internal fun SettingsUi(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("settings-root"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FieldFlowTopAppBar(
                title = "Settings",
                onBackClick = state.eventSinkOrNull()?.let { eventSink ->
                    { eventSink(SettingsEvent.BackSelected) }
                },
            )
        },
    ) { innerPadding ->
        when (state) {
            SettingsState.Loading -> LoadingContent(
                label = "Loading settings",
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(20.dp),
            )
            is SettingsState.Error -> {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    EmptyState(
                        title = "Settings unavailable",
                        message = state.message,
                    )
                    OutlinedButton(onClick = { state.eventSink(SettingsEvent.RetrySelected) }) {
                        Text("Retry")
                    }
                }
            }
            is SettingsState.Content -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .testTag("settings-content"),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        AppearanceCard(state = state)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceCard(state: SettingsState.Content) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
            )
            ThemeMode.entries.forEach { mode ->
                ThemeModeRow(
                    mode = mode,
                    selected = mode == state.selectedThemeMode,
                    enabled = !state.isSaving,
                    onClick = { state.eventSink(SettingsEvent.ThemeModeSelected(mode)) },
                )
            }
            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (state.isSaving) {
                Text(
                    text = "Saving setting",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ThemeModeRow(
    mode: ThemeMode,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mode.title(),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = mode.description(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun ThemeMode.title(): String = when (this) {
    ThemeMode.SYSTEM -> "Use system setting"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private fun ThemeMode.description(): String = when (this) {
    ThemeMode.SYSTEM -> "Match this device."
    ThemeMode.LIGHT -> "Use the light appearance."
    ThemeMode.DARK -> "Use the dark appearance."
}

private fun SettingsState.eventSinkOrNull(): ((SettingsEvent) -> Unit)? = when (this) {
    SettingsState.Loading -> null
    is SettingsState.Content -> eventSink
    is SettingsState.Error -> eventSink
}
