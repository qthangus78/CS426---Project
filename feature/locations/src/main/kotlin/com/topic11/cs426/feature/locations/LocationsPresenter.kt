package com.topic11.cs426.feature.locations

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
import com.topic11.cs426.core.navigation.LocationDetailScreen
import com.topic11.cs426.core.navigation.LocationEditorScreen
import com.topic11.cs426.domain.model.Location
import com.topic11.cs426.domain.model.LocationId
import com.topic11.cs426.domain.usecase.CreateLocationUseCase
import com.topic11.cs426.domain.usecase.GetLocationUseCase
import com.topic11.cs426.domain.usecase.LocationInput
import com.topic11.cs426.domain.usecase.LocationSaveResult
import com.topic11.cs426.domain.usecase.ObserveLocationsUseCase
import com.topic11.cs426.domain.usecase.UpdateLocationUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class LocationsPresenter(
    private val observeLocations: ObserveLocationsUseCase,
    private val navigator: Navigator,
) : Presenter<LocationsState> {
    @Composable
    override fun present(): LocationsState {
        var query by remember { mutableStateOf("") }
        val eventSink = remember(navigator) {
            { event: LocationsEvent ->
                when (event) {
                    LocationsEvent.AddSelected -> navigator.goTo(LocationEditorScreen(locationId = null))
                    LocationsEvent.BackSelected -> navigator.pop()
                    is LocationsEvent.LocationSelected -> {
                        navigator.goTo(LocationDetailScreen(event.locationId.value))
                    }
                    is LocationsEvent.SearchQueryChanged -> query = event.query
                    LocationsEvent.SearchCleared -> query = ""
                }
                Unit
            }
        }
        val observedLocations by remember(observeLocations) {
            observeLocations()
                .map<List<Location>, ObservedLocations> { ObservedLocations.Loaded(it) }
                .catch { emit(ObservedLocations.Failed) }
        }.collectAsState(initial = ObservedLocations.Loading)

        return when (val observed = observedLocations) {
            ObservedLocations.Loading -> LocationsState.Loading
            ObservedLocations.Failed -> LocationsState.Error(
                message = "Locations could not be loaded.",
                eventSink = eventSink,
            )
            is ObservedLocations.Loaded -> {
                val locations = observed.locations.toListItems()
                val filtered = locations.filterBy(query)
                when {
                    locations.isEmpty() -> LocationsState.Empty(query = query, eventSink = eventSink)
                    else -> LocationsState.Content(
                        locations = filtered,
                        query = query,
                        hasNoSearchResults = query.isNotBlank() && filtered.isEmpty(),
                        eventSink = eventSink,
                    )
                }
            }
        }
    }
}

internal class LocationDetailPresenter(
    private val screen: LocationDetailScreen,
    private val observeLocations: ObserveLocationsUseCase,
    private val navigator: Navigator,
) : Presenter<LocationDetailState> {
    @Composable
    override fun present(): LocationDetailState {
        val locationId = remember(screen.locationId) { LocationId(screen.locationId) }
        val eventSink = remember(locationId, navigator) {
            { event: LocationDetailEvent ->
                when (event) {
                    LocationDetailEvent.BackSelected -> navigator.pop()
                    LocationDetailEvent.EditSelected -> navigator.goTo(LocationEditorScreen(locationId.value))
                }
                Unit
            }
        }
        val observedLocations by remember(observeLocations, locationId) {
            observeLocations()
                .map<List<Location>, ObservedLocations> { ObservedLocations.Loaded(it) }
                .catch { emit(ObservedLocations.Loaded(emptyList())) }
        }.collectAsState(initial = ObservedLocations.Loading)

        return when (val observed = observedLocations) {
            ObservedLocations.Loading -> LocationDetailState.Loading
            ObservedLocations.Failed -> LocationDetailState.Missing(eventSink)
            is ObservedLocations.Loaded -> {
                val location = observed.locations.firstOrNull { it.id == locationId }
                    ?: return LocationDetailState.Missing(eventSink)
                LocationDetailState.Content(
                    location = location.toDetailUi(observed.locations),
                    eventSink = eventSink,
                )
            }
        }
    }
}

internal class LocationEditorPresenter(
    private val screen: LocationEditorScreen,
    private val getLocation: GetLocationUseCase,
    private val observeLocations: ObserveLocationsUseCase,
    private val createLocation: CreateLocationUseCase,
    private val updateLocation: UpdateLocationUseCase,
    private val navigator: Navigator,
) : Presenter<LocationEditorState> {
    @Composable
    override fun present(): LocationEditorState {
        val locationId = remember(screen.locationId) { screen.locationId?.let(::LocationId) }
        var isLoaded by remember(locationId) { mutableStateOf(locationId == null) }
        var isMissing by remember(locationId) { mutableStateOf(false) }
        var form by remember(locationId) {
            mutableStateOf(LocationFormUi(name = "", parentId = null))
        }
        var isSaving by remember(locationId) { mutableStateOf(false) }
        var validationMessages by remember(locationId) { mutableStateOf(emptyList<String>()) }
        var saveError by remember(locationId) { mutableStateOf<String?>(null) }
        val coroutineScope = rememberCoroutineScope()

        val locations by remember(observeLocations) {
            observeLocations().catch { emit(emptyList()) }
        }.collectAsState(initial = emptyList())

        LaunchedEffect(locationId) {
            if (locationId == null) {
                isLoaded = true
                isMissing = false
            } else {
                isLoaded = false
                val location = getLocation(locationId)
                if (location == null) {
                    isMissing = true
                } else {
                    form = LocationFormUi(
                        name = location.name,
                        parentId = location.parentId,
                    )
                    isMissing = false
                }
                isLoaded = true
            }
        }

        val parentOptions = remember(locations, locationId) {
            locations
                .filter { location -> location.id != locationId }
                .sortedBy { it.name }
                .map { LocationParentOptionUi(it.id, it.name) }
        }

        val eventSink = remember(
            navigator,
            coroutineScope,
            locationId,
            form,
            createLocation,
            updateLocation,
        ) {
            { event: LocationEditorEvent ->
                when (event) {
                    LocationEditorEvent.BackSelected -> navigator.pop()
                    is LocationEditorEvent.NameChanged -> {
                        form = form.copy(name = event.value)
                        validationMessages = emptyList()
                        saveError = null
                    }
                    is LocationEditorEvent.ParentSelected -> {
                        form = form.copy(parentId = event.locationId)
                        validationMessages = emptyList()
                        saveError = null
                    }
                    LocationEditorEvent.SaveSelected -> {
                        coroutineScope.launch {
                            try {
                                isSaving = true
                                validationMessages = emptyList()
                                saveError = null
                                val input = LocationInput(
                                    name = form.name,
                                    parentId = form.parentId,
                                )
                                val result = if (locationId == null) {
                                    createLocation(input)
                                } else {
                                    updateLocation(locationId, input)
                                }
                                isSaving = false
                                when (result) {
                                    is LocationSaveResult.Success -> navigator.pop()
                                    is LocationSaveResult.ValidationFailed -> {
                                        validationMessages = result.errors.map { it.message }
                                    }
                                    LocationSaveResult.NotFound -> {
                                        saveError = "Location could not be found."
                                    }
                                }
                            } catch (exception: Exception) {
                                if (exception is CancellationException) throw exception
                                isSaving = false
                                saveError = "Location could not be saved."
                            }
                        }
                    }
                }
                Unit
            }
        }

        return when {
            !isLoaded -> LocationEditorState.Loading
            isMissing -> LocationEditorState.Missing(eventSink)
            else -> LocationEditorState.Editing(
                title = if (locationId == null) "Add location" else "Edit location",
                form = form,
                parentOptions = parentOptions,
                isSaving = isSaving,
                validationMessages = validationMessages,
                saveErrorMessage = saveError,
                eventSink = eventSink,
            )
        }
    }
}

private sealed interface ObservedLocations {
    data object Loading : ObservedLocations
    data object Failed : ObservedLocations
    data class Loaded(val locations: List<Location>) : ObservedLocations
}

private fun List<Location>.toListItems(): List<LocationListItemUi> =
    sortedBy { it.name }.map { location ->
        LocationListItemUi(
            id = location.id,
            name = location.name,
            parentName = firstOrNull { it.id == location.parentId }?.name,
        )
    }

private fun List<LocationListItemUi>.filterBy(query: String): List<LocationListItemUi> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return this
    return filter { location ->
        location.name.contains(normalized, ignoreCase = true) ||
            location.parentName?.contains(normalized, ignoreCase = true) == true
    }
}

private fun Location.toDetailUi(locations: List<Location>): LocationDetailUi =
    LocationDetailUi(
        id = id,
        name = name,
        parentName = locations.firstOrNull { it.id == parentId }?.name,
    )
