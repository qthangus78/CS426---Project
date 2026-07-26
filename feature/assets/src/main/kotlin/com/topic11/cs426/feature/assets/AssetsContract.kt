package com.topic11.cs426.feature.assets

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.LocationId
import com.topic11.cs426.domain.model.TemplateId

@Immutable
sealed interface AssetsState : CircuitUiState {
    data object Loading : AssetsState

    @Immutable
    data class Empty(
        val query: String,
        val eventSink: (AssetsEvent) -> Unit,
    ) : AssetsState

    @Immutable
    data class Content(
        val assets: List<AssetListItemUi>,
        val query: String,
        val hasNoSearchResults: Boolean,
        val eventSink: (AssetsEvent) -> Unit,
    ) : AssetsState

    @Immutable
    data class Error(
        val message: String,
        val eventSink: (AssetsEvent) -> Unit,
    ) : AssetsState
}

@Immutable
data class AssetListItemUi(
    val id: AssetId,
    val name: String,
    val code: String?,
    val locationName: String?,
    val nextInspectionDueLabel: String?,
)

sealed interface AssetsEvent : CircuitUiEvent {
    data object BackSelected : AssetsEvent
    data object AddSelected : AssetsEvent
    data class AssetSelected(val assetId: AssetId) : AssetsEvent
    data class SearchQueryChanged(val query: String) : AssetsEvent
    data object SearchCleared : AssetsEvent
}

@Immutable
sealed interface AssetDetailState : CircuitUiState {
    data object Loading : AssetDetailState

    @Immutable
    data class Content(
        val asset: AssetDetailUi,
        val startInspection: AssetStartInspectionUi,
        val eventSink: (AssetDetailEvent) -> Unit,
    ) : AssetDetailState

    @Immutable
    data class Missing(
        val eventSink: (AssetDetailEvent) -> Unit,
    ) : AssetDetailState
}

@Immutable
data class AssetDetailUi(
    val id: AssetId,
    val name: String,
    val code: String?,
    val locationName: String,
    val nextInspectionDueLabel: String?,
)

@Immutable
data class AssetStartInspectionUi(
    val isVisible: Boolean,
    val isStarting: Boolean,
    val templates: List<TemplateOptionUi>,
    val selectedTemplateId: TemplateId?,
    val errorMessage: String?,
) {
    val canStart: Boolean
        get() = !isStarting && selectedTemplateId != null
}

@Immutable
data class TemplateOptionUi(
    val id: TemplateId,
    val name: String,
    val detail: String,
)

sealed interface AssetDetailEvent : CircuitUiEvent {
    data object BackSelected : AssetDetailEvent
    data object EditSelected : AssetDetailEvent
    data object StartInspectionSelected : AssetDetailEvent
    data object StartInspectionDismissed : AssetDetailEvent
    data class TemplateSelected(val templateId: TemplateId) : AssetDetailEvent
    data object StartInspectionConfirmed : AssetDetailEvent
}

@Immutable
sealed interface AssetEditorState : CircuitUiState {
    data object Loading : AssetEditorState

    @Immutable
    data class Editing(
        val title: String,
        val form: AssetFormUi,
        val locations: List<LocationOptionUi>,
        val isSaving: Boolean,
        val validationMessages: List<String>,
        val saveErrorMessage: String?,
        val eventSink: (AssetEditorEvent) -> Unit,
    ) : AssetEditorState

    @Immutable
    data class Missing(
        val eventSink: (AssetEditorEvent) -> Unit,
    ) : AssetEditorState
}

@Immutable
data class AssetFormUi(
    val name: String,
    val code: String,
    val selectedLocationId: LocationId?,
)

@Immutable
data class LocationOptionUi(
    val id: LocationId,
    val name: String,
)

sealed interface AssetEditorEvent : CircuitUiEvent {
    data object BackSelected : AssetEditorEvent
    data class NameChanged(val value: String) : AssetEditorEvent
    data class CodeChanged(val value: String) : AssetEditorEvent
    data class LocationSelected(val locationId: LocationId) : AssetEditorEvent
    data object SaveSelected : AssetEditorEvent
}
