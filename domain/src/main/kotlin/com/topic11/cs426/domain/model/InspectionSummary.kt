package com.topic11.cs426.domain.model

data class InspectionSummary(
    val id: InspectionId,
    val title: String,
    val status: InspectionStatus,
    val completedItems: Int,
    val totalItems: Int,
) {
    init {
        require(title.isNotBlank()) { "Inspection title cannot be blank." }
        require(completedItems >= 0) { "Completed item count cannot be negative." }
        require(totalItems >= 0) { "Total item count cannot be negative." }
        require(completedItems <= totalItems) {
            "Completed item count cannot exceed total item count."
        }
    }

    val progressFraction: Float
        get() = if (totalItems == 0) 0f else completedItems.toFloat() / totalItems.toFloat()
}
