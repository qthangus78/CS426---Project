package com.topic11.cs426.data.mapping

import com.topic11.cs426.core.database.dao.InspectionSessionRecord
import com.topic11.cs426.core.database.entity.EvidenceEntity
import com.topic11.cs426.core.database.entity.InspectionAnswerEntity
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.ChecklistAnswerValue
import com.topic11.cs426.domain.model.ChecklistItemId
import com.topic11.cs426.domain.model.EvidenceId
import com.topic11.cs426.domain.model.InspectionAnswer
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionScore
import com.topic11.cs426.domain.model.InspectionSession
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
        .filter { it.checklistItemId != null }
        .groupBy { it.checklistItemId!! }
        .mapValues { (_, records) -> records.map { EvidenceId(it.id) } }

    return InspectionSession(
        id = InspectionId(inspectionId),
        assetId = AssetId(assetId),
        assetName = assetName,
        templateId = TemplateId(templateId),
        templateName = templateName,
        status = toDomainInspectionStatus(lifecycleStatus, syncStatus),
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

fun InspectionAnswer.toEntity(): InspectionAnswerEntity {
    val persistedValue = value.toPersistedValue()
    return InspectionAnswerEntity(
        inspectionId = inspectionId.value,
        checklistItemId = checklistItemId.value,
        answerType = persistedValue.answerType,
        valueText = persistedValue.valueText,
        valueNumber = persistedValue.valueNumber,
        valueBoolean = persistedValue.valueBoolean,
        unit = persistedValue.unit,
        note = note,
        updatedAtMillis = updatedAtMillis,
    )
}

private fun InspectionSessionRecord.toDomainScore(): InspectionScore? {
    if (earnedWeight == null && totalWeight == null) return null
    requireSessionValue(earnedWeight != null && totalWeight != null, "Incomplete score for $inspectionId")
    return InspectionScore(
        earnedWeight = earnedWeight.toExactInt("earned weight"),
        totalWeight = totalWeight.toExactInt("total weight"),
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
    if (valueText == null && valueNumber == null && valueBoolean == null) return null

    return when (answerType) {
        "PASS_FAIL_NA" -> when (valueText) {
            "PASS" -> ChecklistAnswerValue.Pass
            "FAIL" -> ChecklistAnswerValue.Fail
            "NOT_APPLICABLE", "NA" -> ChecklistAnswerValue.NotApplicable
            else -> invalidAnswer("Unknown pass/fail value: $valueText")
        }

        "YES_NO" -> ChecklistAnswerValue.YesNo(
            valueBoolean ?: invalidAnswer("YES_NO answer is missing a boolean value"),
        )

        "TEXT" -> ChecklistAnswerValue.Text(
            valueText ?: invalidAnswer("TEXT answer is missing text"),
        )

        "NUMBER" -> ChecklistAnswerValue.NumberValue(
            value = valueNumber ?: invalidAnswer("NUMBER answer is missing a numeric value"),
            unit = unit,
        )

        "SINGLE_CHOICE" -> ChecklistAnswerValue.SingleChoice(
            valueText ?: invalidAnswer("SINGLE_CHOICE answer is missing an option"),
        )

        "UNANSWERED" -> null
        else -> invalidAnswer("Unknown answer type: $answerType")
    }
}

private fun ChecklistAnswerValue?.toPersistedValue(): PersistedAnswerValue = when (this) {
    null -> PersistedAnswerValue(answerType = "UNANSWERED")
    ChecklistAnswerValue.Pass -> PersistedAnswerValue("PASS_FAIL_NA", valueText = "PASS")
    ChecklistAnswerValue.Fail -> PersistedAnswerValue("PASS_FAIL_NA", valueText = "FAIL")
    ChecklistAnswerValue.NotApplicable -> {
        PersistedAnswerValue("PASS_FAIL_NA", valueText = "NOT_APPLICABLE")
    }

    is ChecklistAnswerValue.YesNo -> PersistedAnswerValue("YES_NO", valueBoolean = value)
    is ChecklistAnswerValue.Text -> PersistedAnswerValue("TEXT", valueText = value)
    is ChecklistAnswerValue.NumberValue -> {
        PersistedAnswerValue("NUMBER", valueNumber = value, unit = unit)
    }

    is ChecklistAnswerValue.SingleChoice -> {
        PersistedAnswerValue("SINGLE_CHOICE", valueText = optionId)
    }
}

private data class PersistedAnswerValue(
    val answerType: String,
    val valueText: String? = null,
    val valueNumber: Double? = null,
    val valueBoolean: Boolean? = null,
    val unit: String? = null,
)

private fun Double?.toExactInt(label: String): Int {
    val value = this ?: invalidSession("Missing $label")
    requireSessionValue(value.isFinite() && value == value.toInt().toDouble(), "Invalid $label: $value")
    return value.toInt()
}

private fun requireSessionValue(condition: Boolean, message: String) {
    if (!condition) invalidSession(message)
}

private fun invalidAnswer(message: String): Nothing = invalidSession(message)

private fun invalidSession(message: String): Nothing {
    throw PersistenceMappingException(message)
}
