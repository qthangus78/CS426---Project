package com.topic11.cs426.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.topic11.cs426.core.database.entity.MaintenanceIssueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IssueDao {
    @Query("SELECT * FROM maintenance_issues ORDER BY updated_at_ms DESC, id")
    fun observeIssues(): Flow<List<MaintenanceIssueEntity>>

    @Query(
        """
        SELECT * FROM maintenance_issues
        WHERE inspection_id = :inspectionId
        ORDER BY created_at_ms, id
        """,
    )
    fun observeIssuesForInspection(inspectionId: String): Flow<List<MaintenanceIssueEntity>>

    @Query("SELECT * FROM maintenance_issues WHERE id = :issueId")
    fun observeIssue(issueId: String): Flow<MaintenanceIssueEntity?>

    @Query("SELECT * FROM maintenance_issues WHERE id = :issueId")
    suspend fun getIssue(issueId: String): MaintenanceIssueEntity?

    @Query(
        """
        SELECT * FROM maintenance_issues
        WHERE inspection_id = :inspectionId
        ORDER BY created_at_ms, id
        """,
    )
    suspend fun getIssuesForInspection(inspectionId: String): List<MaintenanceIssueEntity>

    @Upsert
    suspend fun upsertIssue(issue: MaintenanceIssueEntity)
}
