package com.topic11.cs426.core.testing

import com.topic11.cs426.domain.model.InspectionTemplate
import com.topic11.cs426.domain.model.InspectionTemplateSummary
import com.topic11.cs426.domain.model.TemplateId
import com.topic11.cs426.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeTemplateRepository(
    templates: Map<TemplateId, InspectionTemplate> = emptyMap(),
) : TemplateRepository {
    private val templatesById = MutableStateFlow(templates)
    private val templateList = MutableStateFlow(templates.values.toSummaries())

    override fun observeTemplates(): Flow<List<InspectionTemplateSummary>> = templateList

    override fun observeTemplate(id: TemplateId): Flow<InspectionTemplate?> {
        return templatesById.map { templates -> templates[id] }
    }

    override suspend fun getTemplate(id: TemplateId): InspectionTemplate? {
        return templatesById.value[id]
    }

    override suspend fun saveTemplate(template: InspectionTemplate) {
        templatesById.value = templatesById.value + (template.id to template)
        templateList.value = templatesById.value.values.toSummaries()
    }
}

private fun Collection<InspectionTemplate>.toSummaries(): List<InspectionTemplateSummary> {
    return map { template ->
        InspectionTemplateSummary(
            id = template.id,
            name = template.name,
            version = template.version,
            sectionCount = template.sections.size,
        )
    }
}
