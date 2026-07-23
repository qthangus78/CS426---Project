package com.topic11.cs426.core.testing

import com.topic11.cs426.domain.model.InspectionTemplate
import com.topic11.cs426.domain.model.InspectionTemplateSummary
import com.topic11.cs426.domain.model.TemplateId
import com.topic11.cs426.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeTemplateRepository(
    private val templates: Map<TemplateId, InspectionTemplate> = emptyMap(),
) : TemplateRepository {
    private val templateList = MutableStateFlow(
        templates.map { (id, t) ->
            InspectionTemplateSummary(
                id = id,
                name = t.name,
                version = t.version,
                sectionCount = t.sections.size,
            )
        },
    )

    override fun observeTemplates(): Flow<List<InspectionTemplateSummary>> = templateList

    override fun observeTemplate(id: TemplateId): Flow<InspectionTemplate?> {
        return MutableStateFlow(templates[id])
    }

    override suspend fun getTemplate(id: TemplateId): InspectionTemplate? {
        return templates[id]
    }
}
