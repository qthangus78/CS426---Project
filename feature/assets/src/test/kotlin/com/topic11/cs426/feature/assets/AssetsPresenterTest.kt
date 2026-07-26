package com.topic11.cs426.feature.assets

import app.cash.turbine.ReceiveTurbine
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import com.topic11.cs426.core.navigation.AssetDetailScreen
import com.topic11.cs426.core.navigation.AssetEditorScreen
import com.topic11.cs426.core.navigation.AssetsScreen
import com.topic11.cs426.core.navigation.DashboardScreen
import com.topic11.cs426.core.navigation.InspectionScreen
import com.topic11.cs426.core.testing.FakeAssetRepository
import com.topic11.cs426.core.testing.FakeTemplateRepository
import com.topic11.cs426.core.testing.InspectionTestFixtures
import com.topic11.cs426.core.testing.RecordingInspectionRepository
import com.topic11.cs426.domain.model.Asset
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.LocationId
import com.topic11.cs426.domain.usecase.CreateAssetUseCase
import com.topic11.cs426.domain.usecase.GetAssetUseCase
import com.topic11.cs426.domain.usecase.ObserveAssetsUseCase
import com.topic11.cs426.domain.usecase.ObserveLocationsUseCase
import com.topic11.cs426.domain.usecase.ObserveTemplatesUseCase
import com.topic11.cs426.domain.usecase.StartInspectionUseCase
import com.topic11.cs426.domain.usecase.UpdateAssetUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssetsPresenterTest {
    @Test
    fun `list presents assets and opens detail`() = runTest {
        val navigator = FakeNavigator(DashboardScreen, AssetsScreen)
        val repository = FakeAssetRepository()
        val presenter = AssetsPresenter(
            observeAssets = ObserveAssetsUseCase(repository),
            navigator = navigator,
        )

        presenter.test {
            assertEquals(AssetsState.Loading, awaitItem())
            val content = awaitItem() as AssetsState.Content

            assertEquals(listOf(InspectionTestFixtures.asset1Id), content.assets.map { it.id })
            content.eventSink(AssetsEvent.SearchQueryChanged("lab"))
            val filtered = awaitItem() as AssetsState.Content
            assertEquals("lab", filtered.query)
            assertEquals(listOf(InspectionTestFixtures.asset1Id), filtered.assets.map { it.id })
            filtered.eventSink(AssetsEvent.AssetSelected(InspectionTestFixtures.asset1Id))

            assertEquals(
                AssetDetailScreen(InspectionTestFixtures.asset1Id.value),
                navigator.awaitNextScreen(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `list presents empty state`() = runTest {
        val presenter = AssetsPresenter(
            observeAssets = ObserveAssetsUseCase(FakeAssetRepository(initialAssets = emptyList())),
            navigator = FakeNavigator(DashboardScreen, AssetsScreen),
        )

        presenter.test {
            assertEquals(AssetsState.Loading, awaitItem())
            assertTrue(awaitItem() is AssetsState.Empty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `editor creates asset and returns to previous screen`() = runTest {
        val screen = AssetEditorScreen(assetId = null)
        val navigator = FakeNavigator(DashboardScreen, AssetsScreen, screen)
        val repository = FakeAssetRepository(initialAssets = emptyList())
        val presenter = AssetEditorPresenter(
            screen = screen,
            getAsset = GetAssetUseCase(repository),
            observeLocations = ObserveLocationsUseCase(repository),
            createAsset = CreateAssetUseCase(
                assetRepository = repository,
                idFactory = { AssetId("asset-created") },
            ),
            updateAsset = UpdateAssetUseCase(repository),
            navigator = navigator,
        )

        presenter.test {
            val editing = awaitReadyEditor()

            editing.eventSink(AssetEditorEvent.NameChanged("Room 202"))
            var current = awaitItem() as AssetEditorState.Editing
            current.eventSink(AssetEditorEvent.CodeChanged("ROOM-202"))
            current = awaitItem() as AssetEditorState.Editing
            current.eventSink(AssetEditorEvent.SaveSelected)
            advanceUntilIdle()

            assertEquals(screen, navigator.awaitPop().poppedScreen)
            assertEquals("Room 202", repository.getAsset(AssetId("asset-created"))?.name)
            assertEquals("ROOM-202", repository.getAsset(AssetId("asset-created"))?.code)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `editor shows validation errors without navigating`() = runTest {
        val screen = AssetEditorScreen(assetId = null)
        val navigator = FakeNavigator(DashboardScreen, AssetsScreen, screen)
        val repository = FakeAssetRepository(initialAssets = emptyList())
        val presenter = AssetEditorPresenter(
            screen = screen,
            getAsset = GetAssetUseCase(repository),
            observeLocations = ObserveLocationsUseCase(repository),
            createAsset = CreateAssetUseCase(
                assetRepository = repository,
                idFactory = { AssetId("asset-created") },
            ),
            updateAsset = UpdateAssetUseCase(repository),
            navigator = navigator,
        )

        presenter.test {
            val editing = awaitReadyEditor()

            editing.eventSink(AssetEditorEvent.SaveSelected)
            advanceUntilIdle()
            val validation = awaitEditorWithValidation()

            assertEquals(listOf("Asset name is required."), validation.validationMessages)
            assertTrue(repository.savedAssets.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `detail missing state supports back navigation`() = runTest {
        val screen = AssetDetailScreen("missing-asset")
        val navigator = FakeNavigator(DashboardScreen, AssetsScreen, screen)
        val repository = FakeAssetRepository(initialAssets = emptyList())
        val presenter = AssetDetailPresenter(
            screen = screen,
            getAsset = GetAssetUseCase(repository),
            observeLocations = ObserveLocationsUseCase(repository),
            observeTemplates = ObserveTemplatesUseCase(FakeTemplateRepository()),
            startInspection = StartInspectionUseCase(
                inspectionRepository = RecordingInspectionRepository(),
                templateRepository = FakeTemplateRepository(),
            ),
            navigator = navigator,
        )

        presenter.test {
            var state = awaitItem()
            while (state !is AssetDetailState.Missing) {
                state = awaitItem()
            }

            state.eventSink(AssetDetailEvent.BackSelected)

            assertEquals(screen, navigator.awaitPop().poppedScreen)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `asset detail starts inspection with selected template`() = runTest {
        val asset = Asset(
            id = AssetId("asset-room-202"),
            name = "Room 202",
            code = "ROOM-202",
            locationId = LocationId("location-lab"),
        )
        val assetRepository = FakeAssetRepository(initialAssets = listOf(asset))
        val templateRepository = FakeTemplateRepository(
            mapOf(InspectionTestFixtures.templateId to InspectionTestFixtures.sampleTemplate),
        )
        val inspectionRepository = RecordingInspectionRepository()
        val screen = AssetDetailScreen(asset.id.value)
        val navigator = FakeNavigator(DashboardScreen, AssetsScreen, screen)
        val presenter = AssetDetailPresenter(
            screen = screen,
            getAsset = GetAssetUseCase(assetRepository),
            observeLocations = ObserveLocationsUseCase(assetRepository),
            observeTemplates = ObserveTemplatesUseCase(templateRepository),
            startInspection = StartInspectionUseCase(inspectionRepository, templateRepository),
            navigator = navigator,
        )

        presenter.test {
            awaitItem()
            val content = awaitDetailContent()

            content.eventSink(AssetDetailEvent.StartInspectionSelected)
            val selecting = awaitDetailContent()
            assertTrue(selecting.startInspection.isVisible)
            selecting.eventSink(AssetDetailEvent.StartInspectionConfirmed)
            advanceUntilIdle()

            assertEquals(InspectionScreen("inspection-1"), navigator.awaitNextScreen())
            assertEquals(1, inspectionRepository.createInspectionCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun ReceiveTurbine<AssetEditorState>.awaitReadyEditor(): AssetEditorState.Editing {
        var state = awaitItem()
        while (state !is AssetEditorState.Editing || state.form.selectedLocationId == null) {
            state = awaitItem()
        }
        return state
    }

    private suspend fun ReceiveTurbine<AssetEditorState>.awaitEditorWithValidation(): AssetEditorState.Editing {
        var state = awaitItem()
        while (state !is AssetEditorState.Editing || state.validationMessages.isEmpty()) {
            state = awaitItem()
        }
        return state
    }

    private suspend fun ReceiveTurbine<AssetDetailState>.awaitDetailContent(): AssetDetailState.Content {
        var state = awaitItem()
        while (state !is AssetDetailState.Content) {
            state = awaitItem()
        }
        return state
    }
}
