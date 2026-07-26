package com.topic11.cs426.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.topic11.cs426.core.database.entity.AssetEntity
import com.topic11.cs426.core.database.entity.ChecklistItemEntity
import com.topic11.cs426.core.database.entity.InspectionSectionEntity
import com.topic11.cs426.core.database.entity.InspectionTemplateEntity
import com.topic11.cs426.core.database.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Upsert
    suspend fun upsertLocations(locations: List<LocationEntity>)

    @Upsert
    suspend fun upsertAssets(assets: List<AssetEntity>)

    @Upsert
    suspend fun upsertTemplates(templates: List<InspectionTemplateEntity>)

    @Upsert
    suspend fun upsertSections(sections: List<InspectionSectionEntity>)

    @Upsert
    suspend fun upsertChecklistItems(items: List<ChecklistItemEntity>)

    @Query("SELECT * FROM locations ORDER BY name, id")
    fun observeLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE id = :locationId")
    suspend fun getLocation(locationId: String): LocationEntity?

    @Query("SELECT * FROM assets ORDER BY name, id")
    fun observeAssets(): Flow<List<AssetEntity>>

    @Query(
        """
        SELECT assets.*, locations.name AS locationName
        FROM assets
        LEFT JOIN locations ON locations.id = assets.location_id
        ORDER BY assets.name, assets.id
        """,
    )
    fun observeAssetSummaries(): Flow<List<AssetSummaryRecord>>

    @Query("SELECT * FROM assets WHERE id = :assetId")
    suspend fun getAsset(assetId: String): AssetEntity?

    @Query("SELECT * FROM assets WHERE code = :code LIMIT 1")
    suspend fun getAssetByCode(code: String): AssetEntity?

    @Query("SELECT * FROM inspection_templates ORDER BY name, version DESC")
    fun observeTemplates(): Flow<List<InspectionTemplateEntity>>

    @Transaction
    @Query("SELECT * FROM inspection_templates ORDER BY name, version DESC")
    fun observeTemplateAggregates(): Flow<List<TemplateAggregateRecord>>

    @Transaction
    @Query(
        """
        SELECT * FROM inspection_templates
        WHERE revision_id = :id OR template_id = :id
        ORDER BY CASE WHEN revision_id = :id THEN 0 ELSE 1 END, version DESC
        LIMIT 1
        """,
    )
    fun observeTemplateAggregate(id: String): Flow<TemplateAggregateRecord?>

    @Transaction
    @Query(
        """
        SELECT * FROM inspection_templates
        WHERE revision_id = :id OR template_id = :id
        ORDER BY CASE WHEN revision_id = :id THEN 0 ELSE 1 END, version DESC
        LIMIT 1
        """,
    )
    suspend fun getTemplateAggregate(id: String): TemplateAggregateRecord?

    @Query(
        """
        SELECT * FROM inspection_sections
        WHERE template_revision_id = :templateRevisionId
        ORDER BY position, id
        """,
    )
    suspend fun getSections(templateRevisionId: String): List<InspectionSectionEntity>

    @Query(
        """
        SELECT checklist_items.* FROM checklist_items
        INNER JOIN inspection_sections
            ON inspection_sections.id = checklist_items.section_id
        WHERE inspection_sections.template_revision_id = :templateRevisionId
        ORDER BY inspection_sections.position, checklist_items.position, checklist_items.id
        """,
    )
    suspend fun getChecklistItems(templateRevisionId: String): List<ChecklistItemEntity>

    @Transaction
    suspend fun upsertTemplateAggregate(
        template: InspectionTemplateEntity,
        sections: List<InspectionSectionEntity>,
        items: List<ChecklistItemEntity>,
    ) {
        upsertTemplates(listOf(template))
        upsertSections(sections)
        upsertChecklistItems(items)
    }
}
