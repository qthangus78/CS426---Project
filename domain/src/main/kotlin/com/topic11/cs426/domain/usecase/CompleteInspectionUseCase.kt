package com.topic11.cs426.domain.usecase

import com.topic11.cs426.domain.model.ChecklistAnswerValue
import com.topic11.cs426.domain.model.CompleteInspectionResult
import com.topic11.cs426.domain.model.CompletedInspection
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionSession
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.InspectionTemplate
import com.topic11.cs426.domain.model.InspectionValidationError
import com.topic11.cs426.domain.repository.AssetRepository
import com.topic11.cs426.domain.repository.InspectionRepository
import com.topic11.cs426.domain.repository.IssueRepository
import com.topic11.cs426.domain.repository.TemplateRepository

class CompleteInspectionUseCase(
    private val inspectionRepository: InspectionRepository,
    private val templateRepository: TemplateRepository,
    private val assetRepository: AssetRepository,
    private val issueRepository: IssueRepository,
    private val validateInspection: ValidateInspectionUseCase,
    private val calculateScore: CalculateInspectionScoreUseCase,
    private val createIssue: CreateMaintenanceIssueUseCase,
    private val scheduleNext: ScheduleNextInspectionUseCase,
) {
    suspend operator fun invoke(
        inspectionId: InspectionId,
        completedAtMillis: Long = System.currentTimeMillis(),
    ): CompleteInspectionResult {
        // Get session
        val session = inspectionRepository.getInspection(inspectionId)
            ?: return CompleteInspectionResult.Error("Inspection not found")

        // Get template
        val template = templateRepository.getTemplate(session.templateId)
            ?: return CompleteInspectionResult.Error("Template not found")

        // Get asset
        val asset = assetRepository.getAsset(session.assetId)
            ?: return CompleteInspectionResult.Error("Asset not found")

        // RULE 4: Lifecycle — only REVIEWING or IN_PROGRESS can be completed
        if (session.status != InspectionStatus.REVIEWING &&
            session.status != InspectionStatus.IN_PROGRESS
        ) {
            val error = InspectionValidationError.InvalidLifecycleTransition(
                from = session.status,
                to = InspectionStatus.COMPLETED,
                message = "Cannot complete inspection in status ${session.status}.",
            )
            return CompleteInspectionResult.ValidationFailed(listOf(error))
        }

        // RULE 1 + 2: Validate before completing
        val validation = validateInspection(session, template)
        if (!validation.isValid) {
            return CompleteInspectionResult.ValidationFailed(validation.errors)
        }

        // RULE 5 + 6: Calculate score
        val score = calculateScore(session, template)

        // RULE 3: Create issues for critical failures
        val criticalFailures = findCriticalFailures(session, template)
        val newIssues = if (criticalFailures.isNotEmpty()) {
            createIssue(criticalFailures)
        } else {
            emptyList()
        }

        // RULE 8: Schedule next inspection (fallback: template → asset policy)
        val nextDue = scheduleNext(template, asset, completedAtMillis)

        // Persist nextDue into asset (Cách B)
        if (nextDue != null) {
            val updatedAsset = asset.copy(nextInspectionDueAtMillis = nextDue)
            assetRepository.saveAsset(updatedAsset)
        }

        // Save complete
        val completed = CompletedInspection(
            id = session.id,
            answers = session.answers,
            score = score,
            issues = newIssues,
            completedAtMillis = completedAtMillis,
        )
        inspectionRepository.complete(completed)

        return CompleteInspectionResult.Success(
            score = score,
            issues = newIssues,
            nextInspectionDueAtMillis = nextDue,
            completedAtMillis = completedAtMillis,
        )
    }

    private fun findCriticalFailures(
        session: InspectionSession,
        template: InspectionTemplate,
    ): List<CriticalFailure> {
        val answerMap = session.answers.associateBy { it.checklistItemId }
        val failures = mutableListOf<CriticalFailure>()

        for (section in template.sections) {
            for (item in section.items) {
                val answer = answerMap[item.id]
                if (item.critical && answer?.value == ChecklistAnswerValue.Fail) {
                    failures.add(
                        CriticalFailure(
                            inspectionId = session.id,
                            assetId = session.assetId,
                            checklistItemId = item.id,
                            title = "Critical failure: ${item.title}",
                            description = item.description,
                        ),
                    )
                }
            }
        }

        return failures
    }
}
