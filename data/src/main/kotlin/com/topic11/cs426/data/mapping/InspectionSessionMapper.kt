package com.topic11.cs426.data.mapping

import com.topic11.cs426.core.database.dao.InspectionDraftRecord
import com.topic11.cs426.core.database.entity.InspectionAnswerEntity
import com.topic11.cs426.core.database.entity.InspectionEntity
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.ChecklistAnswerValue
import com.topic11.cs426.domain.model.ChecklistItemId
import com.topic11.cs426.domain.model.EvidenceId
import com.topic11.cs426.domain.model.InspectionAnswer
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionScore
import com.topic11.cs426.domain.model.InspectionSession
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.SectionId
import com.topic11.cs426.domain.model.TemplateId

fun InspectionDraftRecord.toDomain(): InspectionSession {
    val evidenceByItem = evidence
        .filter { it.checklistItemId != null }
        .groupBy { requireNotNull(it.checklistItemId) }
    return InspectionSession(
        id = InspectionId(inspection.id),
        assetId = AssetId(inspection.assetId),
        assetName = assetName,
        templateId = TemplateId(inspection.templateRevisionId),
        status = inspection.toDomainStatus(),
        currentSectionId = inspection.currentSectionId?.let(::SectionId),
        answers = answers.map { answer ->
            answer.toDomain(
                evidenceIds = evidenceByItem[answer.checklistItemId]
                    .orEmpty()
                    .map { EvidenceId(it.id) },
            )
        },
        startedAtMillis = inspection.startedAtMillis,
        updatedAtMillis = inspection.updatedAtMillis,
        completedAtMillis = inspection.completedAtMillis,
        score = inspection.toDomainScore(),
    )
}

fun InspectionSession.toEntity(existing: InspectionEntity): InspectionEntity = existing.copy(
    lifecycleStatus = status.toLifecycleValue(),
    currentSectionId = currentSectionId?.value,
    startedAtMillis = startedAtMillis,
    updatedAtMillis = updatedAtMillis,
    completedAtMillis = completedAtMillis,
    earnedWeight = score?.earnedWeight?.toDouble(),
    totalWeight = score?.totalWeight?.toDouble(),
)

fun InspectionAnswer.toEntity(answerType: String) = InspectionAnswerEntity(
    inspectionId = inspectionId.value,
    checklistItemId = checklistItemId.value,
    answerType = answerType,
    valueText = when (val answer = value) {
        ChecklistAnswerValue.Pass -> "PASS"
        ChecklistAnswerValue.Fail -> "FAIL"
        ChecklistAnswerValue.NotApplicable -> "NOT_APPLICABLE"
        is ChecklistAnswerValue.Text -> answer.value
        is ChecklistAnswerValue.SingleChoice -> answer.optionId
        else -> null
    },
    valueNumber = (value as? ChecklistAnswerValue.NumberValue)?.value,
    valueBoolean = (value as? ChecklistAnswerValue.YesNo)?.value,
    unit = (value as? ChecklistAnswerValue.NumberValue)?.unit,
    note = note,
    updatedAtMillis = updatedAtMillis,
)

private fun InspectionAnswerEntity.toDomain(evidenceIds: List<EvidenceId>) = InspectionAnswer(
    inspectionId = InspectionId(inspectionId),
    checklistItemId = ChecklistItemId(checklistItemId),
    value = when (answerType) {
        "PASS_FAIL_NA" -> when (valueText) {
            null -> null
            "PASS" -> ChecklistAnswerValue.Pass
            "FAIL" -> ChecklistAnswerValue.Fail
            "NOT_APPLICABLE" -> ChecklistAnswerValue.NotApplicable
            else -> mappingError("Unknown pass/fail value for $checklistItemId: $valueText")
        }

        "YES_NO" -> valueBoolean?.let(ChecklistAnswerValue::YesNo)
        "TEXT" -> valueText?.let(ChecklistAnswerValue::Text)
        "NUMBER" -> valueNumber?.let { ChecklistAnswerValue.NumberValue(it, unit) }
        "SINGLE_CHOICE" -> valueText?.let(ChecklistAnswerValue::SingleChoice)
        else -> mappingError("Unknown answer type for $checklistItemId: $answerType")
    },
    note = note,
    evidenceIds = evidenceIds,
    updatedAtMillis = updatedAtMillis,
)

private fun InspectionEntity.toDomainStatus(): InspectionStatus = when (lifecycleStatus) {
    "NOT_STARTED" -> InspectionStatus.NOT_STARTED
    "IN_PROGRESS" -> InspectionStatus.IN_PROGRESS
    "REVIEWING" -> InspectionStatus.REVIEWING
    "COMPLETED" -> when (syncStatus) {
        "PENDING", "SYNCING", "FAILED" -> InspectionStatus.SYNC_PENDING
        "NOT_REQUIRED", "SYNCED" -> InspectionStatus.COMPLETED
        else -> mappingError("Unknown sync status for $id: $syncStatus")
    }

    else -> mappingError("Unknown lifecycle status for $id: $lifecycleStatus")
}

private fun InspectionStatus.toLifecycleValue(): String = when (this) {
    InspectionStatus.NOT_STARTED -> "NOT_STARTED"
    InspectionStatus.IN_PROGRESS -> "IN_PROGRESS"
    InspectionStatus.REVIEWING -> "REVIEWING"
    InspectionStatus.COMPLETED,
    InspectionStatus.SYNC_PENDING,
    -> "COMPLETED"
}

private fun InspectionEntity.toDomainScore(): InspectionScore? {
    if (earnedWeight == null && totalWeight == null) return null
    val earned = earnedWeight
        ?: mappingError("Inspection $id has total weight without earned weight")
    val total = totalWeight
        ?: mappingError("Inspection $id has earned weight without total weight")
    return InspectionScore(
        earnedWeight = earned.toExactInt("Earned weight", id),
        totalWeight = total.toExactInt("Total weight", id),
    )
}

private fun mappingError(message: String): Nothing {
    throw PersistenceMappingException(message)
}
