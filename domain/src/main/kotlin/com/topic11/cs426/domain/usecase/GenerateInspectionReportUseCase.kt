package com.topic11.cs426.domain.usecase

import com.topic11.cs426.domain.model.ChecklistAnswerValue
import com.topic11.cs426.domain.model.ChecklistItemId
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionAnswer
import com.topic11.cs426.domain.model.InspectionReport
import com.topic11.cs426.domain.model.InspectionReportItem
import com.topic11.cs426.domain.model.InspectionReportSection
import com.topic11.cs426.domain.model.InspectionScore
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.InspectionTemplate
import com.topic11.cs426.domain.model.ReportGenerationError
import com.topic11.cs426.domain.model.ReportGenerationResult
import com.topic11.cs426.domain.model.ReportId
import com.topic11.cs426.domain.repository.InspectionRepository
import com.topic11.cs426.domain.repository.IssueRepository
import com.topic11.cs426.domain.repository.TemplateRepository
import java.util.UUID
import kotlinx.coroutines.CancellationException

class GenerateInspectionReportUseCase(
    private val inspectionRepository: InspectionRepository,
    private val templateRepository: TemplateRepository,
    private val issueRepository: IssueRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    private val idFactory: () -> ReportId = { ReportId(UUID.randomUUID().toString()) },
) {
    suspend operator fun invoke(inspectionId: InspectionId): ReportGenerationResult {
        val session = inspectionRepository.getInspection(inspectionId)
            ?: return ReportGenerationResult.Failed(ReportGenerationError.InspectionMissing)

        if (!session.status.isReportEligible()) {
            return ReportGenerationResult.Failed(
                ReportGenerationError.NotEligible(session.status),
            )
        }

        val template = templateRepository.getTemplate(session.templateId)
            ?: return ReportGenerationResult.Failed(ReportGenerationError.TemplateMissing)

        val issues = try {
            issueRepository.getIssuesForInspection(inspectionId)
                .sortedWith(compareBy({ it.createdAtMillis }, { it.id.value }))
        } catch (failure: Throwable) {
            if (failure is CancellationException) throw failure
            return ReportGenerationResult.Failed(ReportGenerationError.IssueLookupFailed)
        }

        val completedAtMillis = session.completedAtMillis ?: session.updatedAtMillis
        val score = session.score ?: InspectionScore(earnedWeight = 0, totalWeight = 0)

        return ReportGenerationResult.Success(
            InspectionReport(
                id = idFactory(),
                inspectionId = inspectionId,
                assetName = session.assetName,
                templateName = session.templateName,
                summary = "Inspection report for ${session.assetName}",
                score = score,
                completedAtMillis = completedAtMillis,
                sections = template.toReportSections(session.answers.associateBy { it.checklistItemId }),
                issues = issues,
                generatedAtMillis = clock(),
            ),
        )
    }
}

fun InspectionStatus.isReportEligible(): Boolean =
    this == InspectionStatus.COMPLETED || this == InspectionStatus.SYNC_PENDING

private fun InspectionTemplate.toReportSections(
    answersByItemId: Map<ChecklistItemId, InspectionAnswer>,
): List<InspectionReportSection> =
    sections
        .sortedBy { section -> section.order }
        .map { section ->
            InspectionReportSection(
                id = section.id,
                title = section.title,
                items = section.items.map { item ->
                    val answer = answersByItemId[item.id]
                    InspectionReportItem(
                        id = item.id,
                        title = item.title,
                        required = item.required,
                        critical = item.critical,
                        weight = item.weight,
                        answer = answer?.value,
                        note = answer?.note,
                        evidenceIds = answer?.evidenceIds.orEmpty(),
                    )
                },
            )
        }

fun ChecklistAnswerValue.toReportLabel(): String = when (this) {
    ChecklistAnswerValue.Pass -> "Pass"
    ChecklistAnswerValue.Fail -> "Fail"
    ChecklistAnswerValue.NotApplicable -> "Not applicable"
    is ChecklistAnswerValue.YesNo -> if (value) "Yes" else "No"
    is ChecklistAnswerValue.Text -> value
    is ChecklistAnswerValue.NumberValue -> buildString {
        append(value)
        if (!unit.isNullOrBlank()) {
            append(' ')
            append(unit)
        }
    }
    is ChecklistAnswerValue.SingleChoice -> optionId
}
