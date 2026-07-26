package com.topic11.cs426.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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
    @Query("SELECT COUNT(*) FROM inspections")
    suspend fun getInspectionCount(): Int

    @Query(
        """
        SELECT
            inspections.id AS inspectionId,
            assets.name AS title,
            inspections.lifecycle_status AS lifecycleStatus,
            inspections.sync_status AS syncStatus,
            COUNT(
                DISTINCT CASE
                    WHEN inspection_answers.answer_type != 'UNANSWERED'
                    THEN inspection_answers.checklist_item_id
                END
            ) AS completedItems,
            COUNT(DISTINCT checklist_items.id) AS totalItems
        FROM inspections
        INNER JOIN assets ON assets.id = inspections.asset_id
        LEFT JOIN inspection_sections
            ON inspection_sections.template_revision_id = inspections.template_revision_id
        LEFT JOIN checklist_items
            ON checklist_items.section_id = inspection_sections.id
        LEFT JOIN inspection_answers
            ON inspection_answers.inspection_id = inspections.id
            AND inspection_answers.checklist_item_id = checklist_items.id
        GROUP BY
            inspections.id,
            assets.name,
            inspections.lifecycle_status,
            inspections.sync_status,
            inspections.updated_at_ms
        ORDER BY inspections.updated_at_ms DESC, inspections.id
        """,
    )
    fun observeInspectionSummaries(): Flow<List<InspectionSummaryRecord>>

    @Query(
        """
        SELECT
            inspections.id AS inspectionId,
            assets.name AS title,
            inspections.lifecycle_status AS lifecycleStatus,
            inspections.sync_status AS syncStatus,
            COUNT(
                DISTINCT CASE
                    WHEN inspection_answers.answer_type != 'UNANSWERED'
                    THEN inspection_answers.checklist_item_id
                END
            ) AS completedItems,
            COUNT(DISTINCT checklist_items.id) AS totalItems
        FROM inspections
        INNER JOIN assets ON assets.id = inspections.asset_id
        LEFT JOIN inspection_sections
            ON inspection_sections.template_revision_id = inspections.template_revision_id
        LEFT JOIN checklist_items
            ON checklist_items.section_id = inspection_sections.id
        LEFT JOIN inspection_answers
            ON inspection_answers.inspection_id = inspections.id
            AND inspection_answers.checklist_item_id = checklist_items.id
        WHERE inspections.id = :inspectionId
        GROUP BY
            inspections.id,
            assets.name,
            inspections.lifecycle_status,
            inspections.sync_status
        """,
    )
    fun observeInspectionSummary(inspectionId: String): Flow<InspectionSummaryRecord?>

    @Query(
        """
        SELECT
            inspections.id AS inspectionId,
            inspections.asset_id AS assetId,
            assets.name AS assetName,
            inspection_templates.revision_id AS templateId,
            inspection_templates.name AS templateName,
            inspections.lifecycle_status AS lifecycleStatus,
            inspections.sync_status AS syncStatus,
            inspections.current_section_id AS currentSectionId,
            inspections.started_at_ms AS startedAtMillis,
            inspections.updated_at_ms AS updatedAtMillis,
            inspections.completed_at_ms AS completedAtMillis,
            inspections.earned_weight AS earnedWeight,
            inspections.total_weight AS totalWeight
        FROM inspections
        INNER JOIN assets ON assets.id = inspections.asset_id
        INNER JOIN inspection_templates
            ON inspection_templates.revision_id = inspections.template_revision_id
        WHERE inspections.id = :inspectionId
        """,
    )
    fun observeInspectionSession(inspectionId: String): Flow<InspectionSessionRecord?>

    @Query(
        """
        SELECT
            inspections.id AS inspectionId,
            inspections.asset_id AS assetId,
            assets.name AS assetName,
            inspection_templates.revision_id AS templateId,
            inspection_templates.name AS templateName,
            inspections.lifecycle_status AS lifecycleStatus,
            inspections.sync_status AS syncStatus,
            inspections.current_section_id AS currentSectionId,
            inspections.started_at_ms AS startedAtMillis,
            inspections.updated_at_ms AS updatedAtMillis,
            inspections.completed_at_ms AS completedAtMillis,
            inspections.earned_weight AS earnedWeight,
            inspections.total_weight AS totalWeight
        FROM inspections
        INNER JOIN assets ON assets.id = inspections.asset_id
        INNER JOIN inspection_templates
            ON inspection_templates.revision_id = inspections.template_revision_id
        WHERE inspections.id = :inspectionId
        """,
    )
    suspend fun getInspectionSession(inspectionId: String): InspectionSessionRecord?

    @Query(
        """
        SELECT revision_id FROM inspection_templates
        WHERE template_id = :templateId
        ORDER BY version DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestTemplateRevisionId(templateId: String): String?

    @Query("SELECT * FROM inspections ORDER BY updated_at_ms DESC, id")
    fun observeInspections(): Flow<List<InspectionEntity>>

    @Query("SELECT * FROM inspections WHERE id = :inspectionId")
    fun observeInspection(inspectionId: String): Flow<InspectionEntity?>

    @Query(
        """
        SELECT assets.name FROM assets
        INNER JOIN inspections ON inspections.asset_id = assets.id
        WHERE inspections.id = :inspectionId
        """,
    )
    fun observeAssetName(inspectionId: String): Flow<String?>

    @Query("SELECT * FROM inspections WHERE id = :inspectionId")
    suspend fun getInspection(inspectionId: String): InspectionEntity?

    @Query(
        """
        SELECT assets.name FROM assets
        INNER JOIN inspections ON inspections.asset_id = assets.id
        WHERE inspections.id = :inspectionId
        """,
    )
    suspend fun getAssetName(inspectionId: String): String?

    @Query(
        """
        SELECT * FROM inspection_answers
        WHERE inspection_id = :inspectionId
        ORDER BY checklist_item_id
        """,
    )
    suspend fun getAnswers(inspectionId: String): List<InspectionAnswerEntity>

    @Query(
        """
        SELECT * FROM evidence
        WHERE inspection_id = :inspectionId
        ORDER BY created_at_ms, id
        """,
    )
    suspend fun getEvidence(inspectionId: String): List<EvidenceEntity>

    @Query("SELECT * FROM evidence WHERE id = :evidenceId")
    suspend fun getEvidenceById(evidenceId: String): EvidenceEntity?

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

    @Query("DELETE FROM evidence WHERE id = :evidenceId")
    suspend fun deleteEvidence(evidenceId: String): Int

    @Query("DELETE FROM inspection_answers WHERE inspection_id = :inspectionId")
    suspend fun deleteAnswers(inspectionId: String)

    @Upsert
    suspend fun upsertIssues(issues: List<MaintenanceIssueEntity>)

    @Query(
        """
        UPDATE assets
        SET next_inspection_due_at_ms = :nextInspectionDueAtMillis
        WHERE id = :assetId
        """,
    )
    suspend fun updateAssetNextInspectionDue(
        assetId: String,
        nextInspectionDueAtMillis: Long?,
    ): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPendingSync(commands: List<PendingSyncEntity>)

    @Transaction
    suspend fun getDraft(inspectionId: String): InspectionDraftRecord? {
        val inspection = getInspection(inspectionId) ?: return null
        val assetName = checkNotNull(getAssetName(inspectionId)) {
            "Inspection asset no longer exists: $inspectionId"
        }
        return InspectionDraftRecord(
            inspection = inspection,
            assetName = assetName,
            answers = getAnswers(inspectionId),
            evidence = getEvidence(inspectionId),
        )
    }

    @Transaction
    suspend fun saveDraft(
        inspection: InspectionEntity,
        answers: List<InspectionAnswerEntity>,
        evidence: List<EvidenceEntity>,
    ) {
        upsertInspection(inspection)
        deleteAnswers(inspection.id)
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
        nextInspectionDueAtMillis: Long?,
    ) {
        upsertInspection(inspection)
        check(
            updateAssetNextInspectionDue(
                assetId = inspection.assetId,
                nextInspectionDueAtMillis = nextInspectionDueAtMillis,
            ) == 1,
        ) {
            "Inspection asset no longer exists: ${inspection.assetId}"
        }
        deleteAnswers(inspection.id)
        upsertAnswers(answers)
        upsertEvidence(evidence)
        upsertIssues(issues)
        insertPendingSync(pendingSync)
    }
}
