package com.topic11.cs426.feature.assets

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.topic11.cs426.core.designsystem.EmptyState
import com.topic11.cs426.core.designsystem.FieldFlowTopAppBar
import com.topic11.cs426.core.designsystem.LoadingContent

@Composable
internal fun AssetsUi(
    state: AssetsState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("assets-root"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FieldFlowTopAppBar(
                title = "Assets",
                onBackClick = state.eventSinkOrNull()?.let { eventSink ->
                    { eventSink(AssetsEvent.BackSelected) }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("assets-content"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state) {
                AssetsState.Loading -> {
                    item {
                        LoadingContent(label = "Loading assets")
                    }
                }
                is AssetsState.Empty -> {
                    item {
                        ListHeader(
                            title = "Assets",
                            actionLabel = "Add asset",
                            onAction = { state.eventSink(AssetsEvent.AddSelected) },
                        )
                    }
                    item {
                        EmptyState(
                            title = "No assets yet",
                            message = "Add a room or piece of equipment to make it available for inspections.",
                        )
                    }
                }
                is AssetsState.Content -> {
                    item {
                        ListHeader(
                            title = "Assets",
                            actionLabel = "Add asset",
                            onAction = { state.eventSink(AssetsEvent.AddSelected) },
                        )
                    }
                    items(
                        items = state.assets,
                        key = { asset -> asset.id.value },
                    ) { asset ->
                        AssetListCard(
                            asset = asset,
                            onClick = { state.eventSink(AssetsEvent.AssetSelected(asset.id)) },
                        )
                    }
                }
                is AssetsState.Error -> {
                    item {
                        EmptyState(
                            title = "Assets unavailable",
                            message = state.message,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AssetDetailUi(
    state: AssetDetailState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("asset-detail-root"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FieldFlowTopAppBar(
                title = "Asset details",
                onBackClick = state.eventSinkOrNull()?.let { eventSink ->
                    { eventSink(AssetDetailEvent.BackSelected) }
                },
            )
        },
    ) { innerPadding ->
        when (state) {
            AssetDetailState.Loading -> {
                LoadingContent(
                    label = "Loading asset",
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(20.dp),
                )
            }
            is AssetDetailState.Missing -> {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(20.dp),
                ) {
                    EmptyState(
                        title = "Asset not found",
                        message = "This asset may have been removed.",
                    )
                }
            }
            is AssetDetailState.Content -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        DetailCard(asset = state.asset)
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedButton(
                                onClick = { state.eventSink(AssetDetailEvent.EditSelected) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Edit asset")
                            }
                            Button(
                                onClick = { state.eventSink(AssetDetailEvent.StartInspectionSelected) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Start inspection")
                            }
                        }
                    }
                }
                if (state.startInspection.isVisible) {
                    AssetStartInspectionDialog(
                        state = state.startInspection,
                        eventSink = state.eventSink,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AssetEditorUi(
    state: AssetEditorState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("asset-editor-root"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FieldFlowTopAppBar(
                title = when (state) {
                    is AssetEditorState.Editing -> state.title
                    else -> "Asset"
                },
                onBackClick = state.eventSinkOrNull()?.let { eventSink ->
                    { eventSink(AssetEditorEvent.BackSelected) }
                },
            )
        },
    ) { innerPadding ->
        when (state) {
            AssetEditorState.Loading -> {
                LoadingContent(
                    label = "Loading asset",
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(20.dp),
                )
            }
            is AssetEditorState.Missing -> {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(20.dp),
                ) {
                    EmptyState(
                        title = "Asset not found",
                        message = "This asset may have been removed.",
                    )
                }
            }
            is AssetEditorState.Editing -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        AssetForm(
                            state = state,
                            eventSink = state.eventSink,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )
        Button(onClick = onAction) {
            Text(actionLabel)
        }
    }
}

@Composable
private fun AssetListCard(
    asset: AssetListItemUi,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("asset-card-${asset.id.value}"),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = asset.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            MetadataLine(
                values = listOfNotNull(asset.code, asset.locationName, asset.nextInspectionDueLabel),
            )
        }
    }
}

@Composable
private fun DetailCard(asset: AssetDetailUi) {
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
                text = asset.name,
                style = MaterialTheme.typography.headlineSmall,
            )
            DetailRow(label = "Code", value = asset.code ?: "No code")
            DetailRow(label = "Location", value = asset.locationName)
            DetailRow(label = "Next inspection", value = asset.nextInspectionDueLabel ?: "Not scheduled")
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun MetadataLine(values: List<String>) {
    Text(
        text = values.takeIf { it.isNotEmpty() }?.joinToString(" • ") ?: "No metadata",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun AssetForm(
    state: AssetEditorState.Editing,
    eventSink: (AssetEditorEvent) -> Unit,
) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            state.validationMessages.forEach { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            state.saveErrorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedTextField(
                value = state.form.name,
                onValueChange = { eventSink(AssetEditorEvent.NameChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Asset name") },
                singleLine = true,
                enabled = !state.isSaving,
            )
            OutlinedTextField(
                value = state.form.code,
                onValueChange = { eventSink(AssetEditorEvent.CodeChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Asset code") },
                singleLine = true,
                enabled = !state.isSaving,
            )
            Text(
                text = "Location",
                style = MaterialTheme.typography.labelLarge,
            )
            if (state.locations.isEmpty()) {
                Text(
                    text = "No locations available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                state.locations.forEach { location ->
                    LocationRow(
                        location = location,
                        selected = location.id == state.form.selectedLocationId,
                        enabled = !state.isSaving,
                        onClick = { eventSink(AssetEditorEvent.LocationSelected(location.id)) },
                    )
                }
            }
            Button(
                onClick = { eventSink(AssetEditorEvent.SaveSelected) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isSaving) "Saving..." else "Save asset")
            }
        }
    }
}

@Composable
private fun LocationRow(
    location: LocationOptionUi,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled,
        )
        Text(
            text = location.name,
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun AssetStartInspectionDialog(
    state: AssetStartInspectionUi,
    eventSink: (AssetDetailEvent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { eventSink(AssetDetailEvent.StartInspectionDismissed) },
        title = { Text("Start inspection") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (state.templates.isEmpty()) {
                    Text(
                        text = "No templates available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.templates.forEach { template ->
                        TemplateRow(
                            template = template,
                            selected = template.id == state.selectedTemplateId,
                            onClick = { eventSink(AssetDetailEvent.TemplateSelected(template.id)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = state.canStart,
                onClick = { eventSink(AssetDetailEvent.StartInspectionConfirmed) },
            ) {
                Text(if (state.isStarting) "Starting..." else "Start")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !state.isStarting,
                onClick = { eventSink(AssetDetailEvent.StartInspectionDismissed) },
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun TemplateRow(
    template: TemplateOptionUi,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        ) {
            Text(
                text = template.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = template.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun AssetsState.eventSinkOrNull(): ((AssetsEvent) -> Unit)? {
    return when (this) {
        AssetsState.Loading -> null
        is AssetsState.Content -> eventSink
        is AssetsState.Empty -> eventSink
        is AssetsState.Error -> eventSink
    }
}

private fun AssetDetailState.eventSinkOrNull(): ((AssetDetailEvent) -> Unit)? {
    return when (this) {
        AssetDetailState.Loading -> null
        is AssetDetailState.Content -> eventSink
        is AssetDetailState.Missing -> eventSink
    }
}

private fun AssetEditorState.eventSinkOrNull(): ((AssetEditorEvent) -> Unit)? {
    return when (this) {
        AssetEditorState.Loading -> null
        is AssetEditorState.Editing -> eventSink
        is AssetEditorState.Missing -> eventSink
    }
}
