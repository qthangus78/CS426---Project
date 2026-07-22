package com.topic11.cs426.domain.model

data class InspectionScore(
    val earnedWeight: Int,
    val totalWeight: Int,
) {
    init {
        require(earnedWeight >= 0) { "Earned weight cannot be negative." }
        require(totalWeight >= 0) { "Total weight cannot be negative." }
        require(earnedWeight <= totalWeight) {
            "Earned weight cannot exceed total weight."
        }
    }

    val percent: Float
        get() = if (totalWeight == 0) 1f else earnedWeight.toFloat() / totalWeight.toFloat()
}
