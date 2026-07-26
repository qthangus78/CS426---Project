package com.topic11.cs426.feature.locations

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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

@Composable
internal fun LocationsUi(
    state: LocationsState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("locations-root"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FieldFlowTopAppBar(
                title = "Locations",
                onBackClick = state.eventSinkOrNull()?.let { eventSink ->
                    { eventSink(LocationsEvent.BackSelected) }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("locations-content"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state) {
                LocationsState.Loading -> item {
                    LoadingContent(label = "Loading locations")
                }
                is LocationsState.Empty -> {
                    item {
                        LocationsHeader(
                            query = state.query,
                            onQueryChanged = { state.eventSink(LocationsEvent.SearchQueryChanged(it)) },
                            onClearQuery = { state.eventSink(LocationsEvent.SearchCleared) },
                            onAdd = { state.eventSink(LocationsEvent.AddSelected) },
                        )
                    }
                    item {
                        EmptyState(
                            title = "No locations yet",
                            message = "Add a campus, building, room, or area before creating assets.",
                        )
                    }
                }
                is LocationsState.Content -> {
                    item {
                        LocationsHeader(
                            query = state.query,
                            onQueryChanged = { state.eventSink(LocationsEvent.SearchQueryChanged(it)) },
                            onClearQuery = { state.eventSink(LocationsEvent.SearchCleared) },
                            onAdd = { state.eventSink(LocationsEvent.AddSelected) },
                        )
                    }
                    if (state.hasNoSearchResults) {
                        item {
                            EmptyState(
                                title = "No locations match this search",
                                message = "Clear the search to view all saved locations.",
                            )
                        }
                    } else {
                        items(
                            items = state.locations,
                            key = { location -> location.id.value },
                        ) { location ->
                            LocationListCard(
                                location = location,
                                onClick = { state.eventSink(LocationsEvent.LocationSelected(location.id)) },
                            )
                        }
                    }
                }
                is LocationsState.Error -> item {
                    EmptyState(
                        title = "Locations unavailable",
                        message = state.message,
                    )
                }
            }
        }
    }
}

@Composable
internal fun LocationDetailUi(
    state: LocationDetailState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("location-detail-root"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FieldFlowTopAppBar(
                title = "Location details",
                onBackClick = state.eventSinkOrNull()?.let { eventSink ->
                    { eventSink(LocationDetailEvent.BackSelected) }
                },
            )
        },
    ) { innerPadding ->
        when (state) {
            LocationDetailState.Loading -> LoadingContent(
                label = "Loading location",
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(20.dp),
            )
            is LocationDetailState.Missing -> {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(20.dp),
                ) {
                    EmptyState(
                        title = "Location not found",
                        message = "This location may have been removed.",
                    )
                }
            }
            is LocationDetailState.Content -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        LocationDetailCard(location = state.location)
                    }
                    item {
                        Button(
                            onClick = { state.eventSink(LocationDetailEvent.EditSelected) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Edit location")
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun LocationEditorUi(
    state: LocationEditorState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("location-editor-root"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FieldFlowTopAppBar(
                title = when (state) {
                    is LocationEditorState.Editing -> state.title
                    else -> "Location"
                },
                onBackClick = state.eventSinkOrNull()?.let { eventSink ->
                    { eventSink(LocationEditorEvent.BackSelected) }
                },
            )
        },
    ) { innerPadding ->
        when (state) {
            LocationEditorState.Loading -> LoadingContent(
                label = "Loading location",
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(20.dp),
            )
            is LocationEditorState.Missing -> {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(20.dp),
                ) {
                    EmptyState(
                        title = "Location not found",
                        message = "This location may have been removed.",
                    )
                }
            }
            is LocationEditorState.Editing -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        LocationForm(
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
private fun LocationsHeader(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onAdd: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Locations",
                style = MaterialTheme.typography.titleLarge,
            )
            Button(onClick = onAdd) {
                Text("Add location")
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search locations") },
            singleLine = true,
            trailingIcon = {
                if (query.isNotBlank()) {
                    OutlinedButton(onClick = onClearQuery) {
                        Text("Clear")
                    }
                }
            },
        )
    }
}

@Composable
private fun LocationListCard(
    location: LocationListItemUi,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("location-card-${location.id.value}"),
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
                text = location.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = location.parentName?.let { "Inside $it" } ?: "Top-level location",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LocationDetailCard(location: LocationDetailUi) {
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
                text = location.name,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = location.parentName?.let { "Inside $it" } ?: "Top-level location",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LocationForm(
    state: LocationEditorState.Editing,
    eventSink: (LocationEditorEvent) -> Unit,
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
                onValueChange = { eventSink(LocationEditorEvent.NameChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Location name") },
                singleLine = true,
                enabled = !state.isSaving,
            )
            Text(
                text = "Parent location",
                style = MaterialTheme.typography.labelLarge,
            )
            ParentLocationRow(
                label = "None",
                selected = state.form.parentId == null,
                enabled = !state.isSaving,
                onClick = { eventSink(LocationEditorEvent.ParentSelected(null)) },
            )
            state.parentOptions.forEach { option ->
                ParentLocationRow(
                    label = option.name,
                    selected = state.form.parentId == option.id,
                    enabled = !state.isSaving,
                    onClick = { eventSink(LocationEditorEvent.ParentSelected(option.id)) },
                )
            }
            Button(
                onClick = { eventSink(LocationEditorEvent.SaveSelected) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isSaving) "Saving..." else "Save location")
            }
        }
    }
}

@Composable
private fun ParentLocationRow(
    label: String,
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
            text = label,
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun LocationsState.eventSinkOrNull(): ((LocationsEvent) -> Unit)? = when (this) {
    LocationsState.Loading -> null
    is LocationsState.Content -> eventSink
    is LocationsState.Empty -> eventSink
    is LocationsState.Error -> eventSink
}

private fun LocationDetailState.eventSinkOrNull(): ((LocationDetailEvent) -> Unit)? = when (this) {
    LocationDetailState.Loading -> null
    is LocationDetailState.Content -> eventSink
    is LocationDetailState.Missing -> eventSink
}

private fun LocationEditorState.eventSinkOrNull(): ((LocationEditorEvent) -> Unit)? = when (this) {
    LocationEditorState.Loading -> null
    is LocationEditorState.Editing -> eventSink
    is LocationEditorState.Missing -> eventSink
}
