package com.topic11.cs426.data

import com.topic11.cs426.core.database.dao.CatalogDao
import com.topic11.cs426.data.mapping.toChecklistItemEntities
import com.topic11.cs426.data.mapping.toDomain
import com.topic11.cs426.data.mapping.toSectionEntities
import com.topic11.cs426.data.mapping.toSummary
import com.topic11.cs426.data.mapping.toTemplateEntity
import com.topic11.cs426.domain.model.InspectionTemplate
import com.topic11.cs426.domain.model.InspectionTemplateSummary
import com.topic11.cs426.domain.model.TemplateId
import com.topic11.cs426.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RoomTemplateRepository(
    private val catalogDao: CatalogDao,
) : TemplateRepository {
    override fun observeTemplates(): Flow<List<InspectionTemplateSummary>> =
        catalogDao.observeTemplateAggregates()
            .map { templates -> templates.map { it.toSummary() } }
            .distinctUntilChanged()

    override fun observeTemplate(id: TemplateId): Flow<InspectionTemplate?> =
        catalogDao.observeTemplateAggregate(id.value)
            .map { it?.toDomain() }
            .distinctUntilChanged()

    override suspend fun getTemplate(id: TemplateId): InspectionTemplate? =
        catalogDao.getTemplateAggregate(id.value)?.toDomain()

    override suspend fun saveTemplate(template: InspectionTemplate) {
        catalogDao.upsertTemplateAggregate(
            template = template.toTemplateEntity(),
            sections = template.toSectionEntities(),
            items = template.toChecklistItemEntities(),
        )
    }
}
