package com.topic11.cs426.feature.templates

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.ChecklistAnswerType
import com.topic11.cs426.domain.model.TemplateId

@Immutable
sealed interface TemplatesState : CircuitUiState {
    data object Loading : TemplatesState

    @Immutable
    data class Empty(val eventSink: (TemplatesEvent) -> Unit) : TemplatesState

    @Immutable
    data class Content(
        val templates: List<TemplateListItemUi>,
        val eventSink: (TemplatesEvent) -> Unit,
    ) : TemplatesState

    @Immutable
    data class Error(
        val message: String,
        val eventSink: (TemplatesEvent) -> Unit,
    ) : TemplatesState
}

@Immutable
data class TemplateListItemUi(
    val id: TemplateId,
    val name: String,
    val versionLabel: String,
    val sectionCountLabel: String,
)

sealed interface TemplatesEvent : CircuitUiEvent {
    data object BackSelected : TemplatesEvent
    data object AddSelected : TemplatesEvent
    data class TemplateSelected(val templateId: TemplateId) : TemplatesEvent
}

@Immutable
sealed interface TemplateDetailState : CircuitUiState {
    data object Loading : TemplateDetailState

    @Immutable
    data class Content(
        val template: TemplateDetailUi,
        val startInspection: TemplateStartInspectionUi,
        val eventSink: (TemplateDetailEvent) -> Unit,
    ) : TemplateDetailState

    @Immutable
    data class Missing(val eventSink: (TemplateDetailEvent) -> Unit) : TemplateDetailState
}

@Immutable
data class TemplateDetailUi(
    val id: TemplateId,
    val name: String,
    val versionLabel: String,
    val recurrenceLabel: String,
    val sections: List<TemplateSectionUi>,
)

@Immutable
data class TemplateSectionUi(
    val title: String,
    val items: List<TemplateChecklistItemUi>,
)

@Immutable
data class TemplateChecklistItemUi(
    val title: String,
    val description: String?,
    val required: Boolean,
    val critical: Boolean,
    val weightLabel: String,
    val answerTypeLabel: String,
)

@Immutable
data class TemplateStartInspectionUi(
    val isVisible: Boolean,
    val isStarting: Boolean,
    val assets: List<AssetOptionUi>,
    val selectedAssetId: AssetId?,
    val errorMessage: String?,
) {
    val canStart: Boolean
        get() = !isStarting && selectedAssetId != null
}

@Immutable
data class AssetOptionUi(
    val id: AssetId,
    val name: String,
    val detail: String?,
)

sealed interface TemplateDetailEvent : CircuitUiEvent {
    data object BackSelected : TemplateDetailEvent
    data object EditSelected : TemplateDetailEvent
    data object StartInspectionSelected : TemplateDetailEvent
    data object StartInspectionDismissed : TemplateDetailEvent
    data class AssetSelected(val assetId: AssetId) : TemplateDetailEvent
    data object StartInspectionConfirmed : TemplateDetailEvent
}

@Immutable
sealed interface TemplateEditorState : CircuitUiState {
    data object Loading : TemplateEditorState

    @Immutable
    data class Editing(
        val title: String,
        val isCreate: Boolean,
        val form: TemplateFormUi,
        val isSaving: Boolean,
        val validationMessages: List<String>,
        val saveErrorMessage: String?,
        val eventSink: (TemplateEditorEvent) -> Unit,
    ) : TemplateEditorState

    @Immutable
    data class Missing(val eventSink: (TemplateEditorEvent) -> Unit) : TemplateEditorState
}

@Immutable
data class TemplateFormUi(
    val name: String,
    val recurrenceDays: String,
    val sectionTitle: String,
    val itemTitle: String,
    val itemDescription: String,
    val required: Boolean,
    val critical: Boolean,
    val weight: String,
    val answerType: ChecklistAnswerType,
)

sealed interface TemplateEditorEvent : CircuitUiEvent {
    data object BackSelected : TemplateEditorEvent
    data class NameChanged(val value: String) : TemplateEditorEvent
    data class RecurrenceChanged(val value: String) : TemplateEditorEvent
    data class SectionTitleChanged(val value: String) : TemplateEditorEvent
    data class ItemTitleChanged(val value: String) : TemplateEditorEvent
    data class ItemDescriptionChanged(val value: String) : TemplateEditorEvent
    data class RequiredChanged(val value: Boolean) : TemplateEditorEvent
    data class CriticalChanged(val value: Boolean) : TemplateEditorEvent
    data class WeightChanged(val value: String) : TemplateEditorEvent
    data class AnswerTypeSelected(val value: ChecklistAnswerType) : TemplateEditorEvent
    data object SaveSelected : TemplateEditorEvent
}
