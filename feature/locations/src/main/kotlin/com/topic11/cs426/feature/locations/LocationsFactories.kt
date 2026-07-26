package com.topic11.cs426.feature.locations

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.topic11.cs426.core.navigation.LocationDetailScreen
import com.topic11.cs426.core.navigation.LocationEditorScreen
import com.topic11.cs426.core.navigation.LocationsScreen
import com.topic11.cs426.domain.usecase.CreateLocationUseCase
import com.topic11.cs426.domain.usecase.GetLocationUseCase
import com.topic11.cs426.domain.usecase.ObserveLocationsUseCase
import com.topic11.cs426.domain.usecase.UpdateLocationUseCase

class LocationsPresenterFactory(
    private val observeLocations: ObserveLocationsUseCase,
    private val getLocation: GetLocationUseCase,
    private val createLocation: CreateLocationUseCase,
    private val updateLocation: UpdateLocationUseCase,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? {
        return when (screen) {
            LocationsScreen -> LocationsPresenter(
                observeLocations = observeLocations,
                navigator = navigator,
            )
            is LocationDetailScreen -> LocationDetailPresenter(
                screen = screen,
                observeLocations = observeLocations,
                navigator = navigator,
            )
            is LocationEditorScreen -> LocationEditorPresenter(
                screen = screen,
                getLocation = getLocation,
                observeLocations = observeLocations,
                createLocation = createLocation,
                updateLocation = updateLocation,
                navigator = navigator,
            )
            else -> null
        }
    }
}

class LocationsUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? {
        return when (screen) {
            LocationsScreen -> ui<LocationsState> { state, modifier ->
                LocationsUi(state = state, modifier = modifier)
            }
            is LocationDetailScreen -> ui<LocationDetailState> { state, modifier ->
                LocationDetailUi(state = state, modifier = modifier)
            }
            is LocationEditorScreen -> ui<LocationEditorState> { state, modifier ->
                LocationEditorUi(state = state, modifier = modifier)
            }
            else -> null
        }
    }
}
