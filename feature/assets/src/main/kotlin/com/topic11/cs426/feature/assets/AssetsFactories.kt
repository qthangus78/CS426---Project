package com.topic11.cs426.feature.assets

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.topic11.cs426.core.navigation.AssetDetailScreen
import com.topic11.cs426.core.navigation.AssetEditorScreen
import com.topic11.cs426.core.navigation.AssetsScreen
import com.topic11.cs426.domain.usecase.CreateAssetUseCase
import com.topic11.cs426.domain.usecase.GetAssetUseCase
import com.topic11.cs426.domain.usecase.ObserveAssetsUseCase
import com.topic11.cs426.domain.usecase.ObserveLocationsUseCase
import com.topic11.cs426.domain.usecase.ObserveTemplatesUseCase
import com.topic11.cs426.domain.usecase.StartInspectionUseCase
import com.topic11.cs426.domain.usecase.UpdateAssetUseCase

class AssetsPresenterFactory(
    private val observeAssets: ObserveAssetsUseCase,
    private val observeLocations: ObserveLocationsUseCase,
    private val getAsset: GetAssetUseCase,
    private val createAsset: CreateAssetUseCase,
    private val updateAsset: UpdateAssetUseCase,
    private val observeTemplates: ObserveTemplatesUseCase,
    private val startInspection: StartInspectionUseCase,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? {
        return when (screen) {
            AssetsScreen -> AssetsPresenter(
                observeAssets = observeAssets,
                navigator = navigator,
            )
            is AssetDetailScreen -> AssetDetailPresenter(
                screen = screen,
                getAsset = getAsset,
                observeLocations = observeLocations,
                observeTemplates = observeTemplates,
                startInspection = startInspection,
                navigator = navigator,
            )
            is AssetEditorScreen -> AssetEditorPresenter(
                screen = screen,
                getAsset = getAsset,
                observeLocations = observeLocations,
                createAsset = createAsset,
                updateAsset = updateAsset,
                navigator = navigator,
            )
            else -> null
        }
    }
}

class AssetsUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? {
        return when (screen) {
            AssetsScreen -> ui<AssetsState> { state, modifier ->
                AssetsUi(state = state, modifier = modifier)
            }
            is AssetDetailScreen -> ui<AssetDetailState> { state, modifier ->
                AssetDetailUi(state = state, modifier = modifier)
            }
            is AssetEditorScreen -> ui<AssetEditorState> { state, modifier ->
                AssetEditorUi(state = state, modifier = modifier)
            }
            else -> null
        }
    }
}
