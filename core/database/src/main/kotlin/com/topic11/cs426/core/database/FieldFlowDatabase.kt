package com.topic11.cs426.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.topic11.cs426.core.database.dao.CatalogDao
import com.topic11.cs426.core.database.dao.InspectionDao
import com.topic11.cs426.core.database.dao.IssueDao
import com.topic11.cs426.core.database.dao.SyncDao
import com.topic11.cs426.core.database.entity.AssetEntity
import com.topic11.cs426.core.database.entity.ChecklistItemEntity
import com.topic11.cs426.core.database.entity.EvidenceEntity
import com.topic11.cs426.core.database.entity.InspectionAnswerEntity
import com.topic11.cs426.core.database.entity.InspectionEntity
import com.topic11.cs426.core.database.entity.InspectionSectionEntity
import com.topic11.cs426.core.database.entity.InspectionTemplateEntity
import com.topic11.cs426.core.database.entity.LocationEntity
import com.topic11.cs426.core.database.entity.MaintenanceIssueEntity
import com.topic11.cs426.core.database.entity.PendingSyncEntity

@Database(
    entities = [
        LocationEntity::class,
        AssetEntity::class,
        InspectionTemplateEntity::class,
        InspectionSectionEntity::class,
        ChecklistItemEntity::class,
        InspectionEntity::class,
        InspectionAnswerEntity::class,
        EvidenceEntity::class,
        MaintenanceIssueEntity::class,
        PendingSyncEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class FieldFlowDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao

    abstract fun inspectionDao(): InspectionDao

    abstract fun issueDao(): IssueDao

    abstract fun syncDao(): SyncDao

    companion object {
        const val DATABASE_NAME = "fieldflow.db"
    }
}
