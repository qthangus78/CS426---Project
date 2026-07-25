package com.topic11.cs426.domain.usecase

import com.topic11.cs426.domain.model.InspectionTemplate
import com.topic11.cs426.domain.model.TemplateId
import com.topic11.cs426.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow

class ObserveTemplateUseCase(
    private val templateRepository: TemplateRepository,
) {
    operator fun invoke(templateId: TemplateId): Flow<InspectionTemplate?> {
        return templateRepository.observeTemplate(templateId)
    }
}