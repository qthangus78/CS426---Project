package com.topic11.cs426.domain.usecase

import com.topic11.cs426.domain.model.ChecklistAnswerType
import com.topic11.cs426.domain.model.ChecklistItem
import com.topic11.cs426.domain.model.ChecklistItemId
import com.topic11.cs426.domain.model.InspectionSection
import com.topic11.cs426.domain.model.InspectionTemplate
import com.topic11.cs426.domain.model.SectionId
import com.topic11.cs426.domain.model.TemplateId
import com.topic11.cs426.domain.repository.TemplateRepository
import java.util.UUID

class GetTemplateUseCase(
    private val templateRepository: TemplateRepository,
) {
    suspend operator fun invoke(templateId: TemplateId): InspectionTemplate? {
        return templateRepository.getTemplate(templateId)
    }
}

class CreateTemplateUseCase(
    private val templateRepository: TemplateRepository,
    private val idFactory: () -> TemplateId = { TemplateId("template-${UUID.randomUUID()}") },
) {
    suspend operator fun invoke(input: TemplateCreateInput): TemplateSaveResult {
        val normalized = input.normalized()
        val errors = validateTemplateCreateInput(normalized)
        if (errors.isNotEmpty()) return TemplateSaveResult.ValidationFailed(errors)

        val templateId = idFactory()
        val sectionId = SectionId("${templateId.value}-section-1")
        val itemId = ChecklistItemId("${templateId.value}-item-1")
        val template = InspectionTemplate(
            id = templateId,
            name = normalized.name,
            version = 1,
            recurrencePolicyDays = normalized.recurrencePolicyDays,
            sections = listOf(
                InspectionSection(
                    id = sectionId,
                    templateId = templateId,
                    title = normalized.sectionTitle,
                    order = 0,
                    items = listOf(
                        ChecklistItem(
                            id = itemId,
                            sectionId = sectionId,
                            title = normalized.itemTitle,
                            description = normalized.itemDescription,
                            required = normalized.required,
                            critical = normalized.critical,
                            weight = normalized.weight,
                            answerType = normalized.answerType,
                        ),
                    ),
                ),
            ),
        )
        templateRepository.saveTemplate(template)
        return TemplateSaveResult.Success(template)
    }
}

class UpdateTemplateMetadataUseCase(
    private val templateRepository: TemplateRepository,
) {
    suspend operator fun invoke(
        templateId: TemplateId,
        input: TemplateMetadataInput,
    ): TemplateSaveResult {
        val current = templateRepository.getTemplate(templateId) ?: return TemplateSaveResult.NotFound
        val normalized = input.normalized()
        val errors = validateTemplateMetadataInput(normalized)
        if (errors.isNotEmpty()) return TemplateSaveResult.ValidationFailed(errors)

        val template = current.copy(
            name = normalized.name,
            recurrencePolicyDays = normalized.recurrencePolicyDays,
        )
        templateRepository.saveTemplate(template)
        return TemplateSaveResult.Success(template)
    }
}

data class TemplateCreateInput(
    val name: String,
    val recurrencePolicyDays: Int?,
    val sectionTitle: String,
    val itemTitle: String,
    val itemDescription: String?,
    val required: Boolean,
    val critical: Boolean,
    val weight: Int,
    val answerType: ChecklistAnswerType,
)

data class TemplateMetadataInput(
    val name: String,
    val recurrencePolicyDays: Int?,
)

sealed interface TemplateSaveResult {
    data class Success(val template: InspectionTemplate) : TemplateSaveResult
    data class ValidationFailed(val errors: List<TemplateValidationError>) : TemplateSaveResult
    data object NotFound : TemplateSaveResult
}

sealed interface TemplateValidationError {
    val message: String

    data object NameRequired : TemplateValidationError {
        override val message: String = "Template name is required."
    }

    data object SectionTitleRequired : TemplateValidationError {
        override val message: String = "Section title is required."
    }

    data object ItemTitleRequired : TemplateValidationError {
        override val message: String = "Checklist item title is required."
    }

    data object WeightInvalid : TemplateValidationError {
        override val message: String = "Checklist item weight must be zero or greater."
    }

    data object RecurrenceInvalid : TemplateValidationError {
        override val message: String = "Recurrence interval must be positive."
    }

    data object AnswerTypeUnsupported : TemplateValidationError {
        override val message: String = "This checklist answer type is not available for template creation."
    }
}

private fun TemplateCreateInput.normalized(): TemplateCreateInput {
    return copy(
        name = name.trim(),
        sectionTitle = sectionTitle.trim(),
        itemTitle = itemTitle.trim(),
        itemDescription = itemDescription?.trim()?.takeIf { it.isNotEmpty() },
    )
}

private fun TemplateMetadataInput.normalized(): TemplateMetadataInput {
    return copy(name = name.trim())
}

private fun validateTemplateCreateInput(
    input: TemplateCreateInput,
): List<TemplateValidationError> {
    val errors = validateTemplateMetadataInput(
        TemplateMetadataInput(
            name = input.name,
            recurrencePolicyDays = input.recurrencePolicyDays,
        ),
    ).toMutableList()
    if (input.sectionTitle.isBlank()) errors += TemplateValidationError.SectionTitleRequired
    if (input.itemTitle.isBlank()) errors += TemplateValidationError.ItemTitleRequired
    if (input.weight < 0) errors += TemplateValidationError.WeightInvalid
    if (input.answerType !in supportedTemplateCreateAnswerTypes) {
        errors += TemplateValidationError.AnswerTypeUnsupported
    }
    return errors
}

private fun validateTemplateMetadataInput(
    input: TemplateMetadataInput,
): List<TemplateValidationError> {
    val errors = mutableListOf<TemplateValidationError>()
    if (input.name.isBlank()) errors += TemplateValidationError.NameRequired
    val recurrence = input.recurrencePolicyDays
    if (recurrence != null && recurrence <= 0) errors += TemplateValidationError.RecurrenceInvalid
    return errors
}

private val supportedTemplateCreateAnswerTypes = setOf(
    ChecklistAnswerType.PASS_FAIL_NA,
    ChecklistAnswerType.TEXT,
)
