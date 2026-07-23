package com.topic11.cs426.domain.model

data class AssetSummary(
    val id: AssetId,
    val name: String,
    val code: String? = null,
    val locationName: String? = null,
    val nextInspectionDueAtMillis: Long? = null,
) {
    init {
        require(name.isNotBlank()) { "Asset name cannot be blank." }
    }
}
