package com.topic11.cs426.domain.model

data class CompletedInspection(
    val id: InspectionId,
    val answers: List<InspectionAnswer>,
    val score: InspectionScore,
    val issues: List<MaintenanceIssue>,
    val completedAtMillis: Long,
)
