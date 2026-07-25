package com.topic11.cs426.core.database.dao

data class InspectionSessionRecord(
    val inspectionId: String,
    val assetId: String,
    val assetName: String,
    val templateId: String,
    val templateName: String,
    val lifecycleStatus: String,
    val syncStatus: String,
    val currentSectionId: String?,
    val startedAtMillis: Long,
    val updatedAtMillis: Long,
    val completedAtMillis: Long?,
    val earnedWeight: Double?,
    val totalWeight: Double?,
)
