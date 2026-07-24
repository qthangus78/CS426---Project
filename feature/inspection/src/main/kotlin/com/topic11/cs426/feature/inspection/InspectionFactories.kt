package com.topic11.cs426.feature.inspection

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.topic11.cs426.core.navigation.InspectionScreen
import com.topic11.cs426.domain.repository.TemplateRepository
import com.topic11.cs426.domain.usecase.CompleteInspectionUseCase
import com.topic11.cs426.domain.usecase.ObserveInspectionUseCase
import com.topic11.cs426.domain.usecase.SaveInspectionDraftUseCase
import com.topic11.cs426.domain.usecase.ValidateInspectionUseCase

class InspectionPresenterFactory(
    private val observeInspection: ObserveInspectionUseCase,
    private val templateRepository: TemplateRepository,
    private val saveInspectionDraft: SaveInspectionDraftUseCase,
    private val validateInspection: ValidateInspectionUseCase,
    private val completeInspection: CompleteInspectionUseCase,
) : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? {
        return when (screen) {
            is InspectionScreen -> InspectionPresenter(
                screen = screen,
                navigator = navigator,
                observeInspection = observeInspection,
                templateRepository = templateRepository,
                saveInspectionDraft = saveInspectionDraft,
                validateInspection = validateInspection,
                completeInspection = completeInspection,
            )
            else -> null
        }
    }
}

class InspectionUiFactory : Ui.Factory {
    override fun create(
        screen: Screen,
        context: CircuitContext,
    ): Ui<*>? {
        return when (screen) {
            is InspectionScreen -> ui<InspectionState> { state, modifier ->
                InspectionUi(
                    state = state,
                    modifier = modifier,
                )
            }
            else -> null
        }
    }
}
