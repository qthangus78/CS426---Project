package com.topic11.cs426.domain.model

data class InspectionSession(
    val id: InspectionId,
    val assetId: AssetId,
    val assetName: String,
    val templateId: TemplateId,
    val status: InspectionStatus,
    val currentSectionId: SectionId? = null,
    val answers: List<InspectionAnswer> = emptyList(),
    val startedAtMillis: Long,
    val updatedAtMillis: Long,
    val completedAtMillis: Long? = null,
    val score: InspectionScore? = null,
) {
    init {
        require(assetName.isNotBlank()) { "Asset name cannot be blank." }
        if (status == InspectionStatus.COMPLETED) {
            require(completedAtMillis != null) {
                "Completed inspection must have completedAtMillis."
            }
        }
    }

    /**
     * Progress as a fraction [0..1], based on answered vs total required items.
     */
    val progressFraction: Float
        get() {
            val answered = answers.count { it.value != null }
            val total = answers.size
            return if (total == 0) 0f else answered.toFloat() / total.toFloat()
        }
}
