package com.topic11.cs426.core.testing

import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.ChecklistAnswerType
import com.topic11.cs426.domain.model.ChecklistAnswerValue
import com.topic11.cs426.domain.model.ChecklistItem
import com.topic11.cs426.domain.model.ChecklistItemId
import com.topic11.cs426.domain.model.EvidenceId
import com.topic11.cs426.domain.model.InspectionAnswer
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionSection
import com.topic11.cs426.domain.model.InspectionSession
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.InspectionSummary
import com.topic11.cs426.domain.model.InspectionTemplate
import com.topic11.cs426.domain.model.SectionId
import com.topic11.cs426.domain.model.TemplateId

object InspectionTestFixtures {
    // ── ID ──
    val templateId = TemplateId("template-lab-safety")
    val section1Id = SectionId("section-general")
    val section2Id = SectionId("section-electrical")
    val itemRequiredId = ChecklistItemId("item-fire-extinguisher")
    val itemCriticalId = ChecklistItemId("item-emergency-exit")
    val itemOptionalId = ChecklistItemId("item-lighting")
    val evidenceId = EvidenceId("evidence-photo-1")
    val asset1Id = AssetId("asset-lab-i-44")

    // ── InspectionSummary fixtures ──
    val computerLab = InspectionSummary(
        id = InspectionId("computer-lab-i-44"),
        title = "Computer Lab I.44",
        status = InspectionStatus.IN_PROGRESS,
        completedItems = 6,
        totalItems = 10,
    )

    val projector = InspectionSummary(
        id = InspectionId("projector-p-204"),
        title = "Projector P-204",
        status = InspectionStatus.NOT_STARTED,
        completedItems = 0,
        totalItems = 8,
    )

    val laboratorySafetyCheck = InspectionSummary(
        id = InspectionId("laboratory-a2-safety-check"),
        title = "Laboratory A2 Safety Check",
        status = InspectionStatus.SYNC_PENDING,
        completedItems = 12,
        totalItems = 12,
    )

    val inspectionSummaries = listOf(computerLab, projector, laboratorySafetyCheck)

    // ── Template fixtures ──
    val sampleChecklistItems = listOf(
        ChecklistItem(
            id = itemRequiredId,
            sectionId = section1Id,
            title = "Fire extinguisher present",
            required = true,
            critical = true,
            weight = 5,
            answerType = ChecklistAnswerType.PASS_FAIL_NA,
        ),
        ChecklistItem(
            id = itemCriticalId,
            sectionId = section1Id,
            title = "Emergency exit clear",
            required = true,
            critical = true,
            weight = 5,
            answerType = ChecklistAnswerType.PASS_FAIL_NA,
        ),
        ChecklistItem(
            id = itemOptionalId,
            sectionId = section2Id,
            title = "Lighting adequate",
            required = false,
            critical = false,
            weight = 1,
            answerType = ChecklistAnswerType.PASS_FAIL_NA,
        ),
    )

    val sampleSections = listOf(
        InspectionSection(
            id = section1Id,
            templateId = templateId,
            title = "General Safety",
            order = 0,
            items = listOf(sampleChecklistItems[0], sampleChecklistItems[1]),
        ),
        InspectionSection(
            id = section2Id,
            templateId = templateId,
            title = "Electrical",
            order = 1,
            items = listOf(sampleChecklistItems[2]),
        ),
    )

    val sampleTemplate = InspectionTemplate(
        id = templateId,
        name = "Lab Safety Checklist",
        version = 1,
        sections = sampleSections,
        recurrencePolicyDays = 365,
    )

    val templateWithNoRecurrence = sampleTemplate.copy(
        id = TemplateId("template-no-recurrence"),
        recurrencePolicyDays = null,
    )

    // ── Session fixtures ──
    fun createSampleSession(
        id: InspectionId = computerLab.id,
        assetId: AssetId = asset1Id,
        assetName: String = "Computer Lab I.44",
        templateId: TemplateId = InspectionTestFixtures.templateId,
        status: InspectionStatus = InspectionStatus.IN_PROGRESS,
        answers: List<InspectionAnswer> = emptyList(),
    ): InspectionSession {
        return InspectionSession(
            id = id,
            assetId = assetId,
            assetName = assetName,
            templateId = templateId,
            status = status,
            currentSectionId = section1Id,
            answers = answers,
            startedAtMillis = 1000L,
            updatedAtMillis = 2000L,
            completedAtMillis = if (status == InspectionStatus.COMPLETED) 3000L else null,
        )
    }

    fun createAnswer(
        itemId: ChecklistItemId = itemRequiredId,
        value: ChecklistAnswerValue? = ChecklistAnswerValue.Pass,
        evidenceIds: List<EvidenceId> = emptyList(),
        inspectionId: InspectionId = computerLab.id,
    ): InspectionAnswer {
        return InspectionAnswer(
            inspectionId = inspectionId,
            checklistItemId = itemId,
            value = value,
            evidenceIds = evidenceIds,
            updatedAtMillis = 2000L,
        )
    }
}
