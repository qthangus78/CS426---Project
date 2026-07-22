package com.topic11.cs426.domain.repository

import com.topic11.cs426.domain.model.InspectionTemplate
import com.topic11.cs426.domain.model.InspectionTemplateSummary
import com.topic11.cs426.domain.model.TemplateId
import kotlinx.coroutines.flow.Flow

interface TemplateRepository {
    fun observeTemplates(): Flow<List<InspectionTemplateSummary>>

    fun observeTemplate(id: TemplateId): Flow<InspectionTemplate?>

    suspend fun getTemplate(id: TemplateId): InspectionTemplate?
}
