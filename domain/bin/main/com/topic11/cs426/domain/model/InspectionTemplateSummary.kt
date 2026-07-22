package com.topic11.cs426.domain.model

data class InspectionTemplateSummary(
    val id: TemplateId,
    val name: String,
    val version: Int,
    val sectionCount: Int,
) {
    init {
        require(name.isNotBlank()) { "Template name cannot be blank." }
        require(version >= 1) { "Template version must be >= 1." }
        require(sectionCount >= 0) { "Section count cannot be negative." }
    }
}
