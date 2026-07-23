package com.topic11.cs426.core.database.dao

data class InspectionSummaryRecord(
    val inspectionId: String,
    val title: String,
    val lifecycleStatus: String,
    val syncStatus: String,
    val completedItems: Int,
    val totalItems: Int,
)
