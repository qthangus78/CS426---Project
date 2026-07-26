package com.topic11.cs426.feature.locations

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import com.topic11.cs426.core.navigation.DashboardScreen
import com.topic11.cs426.core.navigation.LocationDetailScreen
import com.topic11.cs426.core.navigation.LocationEditorScreen
import com.topic11.cs426.core.navigation.LocationsScreen
import com.topic11.cs426.core.testing.FakeAssetRepository
import com.topic11.cs426.domain.model.Location
import com.topic11.cs426.domain.model.LocationId
import com.topic11.cs426.domain.usecase.CreateLocationUseCase
import com.topic11.cs426.domain.usecase.GetLocationUseCase
import com.topic11.cs426.domain.usecase.ObserveLocationsUseCase
import com.topic11.cs426.domain.usecase.UpdateLocationUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocationsPresenterTest {
    @Test
    fun `list presents locations filters search and opens detail`() = runTest {
        val repository = FakeAssetRepository(
            initialAssets = emptyList(),
            initialLocations = listOf(
                Location(LocationId("location-campus"), "Campus"),
                Location(LocationId("location-room"), "Room 202", LocationId("location-campus")),
            ),
        )
        val navigator = FakeNavigator(DashboardScreen, LocationsScreen)
        val presenter = LocationsPresenter(
            observeLocations = ObserveLocationsUseCase(repository),
            navigator = navigator,
        )

        presenter.test {
            assertEquals(LocationsState.Loading, awaitItem())
            val content = awaitItem() as LocationsState.Content
            assertEquals(2, content.locations.size)

            content.eventSink(LocationsEvent.SearchQueryChanged("room"))
            val filtered = awaitItem() as LocationsState.Content
            assertEquals(listOf(LocationId("location-room")), filtered.locations.map { it.id })
            filtered.eventSink(LocationsEvent.LocationSelected(LocationId("location-room")))

            assertEquals(LocationDetailScreen("location-room"), navigator.awaitNextScreen())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `editor creates location and returns back`() = runTest {
        val repository = FakeAssetRepository(initialAssets = emptyList())
        val screen = LocationEditorScreen(locationId = null)
        val navigator = FakeNavigator(DashboardScreen, LocationsScreen, screen)
        val presenter = LocationEditorPresenter(
            screen = screen,
            getLocation = GetLocationUseCase(repository),
            observeLocations = ObserveLocationsUseCase(repository),
            createLocation = CreateLocationUseCase(
                assetRepository = repository,
                idFactory = { LocationId("location-created") },
            ),
            updateLocation = UpdateLocationUseCase(repository),
            navigator = navigator,
        )

        presenter.test {
            val editing = awaitItem() as LocationEditorState.Editing
            editing.eventSink(LocationEditorEvent.NameChanged("Building A"))
            val named = awaitItem() as LocationEditorState.Editing

            named.eventSink(LocationEditorEvent.SaveSelected)
            advanceUntilIdle()

            assertEquals(screen, navigator.awaitPop().poppedScreen)
            assertEquals("Building A", repository.getLocation(LocationId("location-created"))?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `editor shows validation failure without saving`() = runTest {
        val repository = FakeAssetRepository(initialAssets = emptyList())
        val screen = LocationEditorScreen(locationId = null)
        val presenter = LocationEditorPresenter(
            screen = screen,
            getLocation = GetLocationUseCase(repository),
            observeLocations = ObserveLocationsUseCase(repository),
            createLocation = CreateLocationUseCase(
                assetRepository = repository,
                idFactory = { LocationId("location-created") },
            ),
            updateLocation = UpdateLocationUseCase(repository),
            navigator = FakeNavigator(DashboardScreen, LocationsScreen, screen),
        )

        presenter.test {
            val editing = awaitItem() as LocationEditorState.Editing
            editing.eventSink(LocationEditorEvent.SaveSelected)
            advanceUntilIdle()

            var state = awaitItem()
            while (state !is LocationEditorState.Editing || state.validationMessages.isEmpty()) {
                state = awaitItem()
            }
            assertEquals(listOf("Location name is required."), state.validationMessages)
            assertTrue(repository.getLocation(LocationId("location-created")) == null)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
