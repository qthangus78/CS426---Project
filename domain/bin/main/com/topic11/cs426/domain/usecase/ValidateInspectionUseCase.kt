package com.topic11.cs426.domain.usecase

import com.topic11.cs426.domain.model.ChecklistAnswerValue
import com.topic11.cs426.domain.model.ChecklistItemId
import com.topic11.cs426.domain.model.InspectionSession
import com.topic11.cs426.domain.model.InspectionTemplate
import com.topic11.cs426.domain.model.InspectionValidationError
import com.topic11.cs426.domain.model.ValidationResult

class ValidateInspectionUseCase {

    operator fun invoke(
        session: InspectionSession,
        template: InspectionTemplate,
    ): ValidationResult {
        val errors = mutableListOf<InspectionValidationError>()

        // Build map: itemId -> ChecklistItem
        val allItems: Map<ChecklistItemId, com.topic11.cs426.domain.model.ChecklistItem> =
            template.sections.flatMap { section ->
                section.items.map { it.id to it }
            }.toMap()

        // Build map: itemId -> answer
        val answerMap = session.answers.associateBy { it.checklistItemId }

        for ((itemId, item) in allItems) {
            val answer = answerMap[itemId]

            // RULE 1: Required item unanswered → error
            if (item.required && (answer == null || answer.value == null)) {
                errors.add(
                    InspectionValidationError.RequiredItemUnanswered(
                        itemId = itemId,
                        message = "Required item '${item.title}' has not been answered.",
                    ),
                )
            }

            // RULE 2: Critical + Fail must have evidence
            if (item.critical && answer?.value == ChecklistAnswerValue.Fail) {
                if (answer.evidenceIds.isEmpty()) {
                    errors.add(
                        InspectionValidationError.CriticalFailureNeedsEvidence(
                            itemId = itemId,
                            message = "Critical item '${item.title}' requires evidence on failure.",
                        ),
                    )
                }
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
        )
    }
}
