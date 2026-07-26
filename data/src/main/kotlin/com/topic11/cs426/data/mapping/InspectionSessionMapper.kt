package com.topic11.cs426.data.mapping

import com.topic11.cs426.core.database.dao.InspectionSessionRecord
import com.topic11.cs426.core.database.entity.EvidenceEntity
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

fun InspectionSessionRecord.toDomain(
    answers: List<InspectionAnswerEntity>,
    evidence: List<EvidenceEntity>,
): InspectionSession {
    requireSessionValue(inspectionId.isNotBlank(), "Inspection ID cannot be blank")
    requireSessionValue(assetId.isNotBlank(), "Asset ID cannot be blank for $inspectionId")
    requireSessionValue(assetName.isNotBlank(), "Asset name cannot be blank for $inspectionId")
    requireSessionValue(templateId.isNotBlank(), "Template ID cannot be blank for $inspectionId")
    requireSessionValue(templateName.isNotBlank(), "Template name cannot be blank for $inspectionId")

    val evidenceIdsByItem = evidence
        .mapNotNull { record ->
            record.checklistItemId?.let { checklistItemId ->
                checklistItemId to EvidenceId(record.id)
            }
        }
        .groupBy(
            keySelector = { (checklistItemId, _) -> checklistItemId },
            valueTransform = { (_, evidenceId) -> evidenceId },
        )

    return InspectionSession(
        id = InspectionId(inspectionId),
        assetId = AssetId(assetId),
        assetName = assetName,
        templateId = TemplateId(templateId),
        templateName = templateName,
        status = toDomainSessionStatus(lifecycleStatus, syncStatus),
        currentSectionId = currentSectionId?.let(::SectionId),
        answers = answers.map { answer ->
            answer.toDomain(evidenceIdsByItem[answer.checklistItemId].orEmpty())
        },
        startedAtMillis = startedAtMillis,
        updatedAtMillis = updatedAtMillis,
        completedAtMillis = completedAtMillis,
        score = toDomainScore(),
    )
}

fun InspectionSession.toEntity(existing: InspectionEntity): InspectionEntity =
    existing.copy(
        lifecycleStatus = status.toLifecycleValue(),
        syncStatus = status.toSyncValue(existing.syncStatus),
        currentSectionId = currentSectionId?.value,
        startedAtMillis = startedAtMillis,
        updatedAtMillis = updatedAtMillis,
        completedAtMillis = completedAtMillis,
        earnedWeight = score?.earnedWeight?.toDouble(),
        totalWeight = score?.totalWeight?.toDouble(),
    )

fun InspectionAnswer.toEntity(answerType: String): InspectionAnswerEntity =
    InspectionAnswerEntity(
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

private fun InspectionSessionRecord.toDomainScore(): InspectionScore? {
    if (earnedWeight == null && totalWeight == null) return null
    val earned = earnedWeight ?: mappingError("Inspection $inspectionId has total weight without earned weight")
    val total = totalWeight ?: mappingError("Inspection $inspectionId has earned weight without total weight")
    return InspectionScore(
        earnedWeight = earned.toExactInt("Earned weight", inspectionId),
        totalWeight = total.toExactInt("Total weight", inspectionId),
    )
}

private fun InspectionAnswerEntity.toDomain(evidenceIds: List<EvidenceId>): InspectionAnswer {
    requireSessionValue(inspectionId.isNotBlank(), "Answer inspection ID cannot be blank")
    requireSessionValue(checklistItemId.isNotBlank(), "Checklist item ID cannot be blank")
    return InspectionAnswer(
        inspectionId = InspectionId(inspectionId),
        checklistItemId = ChecklistItemId(checklistItemId),
        value = toDomainValue(),
        note = note,
        evidenceIds = evidenceIds,
        updatedAtMillis = updatedAtMillis,
    )
}

private fun InspectionAnswerEntity.toDomainValue(): ChecklistAnswerValue? {
    return when (answerType) {
        "PASS_FAIL_NA" -> when (valueText) {
            null -> null
            "PASS" -> ChecklistAnswerValue.Pass
            "FAIL" -> ChecklistAnswerValue.Fail
            "NOT_APPLICABLE", "NA" -> ChecklistAnswerValue.NotApplicable
            else -> mappingError("Unknown pass/fail value for $checklistItemId: $valueText")
        }

        "YES_NO" -> valueBoolean?.let(ChecklistAnswerValue::YesNo)
        "TEXT" -> valueText?.let(ChecklistAnswerValue::Text)
        "NUMBER" -> valueNumber?.let { ChecklistAnswerValue.NumberValue(value = it, unit = unit) }
        "SINGLE_CHOICE" -> valueText?.let(ChecklistAnswerValue::SingleChoice)
        else -> mappingError("Unknown answer type for $checklistItemId: $answerType")
    }
}

private fun toDomainSessionStatus(
    lifecycleStatus: String,
    syncStatus: String,
): InspectionStatus {
    return when (lifecycleStatus) {
        "NOT_STARTED" -> InspectionStatus.NOT_STARTED
        "IN_PROGRESS" -> InspectionStatus.IN_PROGRESS
        "REVIEWING" -> InspectionStatus.REVIEWING
        "COMPLETED" -> when (syncStatus) {
            "PENDING", "SYNCING", "FAILED" -> InspectionStatus.SYNC_PENDING
            "NOT_REQUIRED", "SYNCED" -> InspectionStatus.COMPLETED
            else -> mappingError("Unknown sync status: $syncStatus")
        }

        else -> mappingError("Unknown inspection lifecycle status: $lifecycleStatus")
    }
}

private fun InspectionStatus.toLifecycleValue(): String = when (this) {
    InspectionStatus.NOT_STARTED -> "NOT_STARTED"
    InspectionStatus.IN_PROGRESS -> "IN_PROGRESS"
    InspectionStatus.REVIEWING -> "REVIEWING"
    InspectionStatus.COMPLETED,
    InspectionStatus.SYNC_PENDING,
    -> "COMPLETED"
}

private fun InspectionStatus.toSyncValue(existingSyncStatus: String): String = when (this) {
    InspectionStatus.NOT_STARTED,
    InspectionStatus.IN_PROGRESS,
    InspectionStatus.REVIEWING,
    -> "NOT_REQUIRED"

    InspectionStatus.COMPLETED -> when (existingSyncStatus) {
        "PENDING", "SYNCING", "FAILED" -> existingSyncStatus
        else -> "SYNCED"
    }

    InspectionStatus.SYNC_PENDING -> "PENDING"
}

private fun requireSessionValue(condition: Boolean, message: String) {
    if (!condition) mappingError(message)
}

private fun mappingError(message: String): Nothing {
    throw PersistenceMappingException(message)
}
