package com.topic11.cs426.domain.model

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<InspectionValidationError> = emptyList(),
)

sealed interface InspectionValidationError {
    val message: String
    val itemId: ChecklistItemId?

    data class RequiredItemUnanswered(
        override val itemId: ChecklistItemId,
        override val message: String = "Required item has not been answered.",
    ) : InspectionValidationError

    data class CriticalFailureNeedsEvidence(
        override val itemId: ChecklistItemId,
        override val message: String = "Critical failure requires at least one piece of evidence.",
    ) : InspectionValidationError

    data class InvalidLifecycleTransition(
        val from: InspectionStatus,
        val to: InspectionStatus,
        override val message: String,
        override val itemId: ChecklistItemId? = null,
    ) : InspectionValidationError

    data class ReportNotAllowed(
        val inspectionId: InspectionId,
        override val message: String = "Report cannot be generated for an incomplete inspection.",
        override val itemId: ChecklistItemId? = null,
    ) : InspectionValidationError

    data class ScoreCalculationError(
        override val message: String,
        override val itemId: ChecklistItemId? = null,
    ) : InspectionValidationError
}
