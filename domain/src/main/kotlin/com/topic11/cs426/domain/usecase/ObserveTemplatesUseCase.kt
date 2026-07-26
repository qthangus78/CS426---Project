package com.topic11.cs426.domain.usecase

import com.topic11.cs426.domain.model.InspectionTemplateSummary
import com.topic11.cs426.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow

class ObserveTemplatesUseCase(
    private val templateRepository: TemplateRepository,
) {
    operator fun invoke(): Flow<List<InspectionTemplateSummary>> = templateRepository.observeTemplates()
}
