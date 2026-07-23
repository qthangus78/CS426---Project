package com.topic11.cs426.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.topic11.cs426.core.database.entity.EvidenceEntity
import com.topic11.cs426.core.database.entity.InspectionAnswerEntity
import com.topic11.cs426.core.database.entity.InspectionEntity
import com.topic11.cs426.core.database.entity.MaintenanceIssueEntity
import com.topic11.cs426.core.database.entity.PendingSyncEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionDao {
    @Query("SELECT * FROM inspections ORDER BY updated_at_ms DESC, id")
    fun observeInspections(): Flow<List<InspectionEntity>>

    @Query("SELECT * FROM inspections WHERE id = :inspectionId")
    fun observeInspection(inspectionId: String): Flow<InspectionEntity?>

    @Query("SELECT * FROM inspections WHERE id = :inspectionId")
    suspend fun getInspection(inspectionId: String): InspectionEntity?

    @Query(
        """
        SELECT * FROM inspection_answers
        WHERE inspection_id = :inspectionId
        ORDER BY checklist_item_id
        """,
    )
    fun observeAnswers(inspectionId: String): Flow<List<InspectionAnswerEntity>>

    @Query(
        """
        SELECT * FROM evidence
        WHERE inspection_id = :inspectionId
        ORDER BY created_at_ms, id
        """,
    )
    fun observeEvidence(inspectionId: String): Flow<List<EvidenceEntity>>

    @Upsert
    suspend fun upsertInspection(inspection: InspectionEntity)

    @Upsert
    suspend fun upsertAnswers(answers: List<InspectionAnswerEntity>)

    @Upsert
    suspend fun upsertEvidence(evidence: List<EvidenceEntity>)

    @Upsert
    suspend fun upsertIssues(issues: List<MaintenanceIssueEntity>)

    @Upsert
    suspend fun upsertPendingSync(commands: List<PendingSyncEntity>)

    @Transaction
    suspend fun saveDraft(
        inspection: InspectionEntity,
        answers: List<InspectionAnswerEntity>,
        evidence: List<EvidenceEntity>,
    ) {
        upsertInspection(inspection)
        upsertAnswers(answers)
        upsertEvidence(evidence)
    }

    @Transaction
    suspend fun completeInspection(
        inspection: InspectionEntity,
        answers: List<InspectionAnswerEntity>,
        evidence: List<EvidenceEntity>,
        issues: List<MaintenanceIssueEntity>,
        pendingSync: List<PendingSyncEntity>,
    ) {
        upsertInspection(inspection)
        upsertAnswers(answers)
        upsertEvidence(evidence)
        upsertIssues(issues)
        upsertPendingSync(pendingSync)
    }
}
