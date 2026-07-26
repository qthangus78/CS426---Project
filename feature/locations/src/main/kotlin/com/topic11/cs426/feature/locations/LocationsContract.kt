package com.topic11.cs426.feature.locations

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.topic11.cs426.domain.model.LocationId

@Immutable
internal sealed interface LocationsState : CircuitUiState {
    data object Loading : LocationsState

    @Immutable
    data class Empty(
        val query: String,
        val eventSink: (LocationsEvent) -> Unit,
    ) : LocationsState

    @Immutable
    data class Content(
        val locations: List<LocationListItemUi>,
        val query: String,
        val hasNoSearchResults: Boolean,
        val eventSink: (LocationsEvent) -> Unit,
    ) : LocationsState

    @Immutable
    data class Error(
        val message: String,
        val eventSink: (LocationsEvent) -> Unit,
    ) : LocationsState
}

@Immutable
internal data class LocationListItemUi(
    val id: LocationId,
    val name: String,
    val parentName: String?,
)

internal sealed interface LocationsEvent : CircuitUiEvent {
    data object BackSelected : LocationsEvent
    data object AddSelected : LocationsEvent
    data class LocationSelected(val locationId: LocationId) : LocationsEvent
    data class SearchQueryChanged(val query: String) : LocationsEvent
    data object SearchCleared : LocationsEvent
}

@Immutable
internal sealed interface LocationDetailState : CircuitUiState {
    data object Loading : LocationDetailState

    @Immutable
    data class Content(
        val location: LocationDetailUi,
        val eventSink: (LocationDetailEvent) -> Unit,
    ) : LocationDetailState

    @Immutable
    data class Missing(
        val eventSink: (LocationDetailEvent) -> Unit,
    ) : LocationDetailState
}

@Immutable
internal data class LocationDetailUi(
    val id: LocationId,
    val name: String,
    val parentName: String?,
)

internal sealed interface LocationDetailEvent : CircuitUiEvent {
    data object BackSelected : LocationDetailEvent
    data object EditSelected : LocationDetailEvent
}

@Immutable
internal sealed interface LocationEditorState : CircuitUiState {
    data object Loading : LocationEditorState

    @Immutable
    data class Editing(
        val title: String,
        val form: LocationFormUi,
        val parentOptions: List<LocationParentOptionUi>,
        val isSaving: Boolean,
        val validationMessages: List<String>,
        val saveErrorMessage: String?,
        val eventSink: (LocationEditorEvent) -> Unit,
    ) : LocationEditorState

    @Immutable
    data class Missing(
        val eventSink: (LocationEditorEvent) -> Unit,
    ) : LocationEditorState
}

@Immutable
internal data class LocationFormUi(
    val name: String,
    val parentId: LocationId?,
)

@Immutable
internal data class LocationParentOptionUi(
    val id: LocationId,
    val name: String,
)

internal sealed interface LocationEditorEvent : CircuitUiEvent {
    data object BackSelected : LocationEditorEvent
    data class NameChanged(val value: String) : LocationEditorEvent
    data class ParentSelected(val locationId: LocationId?) : LocationEditorEvent
    data object SaveSelected : LocationEditorEvent
}
