package com.topic11.cs426.feature.inspection

/**
 * Test fixture for the inspection presenter.
 *
 * Provides a stable, deterministic session with known counts so presenter tests can
 * assert against exact values:
 *   - 3 sections
 *   - 7 items total
 *   - 5 required items (drives validation failure count in tests)
 *   - 2 optional items
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
                ),
                ChecklistItemUi(
                    id = "item-cables",
                    prompt = "Cables and connectors are undamaged",
                    required = true,
                    answer = ChecklistAnswerUi.Unanswered,
                    note = "",
                ),
                ChecklistItemUi(
                    id = "item-label",
                    prompt = "Asset label is visible",
                    required = false,
                    answer = ChecklistAnswerUi.Unanswered,
                    note = "",
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
                ),
                ChecklistItemUi(
                    id = "item-exit",
                    prompt = "Emergency exit is unobstructed",
                    required = true,
                    answer = ChecklistAnswerUi.Unanswered,
                    note = "",
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
                ),
                ChecklistItemUi(
                    id = "item-temp",
                    prompt = "Temperature is within operating range",
                    required = true,
                    answer = ChecklistAnswerUi.Unanswered,
                    note = "",
                ),
            ),
        ),
    )
}
