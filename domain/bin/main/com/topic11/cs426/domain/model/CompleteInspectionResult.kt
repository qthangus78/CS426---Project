package com.topic11.cs426.domain.model

sealed interface CompleteInspectionResult {
    data class Success(
        val score: InspectionScore,
        val issues: List<MaintenanceIssue>,
        val nextInspectionDueAtMillis: Long?,
        val completedAtMillis: Long,
    ) : CompleteInspectionResult

    data class ValidationFailed(
        val errors: List<InspectionValidationError>,
    ) : CompleteInspectionResult

    data class Error(
        val message: String,
    ) : CompleteInspectionResult
}
