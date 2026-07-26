package com.topic11.cs426.feature.templates

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.topic11.cs426.core.navigation.InspectionScreen
import com.topic11.cs426.core.navigation.TemplateDetailScreen
import com.topic11.cs426.core.navigation.TemplateEditorScreen
import com.topic11.cs426.domain.model.AssetSummary
import com.topic11.cs426.domain.model.ChecklistAnswerType
import com.topic11.cs426.domain.model.ChecklistItem
import com.topic11.cs426.domain.model.InspectionSection
import com.topic11.cs426.domain.model.InspectionTemplate
import com.topic11.cs426.domain.model.InspectionTemplateSummary
import com.topic11.cs426.domain.model.TemplateId
import com.topic11.cs426.domain.usecase.CreateTemplateUseCase
import com.topic11.cs426.domain.usecase.GetTemplateUseCase
import com.topic11.cs426.domain.usecase.ObserveAssetsUseCase
import com.topic11.cs426.domain.usecase.ObserveTemplateUseCase
import com.topic11.cs426.domain.usecase.ObserveTemplatesUseCase
import com.topic11.cs426.domain.usecase.StartInspectionUseCase
import com.topic11.cs426.domain.usecase.TemplateCreateInput
import com.topic11.cs426.domain.usecase.TemplateMetadataInput
import com.topic11.cs426.domain.usecase.TemplateSaveResult
import com.topic11.cs426.domain.usecase.UpdateTemplateMetadataUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class TemplatesPresenter(
    private val observeTemplates: ObserveTemplatesUseCase,
    private val navigator: Navigator,
) : Presenter<TemplatesState> {
    @Composable
    override fun present(): TemplatesState {
        val eventSink = remember(navigator) {
            { event: TemplatesEvent ->
                when (event) {
                    TemplatesEvent.AddSelected -> navigator.goTo(TemplateEditorScreen(templateId = null))
                    TemplatesEvent.BackSelected -> navigator.pop()
                    is TemplatesEvent.TemplateSelected -> {
                        navigator.goTo(TemplateDetailScreen(event.templateId.value))
                    }
                }
                Unit
            }
        }
        val state by remember(observeTemplates, eventSink) {
            observeTemplates()
                .map<List<InspectionTemplateSummary>, TemplatesState> { templates ->
                    if (templates.isEmpty()) {
                        TemplatesState.Empty(eventSink)
                    } else {
                        TemplatesState.Content(
                            templates = templates.map { it.toListItemUi() },
                            eventSink = eventSink,
                        )
                    }
                }
                .catch {
                    emit(
                        TemplatesState.Error(
                            message = "Templates could not be loaded.",
                            eventSink = eventSink,
                        ),
                    )
                }
        }.collectAsState(initial = TemplatesState.Loading)
        return state
    }
}

internal class TemplateDetailPresenter(
    private val screen: TemplateDetailScreen,
    private val observeTemplate: ObserveTemplateUseCase,
    private val observeAssets: ObserveAssetsUseCase,
    private val startInspection: StartInspectionUseCase,
    private val navigator: Navigator,
) : Presenter<TemplateDetailState> {
    @Composable
    override fun present(): TemplateDetailState {
        val templateId = remember(screen.templateId) { TemplateId(screen.templateId) }
        var isStartVisible by remember(templateId) { mutableStateOf(false) }
        var selectedAssetId by remember(templateId) { mutableStateOf<com.topic11.cs426.domain.model.AssetId?>(null) }
        var isStarting by remember(templateId) { mutableStateOf(false) }
        var startError by remember(templateId) { mutableStateOf<String?>(null) }
        val coroutineScope = rememberCoroutineScope()

        val observedTemplate by remember(observeTemplate, templateId) {
            observeTemplate(templateId)
                .map<InspectionTemplate?, ObservedTemplate> { ObservedTemplate.Loaded(it) }
                .catch { emit(ObservedTemplate.Loaded(null)) }
        }.collectAsState(initial = ObservedTemplate.Loading)
        val assets by remember(observeAssets) {
            observeAssets()
                .map { values -> values.map { it.toOptionUi() } }
                .catch { emit(emptyList()) }
        }.collectAsState(initial = emptyList())

        fun selectedAsset(): AssetOptionUi? =
            assets.firstOrNull { it.id == selectedAssetId }
                ?: assets.firstOrNull()

        val eventSink = remember(
            templateId,
            observedTemplate,
            assets,
            navigator,
            coroutineScope,
            startInspection,
        ) {
            { event: TemplateDetailEvent ->
                when (event) {
                    TemplateDetailEvent.BackSelected -> navigator.pop()
                    TemplateDetailEvent.EditSelected -> navigator.goTo(TemplateEditorScreen(templateId.value))
                    TemplateDetailEvent.StartInspectionSelected -> {
                        selectedAssetId = selectedAsset()?.id
                        startError = null
                        isStartVisible = true
                    }
                    TemplateDetailEvent.StartInspectionDismissed -> {
                        if (!isStarting) {
                            isStartVisible = false
                            startError = null
                        }
                    }
                    is TemplateDetailEvent.AssetSelected -> {
                        selectedAssetId = event.assetId
                        startError = null
                    }
                    TemplateDetailEvent.StartInspectionConfirmed -> {
                        val currentTemplate = (observedTemplate as? ObservedTemplate.Loaded)?.template
                        val asset = selectedAsset()
                        if (currentTemplate == null || asset == null) {
                            startError = "Choose an asset."
                        } else {
                            coroutineScope.launch {
                                try {
                                    isStarting = true
                                    val inspectionId = startInspection(
                                        assetId = asset.id,
                                        assetName = asset.name,
                                        templateId = currentTemplate.id,
                                    )
                                    isStarting = false
                                    isStartVisible = false
                                    startError = null
                                    navigator.goTo(InspectionScreen(inspectionId.value))
                                } catch (exception: Exception) {
                                    if (exception is CancellationException) throw exception
                                    isStarting = false
                                    startError = "Couldn't start inspection."
                                }
                            }
                        }
                    }
                }
                Unit
            }
        }

        return when (val current = observedTemplate) {
            ObservedTemplate.Loading -> TemplateDetailState.Loading
            is ObservedTemplate.Loaded -> {
                val template = current.template
                if (template == null) {
                    TemplateDetailState.Missing(eventSink)
                } else {
                    TemplateDetailState.Content(
                        template = template.toDetailUi(),
                        startInspection = TemplateStartInspectionUi(
                            isVisible = isStartVisible,
                            isStarting = isStarting,
                            assets = assets,
                            selectedAssetId = selectedAsset()?.id,
                            errorMessage = startError,
                        ),
                        eventSink = eventSink,
                    )
                }
            }
        }
    }
}

internal class TemplateEditorPresenter(
    private val screen: TemplateEditorScreen,
    private val getTemplate: GetTemplateUseCase,
    private val createTemplate: CreateTemplateUseCase,
    private val updateTemplateMetadata: UpdateTemplateMetadataUseCase,
    private val navigator: Navigator,
) : Presenter<TemplateEditorState> {
    @Composable
    override fun present(): TemplateEditorState {
        val templateId = remember(screen.templateId) { screen.templateId?.let(::TemplateId) }
        var isLoaded by remember(templateId) { mutableStateOf(templateId == null) }
        var isMissing by remember(templateId) { mutableStateOf(false) }
        var form by remember(templateId) {
            mutableStateOf(
                TemplateFormUi(
                    name = "",
                    recurrenceDays = "",
                    sectionTitle = "General",
                    itemTitle = "",
                    itemDescription = "",
                    required = true,
                    critical = false,
                    weight = "1",
                    answerType = ChecklistAnswerType.PASS_FAIL_NA,
                ),
            )
        }
        var isSaving by remember(templateId) { mutableStateOf(false) }
        var validationMessages by remember(templateId) { mutableStateOf(emptyList<String>()) }
        var saveError by remember(templateId) { mutableStateOf<String?>(null) }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(templateId) {
            if (templateId == null) {
                isLoaded = true
                isMissing = false
            } else {
                isLoaded = false
                val template = getTemplate(templateId)
                if (template == null) {
                    isMissing = true
                } else {
                    form = form.copy(
                        name = template.name,
                        recurrenceDays = template.recurrencePolicyDays?.toString().orEmpty(),
                    )
                    isMissing = false
                }
                isLoaded = true
            }
        }

        val eventSink = remember(
            navigator,
            coroutineScope,
            templateId,
            form,
            createTemplate,
            updateTemplateMetadata,
        ) {
            { event: TemplateEditorEvent ->
                when (event) {
                    TemplateEditorEvent.BackSelected -> navigator.pop()
                    is TemplateEditorEvent.AnswerTypeSelected -> {
                        form = form.copy(answerType = event.value)
                        validationMessages = emptyList()
                    }
                    is TemplateEditorEvent.CriticalChanged -> {
                        form = form.copy(critical = event.value)
                        validationMessages = emptyList()
                    }
                    is TemplateEditorEvent.ItemDescriptionChanged -> {
                        form = form.copy(itemDescription = event.value)
                    }
                    is TemplateEditorEvent.ItemTitleChanged -> {
                        form = form.copy(itemTitle = event.value)
                        validationMessages = emptyList()
                    }
                    is TemplateEditorEvent.NameChanged -> {
                        form = form.copy(name = event.value)
                        validationMessages = emptyList()
                    }
                    is TemplateEditorEvent.RecurrenceChanged -> {
                        form = form.copy(recurrenceDays = event.value)
                        validationMessages = emptyList()
                    }
                    is TemplateEditorEvent.RequiredChanged -> {
                        form = form.copy(required = event.value)
                    }
                    is TemplateEditorEvent.SectionTitleChanged -> {
                        form = form.copy(sectionTitle = event.value)
                        validationMessages = emptyList()
                    }
                    is TemplateEditorEvent.WeightChanged -> {
                        form = form.copy(weight = event.value)
                        validationMessages = emptyList()
                    }
                    TemplateEditorEvent.SaveSelected -> {
                        coroutineScope.launch {
                            try {
                                isSaving = true
                                validationMessages = emptyList()
                                saveError = null
                                val recurrence = form.recurrenceDays.toPositiveIntOrNull("Recurrence interval")
                                val weight = form.weight.toNonNegativeIntOrNull("Checklist item weight")
                                val parseErrors = buildList {
                                    recurrence.error?.let(::add)
                                    if (templateId == null) weight.error?.let(::add)
                                }
                                if (parseErrors.isNotEmpty()) {
                                    isSaving = false
                                    validationMessages = parseErrors
                                    return@launch
                                }
                                val result = if (templateId == null) {
                                    createTemplate(
                                        TemplateCreateInput(
                                            name = form.name,
                                            recurrencePolicyDays = recurrence.value,
                                            sectionTitle = form.sectionTitle,
                                            itemTitle = form.itemTitle,
                                            itemDescription = form.itemDescription,
                                            required = form.required,
                                            critical = form.critical,
                                            weight = requireNotNull(weight.value),
                                            answerType = form.answerType,
                                        ),
                                    )
                                } else {
                                    updateTemplateMetadata(
                                        templateId,
                                        TemplateMetadataInput(
                                            name = form.name,
                                            recurrencePolicyDays = recurrence.value,
                                        ),
                                    )
                                }
                                isSaving = false
                                when (result) {
                                    is TemplateSaveResult.Success -> navigator.pop()
                                    is TemplateSaveResult.ValidationFailed -> {
                                        validationMessages = result.errors.map { it.message }
                                    }
                                    TemplateSaveResult.NotFound -> {
                                        saveError = "Template could not be found."
                                    }
                                }
                            } catch (exception: Exception) {
                                if (exception is CancellationException) throw exception
                                isSaving = false
                                saveError = "Template could not be saved."
                            }
                        }
                    }
                }
                Unit
            }
        }

        return when {
            !isLoaded -> TemplateEditorState.Loading
            isMissing -> TemplateEditorState.Missing(eventSink)
            else -> TemplateEditorState.Editing(
                title = if (templateId == null) "Add template" else "Edit template",
                isCreate = templateId == null,
                form = form,
                isSaving = isSaving,
                validationMessages = validationMessages,
                saveErrorMessage = saveError,
                eventSink = eventSink,
            )
        }
    }
}

private fun InspectionTemplateSummary.toListItemUi() = TemplateListItemUi(
    id = id,
    name = name,
    versionLabel = "Version $version",
    sectionCountLabel = "$sectionCount sections",
)

private fun InspectionTemplate.toDetailUi() = TemplateDetailUi(
    id = id,
    name = name,
    versionLabel = "Version $version",
    recurrenceLabel = recurrencePolicyDays?.let { "Every $it days" } ?: "No recurrence",
    sections = sections
        .sortedWith(compareBy({ it.order }, { it.id.value }))
        .map { it.toUi() },
)

private fun InspectionSection.toUi() = TemplateSectionUi(
    title = title,
    items = items.map { it.toUi() },
)

private fun ChecklistItem.toUi() = TemplateChecklistItemUi(
    title = title,
    description = description,
    required = required,
    critical = critical,
    weightLabel = "Weight $weight",
    answerTypeLabel = answerType.displayLabel(),
)

private fun ChecklistAnswerType.displayLabel(): String {
    return when (this) {
        ChecklistAnswerType.PASS_FAIL_NA -> "Pass / Fail / N/A"
        ChecklistAnswerType.YES_NO -> "Yes / No"
        ChecklistAnswerType.TEXT -> "Text"
        ChecklistAnswerType.NUMBER -> "Number"
        ChecklistAnswerType.SINGLE_CHOICE -> "Single choice"
    }
}

private fun AssetSummary.toOptionUi() = AssetOptionUi(
    id = id,
    name = name,
    detail = listOfNotNull(code, locationName).takeIf { it.isNotEmpty() }?.joinToString(" • "),
)

private sealed interface ObservedTemplate {
    data object Loading : ObservedTemplate
    data class Loaded(val template: InspectionTemplate?) : ObservedTemplate
}

private data class ParsedInt(
    val value: Int?,
    val error: String?,
)

private fun String.toPositiveIntOrNull(label: String): ParsedInt {
    val trimmed = trim()
    if (trimmed.isEmpty()) return ParsedInt(null, null)
    val value = trimmed.toIntOrNull()
    return if (value == null || value <= 0) {
        ParsedInt(null, "$label must be a positive whole number.")
    } else {
        ParsedInt(value, null)
    }
}

private fun String.toNonNegativeIntOrNull(label: String): ParsedInt {
    val value = trim().toIntOrNull()
    return if (value == null || value < 0) {
        ParsedInt(null, "$label must be zero or greater.")
    } else {
        ParsedInt(value, null)
    }
}
