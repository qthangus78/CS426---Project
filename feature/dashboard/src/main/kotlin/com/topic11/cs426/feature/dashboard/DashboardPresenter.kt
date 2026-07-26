package com.topic11.cs426.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.topic11.cs426.core.designsystem.StatusTone
import com.topic11.cs426.core.navigation.AssetsScreen
import com.topic11.cs426.core.navigation.InspectionScreen
import com.topic11.cs426.core.navigation.IssuesScreen
import com.topic11.cs426.core.navigation.ReportsScreen
import com.topic11.cs426.core.navigation.TemplatesScreen
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.AssetSummary
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.InspectionSummary
import com.topic11.cs426.domain.model.InspectionTemplateSummary
import com.topic11.cs426.domain.model.TemplateId
import com.topic11.cs426.domain.usecase.ObserveAssetsUseCase
import com.topic11.cs426.domain.usecase.ObserveInspectionSummariesUseCase
import com.topic11.cs426.domain.usecase.ObserveTemplatesUseCase
import com.topic11.cs426.domain.usecase.StartInspectionUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal class DashboardPresenter(
    private val observeInspectionSummaries: ObserveInspectionSummariesUseCase,
    private val observeAssets: ObserveAssetsUseCase,
    private val observeTemplates: ObserveTemplatesUseCase,
    private val startInspection: StartInspectionUseCase,
    private val navigator: Navigator,
) : Presenter<DashboardState> {
    @Composable
    override fun present(): DashboardState {
        val presenterModels = remember(
            observeInspectionSummaries,
            observeAssets,
            observeTemplates,
        ) {
            combine(
                observeInspectionSummaries(),
                observeAssets(),
                observeTemplates(),
            ) { summaries, assets, templates ->
                    val inspections = summaries.map { inspection -> inspection.toUiModel() }
                    DashboardPresenterModel(
                        isLoaded = true,
                        overview = summaries.toOverviewUi(),
                        inspections = inspections,
                        heroInspection = inspections.selectHeroInspection(),
                        assets = assets.map { it.toStartInspectionAssetUi() },
                        templates = templates.map { it.toStartInspectionTemplateUi() },
                    )
                }
        }
        val presenterModel by presenterModels.collectAsState(initial = DashboardPresenterModel())
        var selectedFilter by remember { mutableStateOf(InspectionFilterUi.ALL) }
        var isAboutVisible by remember { mutableStateOf(false) }
        var isStartInspectionVisible by remember { mutableStateOf(false) }
        var selectedAssetId by remember { mutableStateOf<AssetId?>(null) }
        var selectedTemplateId by remember { mutableStateOf<TemplateId?>(null) }
        var isCreatingInspection by remember { mutableStateOf(false) }
        var startInspectionError by remember { mutableStateOf<String?>(null) }
        val coroutineScope = rememberCoroutineScope()

        fun selectedAsset(): StartInspectionAssetUi? =
            presenterModel.assets.firstOrNull { it.id == selectedAssetId }
                ?: presenterModel.assets.firstOrNull()

        fun selectedTemplate(): StartInspectionTemplateUi? =
            presenterModel.templates.firstOrNull { it.id == selectedTemplateId }
                ?: presenterModel.templates.firstOrNull()

        val startInspectionUi = StartInspectionUi(
            isVisible = isStartInspectionVisible,
            isCreating = isCreatingInspection,
            assets = presenterModel.assets,
            templates = presenterModel.templates,
            selectedAssetId = selectedAsset()?.id,
            selectedTemplateId = selectedTemplate()?.id,
            errorMessage = startInspectionError,
        )

        val eventSink = remember(
            navigator,
            presenterModel,
            coroutineScope,
            startInspection,
        ) {
            { event: DashboardEvent ->
                when (event) {
                    DashboardEvent.AboutDismissed -> {
                        isAboutVisible = false
                    }
                    DashboardEvent.AboutSelected -> {
                        isAboutVisible = true
                    }
                    is DashboardEvent.FilterSelected -> {
                        selectedFilter = event.filter
                    }
                    is DashboardEvent.InspectionSelected -> {
                        navigator.goTo(InspectionScreen(event.inspectionId.value))
                    }
                    DashboardEvent.StartInspectionSelected -> {
                        selectedAssetId = selectedAsset()?.id
                        selectedTemplateId = selectedTemplate()?.id
                        startInspectionError = null
                        isStartInspectionVisible = true
                    }
                    DashboardEvent.StartInspectionDismissed -> {
                        if (!isCreatingInspection) {
                            isStartInspectionVisible = false
                            startInspectionError = null
                        }
                    }
                    is DashboardEvent.StartInspectionAssetSelected -> {
                        selectedAssetId = event.assetId
                        startInspectionError = null
                    }
                    is DashboardEvent.StartInspectionTemplateSelected -> {
                        selectedTemplateId = event.templateId
                        startInspectionError = null
                    }
                    DashboardEvent.StartInspectionConfirmed -> {
                        val asset = selectedAsset()
                        val template = selectedTemplate()
                        if (asset == null || template == null) {
                            startInspectionError = "Choose an asset and template."
                        } else {
                            coroutineScope.launch {
                                try {
                                    isCreatingInspection = true
                                    val inspectionId = startInspection(
                                        assetId = asset.id,
                                        assetName = asset.name,
                                        templateId = template.id,
                                    )
                                    isCreatingInspection = false
                                    isStartInspectionVisible = false
                                    startInspectionError = null
                                    navigator.goTo(InspectionScreen(inspectionId.value))
                                } catch (exception: Exception) {
                                    if (exception is CancellationException) throw exception
                                    isCreatingInspection = false
                                    startInspectionError = "Couldn't start inspection."
                                }
                            }
                        }
                    }
                    DashboardEvent.AssetsSelected -> navigator.goTo(AssetsScreen)
                    DashboardEvent.TemplatesSelected -> navigator.goTo(TemplatesScreen)
                    DashboardEvent.IssuesSelected -> navigator.goTo(IssuesScreen)
                    DashboardEvent.ReportsSelected -> navigator.goTo(ReportsScreen)
                }
                Unit
            }
        }

        return when {
            !presenterModel.isLoaded -> DashboardState.Loading
            presenterModel.inspections.isEmpty() -> DashboardState.Empty(
                overview = presenterModel.overview,
                selectedFilter = selectedFilter,
                isAboutVisible = isAboutVisible,
                startInspection = startInspectionUi,
                eventSink = eventSink,
            )
            else -> DashboardState.Content(
                overview = presenterModel.overview,
                heroInspection = presenterModel.heroInspection,
                selectedFilter = selectedFilter,
                filteredInspections = presenterModel.inspections.filterBy(selectedFilter),
                isAboutVisible = isAboutVisible,
                startInspection = startInspectionUi,
                eventSink = eventSink,
            )
        }
    }
}

private data class DashboardPresenterModel(
    val isLoaded: Boolean = false,
    val overview: DashboardOverviewUi = DashboardOverviewUi(
        totalInspections = 0,
        inProgressInspections = 0,
        syncPendingInspections = 0,
    ),
    val heroInspection: InspectionSummaryUi? = null,
    val inspections: List<InspectionSummaryUi> = emptyList(),
    val assets: List<StartInspectionAssetUi> = emptyList(),
    val templates: List<StartInspectionTemplateUi> = emptyList(),
)

private fun List<InspectionSummaryUi>.selectHeroInspection(): InspectionSummaryUi? {
    return firstOrNull { inspection ->
        inspection.filter == InspectionFilterUi.IN_PROGRESS
    } ?: firstOrNull { inspection ->
        inspection.filter == InspectionFilterUi.SYNC_PENDING
    }
}

private fun List<InspectionSummaryUi>.filterBy(filter: InspectionFilterUi): List<InspectionSummaryUi> {
    return when (filter) {
        InspectionFilterUi.ALL -> this
        InspectionFilterUi.IN_PROGRESS,
        InspectionFilterUi.NOT_STARTED,
        InspectionFilterUi.SYNC_PENDING -> filter { inspection -> inspection.filter == filter }
    }
}

private fun List<InspectionSummary>.toOverviewUi(): DashboardOverviewUi {
    return DashboardOverviewUi(
        totalInspections = size,
        inProgressInspections = count { inspection ->
            inspection.status == InspectionStatus.IN_PROGRESS
        },
        syncPendingInspections = count { inspection ->
            inspection.status == InspectionStatus.SYNC_PENDING
        },
    )
}

private fun InspectionSummary.toUiModel(): InspectionSummaryUi {
    return InspectionSummaryUi(
        id = id,
        title = title,
        statusLabel = status.displayLabel(),
        statusTone = status.statusTone(),
        completedItems = completedItems,
        totalItems = totalItems,
        progressFraction = progressFraction.coerceIn(0f, 1f),
        filter = status.filter(),
    )
}

private fun AssetSummary.toStartInspectionAssetUi(): StartInspectionAssetUi {
    val subtitle = listOfNotNull(
        code,
        locationName?.takeIf { it.isNotBlank() },
    ).joinToString(separator = " • ").takeIf { it.isNotBlank() }
    return StartInspectionAssetUi(
        id = id,
        name = name,
        subtitle = subtitle,
    )
}

private fun InspectionTemplateSummary.toStartInspectionTemplateUi(): StartInspectionTemplateUi =
    StartInspectionTemplateUi(
        id = id,
        name = name,
        versionLabel = "v$version • $sectionCount sections",
    )

private fun InspectionStatus.displayLabel(): String {
    return when (this) {
        InspectionStatus.NOT_STARTED -> "Not started"
        InspectionStatus.IN_PROGRESS -> "In progress"
        InspectionStatus.REVIEWING -> "Reviewing"
        InspectionStatus.COMPLETED -> "Completed"
        InspectionStatus.SYNC_PENDING -> "Sync pending"
    }
}

private fun InspectionStatus.statusTone(): StatusTone {
    return when (this) {
        InspectionStatus.NOT_STARTED -> StatusTone.Neutral
        InspectionStatus.IN_PROGRESS -> StatusTone.InProgress
        InspectionStatus.REVIEWING -> StatusTone.InProgress
        InspectionStatus.COMPLETED -> StatusTone.Success
        InspectionStatus.SYNC_PENDING -> StatusTone.Warning
    }
}

private fun InspectionStatus.filter(): InspectionFilterUi? {
    return when (this) {
        InspectionStatus.NOT_STARTED -> InspectionFilterUi.NOT_STARTED
        InspectionStatus.IN_PROGRESS -> InspectionFilterUi.IN_PROGRESS
        InspectionStatus.REVIEWING -> InspectionFilterUi.IN_PROGRESS
        InspectionStatus.SYNC_PENDING -> InspectionFilterUi.SYNC_PENDING
        InspectionStatus.COMPLETED -> null
    }
}
