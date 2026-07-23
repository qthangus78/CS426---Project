package com.topic11.cs426.feature.inspection

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui
import com.topic11.cs426.core.navigation.InspectionScreen

// TODO Phase 2: add ObserveInspectionSessionUseCase to constructor once domain ships.
class InspectionPresenterFactory : Presenter.Factory {
    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? {
        return when (screen) {
            is InspectionScreen -> InspectionPresenter(
                screen = screen,
                navigator = navigator,
                initialSections = inspectionDemoSections,
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

/**
 * Phase 1 placeholder sections used for demo and manual testing.
 *
 * Phase 2: remove once InspectionPresenter is wired to ObserveInspectionSessionUseCase
 * and reads live session data from the domain layer.
 */
internal val inspectionDemoSections: List<InspectionSectionUi> = listOf(
    InspectionSectionUi(
        id = "section-equipment",
        title = "Equipment Condition",
        items = listOf(
            ChecklistItemUi(
                id = "item-power",
                prompt = "Power supply is operational",
                required = true,
                answer = ChecklistAnswerUi.Unanswered,
                note = "",
                evidenceCount = 0,
            ),
            ChecklistItemUi(
                id = "item-cables",
                prompt = "Cables and connectors are undamaged",
                required = true,
                answer = ChecklistAnswerUi.Unanswered,
                note = "",
                evidenceCount = 0,
            ),
            ChecklistItemUi(
                id = "item-label",
                prompt = "Asset label is visible",
                required = false,
                answer = ChecklistAnswerUi.Unanswered,
                note = "",
                evidenceCount = 0,
            ),
        ),
    ),
    InspectionSectionUi(
        id = "section-safety",
        title = "Safety Checks",
        items = listOf(
            ChecklistItemUi(
                id = "item-fire",
                prompt = "Fire extinguisher is accessible",
                required = true,
                answer = ChecklistAnswerUi.Unanswered,
                note = "",
                evidenceCount = 0,
            ),
            ChecklistItemUi(
                id = "item-exit",
                prompt = "Emergency exit is unobstructed",
                required = true,
                answer = ChecklistAnswerUi.Unanswered,
                note = "",
                evidenceCount = 0,
            ),
        ),
    ),
    InspectionSectionUi(
        id = "section-environment",
        title = "Environment",
        items = listOf(
            ChecklistItemUi(
                id = "item-lighting",
                prompt = "Lighting is adequate",
                required = false,
                answer = ChecklistAnswerUi.Unanswered,
                note = "",
                evidenceCount = 0,
            ),
            ChecklistItemUi(
                id = "item-temp",
                prompt = "Temperature is within operating range",
                required = true,
                answer = ChecklistAnswerUi.Unanswered,
                note = "",
                evidenceCount = 0,
            ),
        ),
    ),
)
