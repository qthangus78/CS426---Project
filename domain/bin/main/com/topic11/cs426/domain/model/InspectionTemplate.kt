package com.topic11.cs426.domain.model

data class InspectionTemplate(
    val id: TemplateId,
    val name: String,
    val version: Int,
    val sections: List<InspectionSection>,
    val recurrencePolicyDays: Int? = null,
) {
    init {
        require(name.isNotBlank()) { "Template name cannot be blank." }
        require(version >= 1) { "Template version must be >= 1." }
        require(sections.isNotEmpty()) { "Template must have at least one section." }
        require(sections.map { it.id }.distinct().size == sections.size) {
            "Section IDs must be unique within a template."
        }
    }
}

data class InspectionSection(
    val id: SectionId,
    val templateId: TemplateId,
    val title: String,
    val order: Int,
    val items: List<ChecklistItem>,
) {
    init {
        require(title.isNotBlank()) { "Section title cannot be blank." }
        require(order >= 0) { "Section order must be >= 0." }
        require(items.isNotEmpty()) { "Section must have at least one item." }
        require(items.map { it.id }.distinct().size == items.size) {
            "Checklist item IDs must be unique within a section."
        }
    }
}

data class ChecklistItem(
    val id: ChecklistItemId,
    val sectionId: SectionId,
    val title: String,
    val description: String? = null,
    val required: Boolean = false,
    val critical: Boolean = false,
    val weight: Int = 1,
    val answerType: ChecklistAnswerType,
) {
    init {
        require(title.isNotBlank()) { "Checklist item title cannot be blank." }
        require(weight >= 0) { "Checklist item weight must be >= 0." }
    }
}
