package com.topic11.cs426.domain.usecase

import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionSession
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.TemplateId
import com.topic11.cs426.domain.repository.InspectionRepository
import com.topic11.cs426.domain.repository.TemplateRepository

class StartInspectionUseCase(
    private val inspectionRepository: InspectionRepository,
    private val templateRepository: TemplateRepository,
) {
    suspend operator fun invoke(
        assetId: AssetId,
        assetName: String,
        templateId: TemplateId,
        startedAtMillis: Long = System.currentTimeMillis(),
    ): InspectionId {
        val template = templateRepository.getTemplate(templateId)
            ?: throw IllegalStateException("Template $templateId not found")

        val firstSectionId = template.sections.minByOrNull { it.order }?.id
            ?: throw IllegalStateException("Template $templateId has no sections")

        val inspectionId = inspectionRepository.createInspection(
            assetId = assetId.value,
            assetName = assetName,
            templateId = templateId.value,
            startedAtMillis = startedAtMillis,
        )

        // Save initial draft with first section selected
        val session = InspectionSession(
            id = inspectionId,
            assetId = assetId,
            assetName = assetName,
            templateId = templateId,
            status = InspectionStatus.IN_PROGRESS,
            currentSectionId = firstSectionId,
            answers = emptyList(),
            startedAtMillis = startedAtMillis,
            updatedAtMillis = startedAtMillis,
        )
        inspectionRepository.saveDraft(session)

        return inspectionId
    }
}
