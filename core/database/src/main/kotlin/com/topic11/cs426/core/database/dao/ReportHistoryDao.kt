package com.topic11.cs426.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.topic11.cs426.core.database.entity.ReportExportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportHistoryDao {
    @Query(
        """
        SELECT * FROM report_exports
        ORDER BY generated_at_ms DESC, id
        """,
    )
    fun observeExports(): Flow<List<ReportExportEntity>>

    @Query("SELECT * FROM report_exports WHERE id = :exportId")
    suspend fun getExport(exportId: String): ReportExportEntity?

    @Upsert
    suspend fun upsertExport(export: ReportExportEntity)
}
