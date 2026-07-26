package com.topic11.cs426.feature.assets

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
import com.topic11.cs426.core.navigation.AssetDetailScreen
import com.topic11.cs426.core.navigation.AssetEditorScreen
import com.topic11.cs426.core.navigation.InspectionScreen
import com.topic11.cs426.domain.model.Asset
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.AssetSummary
import com.topic11.cs426.domain.model.InspectionTemplateSummary
import com.topic11.cs426.domain.model.Location
import com.topic11.cs426.domain.model.TemplateId
import com.topic11.cs426.domain.usecase.AssetInput
import com.topic11.cs426.domain.usecase.AssetSaveResult
import com.topic11.cs426.domain.usecase.CreateAssetUseCase
import com.topic11.cs426.domain.usecase.GetAssetUseCase
import com.topic11.cs426.domain.usecase.ObserveAssetsUseCase
import com.topic11.cs426.domain.usecase.ObserveLocationsUseCase
import com.topic11.cs426.domain.usecase.ObserveTemplatesUseCase
import com.topic11.cs426.domain.usecase.StartInspectionUseCase
import com.topic11.cs426.domain.usecase.UpdateAssetUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class AssetsPresenter(
    private val observeAssets: ObserveAssetsUseCase,
    private val navigator: Navigator,
) : Presenter<AssetsState> {
    @Composable
    override fun present(): AssetsState {
        val eventSink = remember(navigator) {
            { event: AssetsEvent ->
                when (event) {
                    AssetsEvent.AddSelected -> navigator.goTo(AssetEditorScreen(assetId = null))
                    is AssetsEvent.AssetSelected -> navigator.goTo(AssetDetailScreen(event.assetId.value))
                    AssetsEvent.BackSelected -> navigator.pop()
                }
                Unit
            }
        }
        val state by remember(observeAssets, eventSink) {
            observeAssets()
                .map<List<AssetSummary>, AssetsState> { assets ->
                    if (assets.isEmpty()) {
                        AssetsState.Empty(eventSink)
                    } else {
                        AssetsState.Content(
                            assets = assets.map { it.toListItemUi() },
                            eventSink = eventSink,
                        )
                    }
                }
                .catch {
                    emit(
                        AssetsState.Error(
                            message = "Assets could not be loaded.",
                            eventSink = eventSink,
                        ),
                    )
                }
        }.collectAsState(initial = AssetsState.Loading)
        return state
    }
}

internal class AssetDetailPresenter(
    private val screen: AssetDetailScreen,
    private val getAsset: GetAssetUseCase,
    private val observeLocations: ObserveLocationsUseCase,
    private val observeTemplates: ObserveTemplatesUseCase,
    private val startInspection: StartInspectionUseCase,
    private val navigator: Navigator,
) : Presenter<AssetDetailState> {
    @Composable
    override fun present(): AssetDetailState {
        val assetId = remember(screen.assetId) { AssetId(screen.assetId) }
        var asset by remember(assetId) { mutableStateOf<Asset?>(null) }
        var isLoaded by remember(assetId) { mutableStateOf(false) }
        var isStartVisible by remember(assetId) { mutableStateOf(false) }
        var selectedTemplateId by remember(assetId) { mutableStateOf<TemplateId?>(null) }
        var isStarting by remember(assetId) { mutableStateOf(false) }
        var startError by remember(assetId) { mutableStateOf<String?>(null) }
        val coroutineScope = rememberCoroutineScope()

        val locations by remember(observeLocations) {
            observeLocations().catch { emit(emptyList()) }
        }.collectAsState(initial = emptyList())
        val templates by remember(observeTemplates) {
            observeTemplates()
                .map { values -> values.map { it.toOptionUi() } }
                .catch { emit(emptyList()) }
        }.collectAsState(initial = emptyList())

        LaunchedEffect(assetId) {
            isLoaded = false
            asset = getAsset(assetId)
            isLoaded = true
        }

        fun selectedTemplate(): TemplateOptionUi? =
            templates.firstOrNull { it.id == selectedTemplateId }
                ?: templates.firstOrNull()

        val eventSink = remember(
            assetId,
            navigator,
            coroutineScope,
            startInspection,
            asset,
            templates,
        ) {
            { event: AssetDetailEvent ->
                when (event) {
                    AssetDetailEvent.BackSelected -> navigator.pop()
                    AssetDetailEvent.EditSelected -> navigator.goTo(AssetEditorScreen(assetId.value))
                    AssetDetailEvent.StartInspectionSelected -> {
                        selectedTemplateId = selectedTemplate()?.id
                        startError = null
                        isStartVisible = true
                    }
                    AssetDetailEvent.StartInspectionDismissed -> {
                        if (!isStarting) {
                            isStartVisible = false
                            startError = null
                        }
                    }
                    is AssetDetailEvent.TemplateSelected -> {
                        selectedTemplateId = event.templateId
                        startError = null
                    }
                    AssetDetailEvent.StartInspectionConfirmed -> {
                        val currentAsset = asset
                        val template = selectedTemplate()
                        if (currentAsset == null || template == null) {
                            startError = "Choose a template."
                        } else {
                            coroutineScope.launch {
                                try {
                                    isStarting = true
                                    val inspectionId = startInspection(
                                        assetId = currentAsset.id,
                                        assetName = currentAsset.name,
                                        templateId = template.id,
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

        val current = asset
        return when {
            !isLoaded -> AssetDetailState.Loading
            current == null -> AssetDetailState.Missing(eventSink)
            else -> AssetDetailState.Content(
                asset = current.toDetailUi(locations),
                startInspection = AssetStartInspectionUi(
                    isVisible = isStartVisible,
                    isStarting = isStarting,
                    templates = templates,
                    selectedTemplateId = selectedTemplate()?.id,
                    errorMessage = startError,
                ),
                eventSink = eventSink,
            )
        }
    }
}

internal class AssetEditorPresenter(
    private val screen: AssetEditorScreen,
    private val getAsset: GetAssetUseCase,
    private val observeLocations: ObserveLocationsUseCase,
    private val createAsset: CreateAssetUseCase,
    private val updateAsset: UpdateAssetUseCase,
    private val navigator: Navigator,
) : Presenter<AssetEditorState> {
    @Composable
    override fun present(): AssetEditorState {
        val assetId = remember(screen.assetId) { screen.assetId?.let(::AssetId) }
        var isLoaded by remember(assetId) { mutableStateOf(assetId == null) }
        var isMissing by remember(assetId) { mutableStateOf(false) }
        var form by remember(assetId) {
            mutableStateOf(AssetFormUi(name = "", code = "", selectedLocationId = null))
        }
        var isSaving by remember(assetId) { mutableStateOf(false) }
        var validationMessages by remember(assetId) { mutableStateOf(emptyList<String>()) }
        var saveError by remember(assetId) { mutableStateOf<String?>(null) }
        val coroutineScope = rememberCoroutineScope()

        val locations by remember(observeLocations) {
            observeLocations()
                .map { values -> values.map { LocationOptionUi(it.id, it.name) } }
                .catch { emit(emptyList()) }
        }.collectAsState(initial = emptyList())

        LaunchedEffect(assetId) {
            if (assetId == null) {
                isLoaded = true
                isMissing = false
            } else {
                isLoaded = false
                val asset = getAsset(assetId)
                if (asset == null) {
                    isMissing = true
                } else {
                    form = AssetFormUi(
                        name = asset.name,
                        code = asset.code.orEmpty(),
                        selectedLocationId = asset.locationId,
                    )
                    isMissing = false
                }
                isLoaded = true
            }
        }

        LaunchedEffect(locations, assetId, form.selectedLocationId) {
            if (assetId == null && form.selectedLocationId == null && locations.isNotEmpty()) {
                form = form.copy(selectedLocationId = locations.first().id)
            }
        }

        val eventSink = remember(
            navigator,
            coroutineScope,
            assetId,
            form,
            createAsset,
            updateAsset,
        ) {
            { event: AssetEditorEvent ->
                when (event) {
                    AssetEditorEvent.BackSelected -> navigator.pop()
                    is AssetEditorEvent.CodeChanged -> {
                        form = form.copy(code = event.value)
                        validationMessages = emptyList()
                        saveError = null
                    }
                    is AssetEditorEvent.LocationSelected -> {
                        form = form.copy(selectedLocationId = event.locationId)
                        validationMessages = emptyList()
                        saveError = null
                    }
                    is AssetEditorEvent.NameChanged -> {
                        form = form.copy(name = event.value)
                        validationMessages = emptyList()
                        saveError = null
                    }
                    AssetEditorEvent.SaveSelected -> {
                        coroutineScope.launch {
                            try {
                                isSaving = true
                                validationMessages = emptyList()
                                saveError = null
                                val input = AssetInput(
                                    name = form.name,
                                    code = form.code,
                                    locationId = form.selectedLocationId,
                                )
                                val result = if (assetId == null) {
                                    createAsset(input)
                                } else {
                                    updateAsset(assetId, input)
                                }
                                isSaving = false
                                when (result) {
                                    is AssetSaveResult.Success -> navigator.pop()
                                    is AssetSaveResult.ValidationFailed -> {
                                        validationMessages = result.errors.map { it.message }
                                    }
                                    AssetSaveResult.NotFound -> {
                                        saveError = "Asset could not be found."
                                    }
                                }
                            } catch (exception: Exception) {
                                if (exception is CancellationException) throw exception
                                isSaving = false
                                saveError = "Asset could not be saved."
                            }
                        }
                    }
                }
                Unit
            }
        }

        return when {
            !isLoaded -> AssetEditorState.Loading
            isMissing -> AssetEditorState.Missing(eventSink)
            else -> AssetEditorState.Editing(
                title = if (assetId == null) "Add asset" else "Edit asset",
                form = form,
                locations = locations,
                isSaving = isSaving,
                validationMessages = validationMessages,
                saveErrorMessage = saveError,
                eventSink = eventSink,
            )
        }
    }
}

private fun AssetSummary.toListItemUi() = AssetListItemUi(
    id = id,
    name = name,
    code = code,
    locationName = locationName,
    nextInspectionDueLabel = nextInspectionDueAtMillis?.formatDateLabel(),
)

private fun Asset.toDetailUi(locations: List<Location>) = AssetDetailUi(
    id = id,
    name = name,
    code = code,
    locationName = locations.firstOrNull { it.id == locationId }?.name ?: "No location",
    nextInspectionDueLabel = nextInspectionDueAtMillis?.formatDateLabel(),
)

private fun InspectionTemplateSummary.toOptionUi() = TemplateOptionUi(
    id = id,
    name = name,
    detail = "v$version • $sectionCount sections",
)

private fun Long.formatDateLabel(): String {
    return "Next due: ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(this))}"
}
