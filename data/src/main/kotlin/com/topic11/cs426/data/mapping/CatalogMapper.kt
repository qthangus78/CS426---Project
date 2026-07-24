package com.topic11.cs426.data.mapping

import com.topic11.cs426.core.database.dao.AssetSummaryRecord
import com.topic11.cs426.core.database.dao.SectionAggregateRecord
import com.topic11.cs426.core.database.dao.TemplateAggregateRecord
import com.topic11.cs426.core.database.entity.AssetEntity
import com.topic11.cs426.core.database.entity.ChecklistItemEntity
import com.topic11.cs426.domain.model.Asset
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.AssetSummary
import com.topic11.cs426.domain.model.ChecklistAnswerType
import com.topic11.cs426.domain.model.ChecklistItem
import com.topic11.cs426.domain.model.ChecklistItemId
import com.topic11.cs426.domain.model.InspectionSection
import com.topic11.cs426.domain.model.InspectionTemplate
import com.topic11.cs426.domain.model.InspectionTemplateSummary
import com.topic11.cs426.domain.model.LocationId
import com.topic11.cs426.domain.model.SectionId
import com.topic11.cs426.domain.model.TemplateId

fun AssetSummaryRecord.toDomain() = AssetSummary(
    id = AssetId(asset.id),
    name = asset.name,
    code = asset.code,
    locationName = locationName,
    nextInspectionDueAtMillis = asset.nextInspectionDueAtMillis,
)

fun AssetEntity.toDomain() = Asset(
    id = AssetId(id),
    name = name,
    code = code,
    locationId = LocationId(locationId),
    nextInspectionDueAtMillis = nextInspectionDueAtMillis,
)

fun TemplateAggregateRecord.toDomain(): InspectionTemplate {
    // Domain currently has no revision ID. Treat the stable Room revision key as TemplateId so
    // an existing inspection can never be remapped to a newer checklist revision.
    val revisionId = TemplateId(template.revisionId)
    return InspectionTemplate(
        id = revisionId,
        name = template.name,
        version = template.version,
        sections = sections
            .sortedWith(compareBy({ it.section.position }, { it.section.id }))
            .map { it.toDomain(revisionId) },
        recurrencePolicyDays = template.recurrenceIntervalDays,
    )
}

fun TemplateAggregateRecord.toSummary() = InspectionTemplateSummary(
    id = TemplateId(template.revisionId),
    name = template.name,
    version = template.version,
    sectionCount = sections.size,
)

private fun SectionAggregateRecord.toDomain(templateId: TemplateId) = InspectionSection(
    id = SectionId(section.id),
    templateId = templateId,
    title = section.title,
    order = section.position,
    items = items
        .sortedWith(compareBy({ it.position }, { it.id }))
        .map { it.toDomain() },
)

private fun ChecklistItemEntity.toDomain() = ChecklistItem(
    id = ChecklistItemId(id),
    sectionId = SectionId(sectionId),
    title = title,
    description = description,
    required = isRequired,
    critical = isCritical,
    weight = weight.toExactInt("Checklist item weight", id),
    answerType = try {
        ChecklistAnswerType.valueOf(answerType)
    } catch (failure: IllegalArgumentException) {
        throw PersistenceMappingException("Unknown answer type for $id: $answerType")
    },
)

internal fun Double.toExactInt(label: String, id: String): Int {
    if (!isFinite() || this % 1.0 != 0.0 || this < Int.MIN_VALUE || this > Int.MAX_VALUE) {
        throw PersistenceMappingException("$label for $id is not an integer: $this")
    }
    return toInt()
}
