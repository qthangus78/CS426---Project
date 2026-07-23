package com.topic11.cs426.core.database.dao

import androidx.room.Dao
import androidx.room.Query
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

    @Query("SELECT * FROM assets ORDER BY name, id")
    fun observeAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM inspection_templates ORDER BY name, version DESC")
    fun observeTemplates(): Flow<List<InspectionTemplateEntity>>

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
}
