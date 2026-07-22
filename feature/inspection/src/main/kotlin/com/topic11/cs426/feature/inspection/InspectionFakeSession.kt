package com.topic11.cs426.feature.inspection

/**
 * Hard-coded inspection session used for Phase 1.
 *
 * Phase 2: replace with a domain session Flow once ObserveInspectionSessionUseCase ships.
 * The Presenter will map domain models onto [InspectionSectionUi] / [ChecklistItemUi] and
 * this file can be deleted.
 */
internal object FakeSession {

    val sections: List<InspectionSectionUi> = listOf(
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
}
