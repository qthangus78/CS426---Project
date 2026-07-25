package com.topic11.cs426.domain.model

data class Asset(
    val id: AssetId,
    val name: String,
    val code: String? = null,
    val locationId: LocationId? = null,
    val recurrencePolicyDays: Int? = null,
    val nextInspectionDueAtMillis: Long? = null,
) {
    init {
        require(name.isNotBlank()) { "Asset name cannot be blank." }
        require(recurrencePolicyDays == null || recurrencePolicyDays > 0) {
            "Recurrence policy days must be positive."
        }
    }
}
