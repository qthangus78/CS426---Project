package com.topic11.cs426.core.database.dao

import androidx.room.Embedded
import androidx.room.Relation
import com.topic11.cs426.core.database.entity.AssetEntity
import com.topic11.cs426.core.database.entity.ChecklistItemEntity
import com.topic11.cs426.core.database.entity.InspectionSectionEntity
import com.topic11.cs426.core.database.entity.InspectionTemplateEntity

data class AssetSummaryRecord(
    @Embedded val asset: AssetEntity,
    val locationName: String?,
)

data class TemplateAggregateRecord(
    @Embedded val template: InspectionTemplateEntity,
    @Relation(
        entity = InspectionSectionEntity::class,
        parentColumn = "revision_id",
        entityColumn = "template_revision_id",
    )
    val sections: List<SectionAggregateRecord>,
)

data class SectionAggregateRecord(
    @Embedded val section: InspectionSectionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "section_id",
    )
    val items: List<ChecklistItemEntity>,
)
