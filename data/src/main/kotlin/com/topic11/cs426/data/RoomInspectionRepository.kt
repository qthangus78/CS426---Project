package com.topic11.cs426.data

import com.topic11.cs426.core.database.dao.InspectionDao
import com.topic11.cs426.core.database.entity.EvidenceEntity
import com.topic11.cs426.core.database.entity.InspectionEntity
import com.topic11.cs426.core.database.entity.MaintenanceIssueEntity
import com.topic11.cs426.core.database.entity.PendingSyncEntity
import com.topic11.cs426.data.mapping.toDomain
import com.topic11.cs426.data.mapping.toEntity
import com.topic11.cs426.domain.model.CompletedInspection
import com.topic11.cs426.domain.model.InspectionAnswer
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionSession
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.InspectionSummary
import com.topic11.cs426.domain.repository.InspectionRepository
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class RoomInspectionRepository(
    private val inspectionDao: InspectionDao,
) : InspectionRepository {
    override fun observeInspectionSummaries(): Flow<List<InspectionSummary>> {
        return inspectionDao.observeInspectionSummaries()
            .map { records -> records.map { it.toDomain() } }
            .distinctUntilChanged()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeInspection(inspectionId: InspectionId): Flow<InspectionSession?> {
        return inspectionDao.observeInspectionSession(inspectionId.value)
            .flatMapLatest { record ->
                if (record == null) {
                    flowOf(null)
                } else {
                    combine(
                        inspectionDao.observeAnswers(inspectionId.value),
                        inspectionDao.observeEvidence(inspectionId.value),
                    ) { answers, evidence ->
                        record.toDomain(answers, evidence)
                    }
                }
            }
            .distinctUntilChanged()
    }

    override suspend fun getInspection(inspectionId: InspectionId): InspectionSession? {
        val record = inspectionDao.getInspectionSession(inspectionId.value) ?: return null
        return record.toDomain(
            answers = inspectionDao.getAnswers(inspectionId.value),
            evidence = inspectionDao.getEvidence(inspectionId.value),
        )
    }

    override suspend fun createInspection(
        assetId: String,
        assetName: String,
        templateId: String,
        startedAtMillis: Long,
    ): InspectionId {
        val templateRevisionId = inspectionDao.getLatestTemplateRevisionId(templateId)
            ?: error("Template $templateId not found")
        val inspectionId = InspectionId("inspection-${UUID.randomUUID()}")
        inspectionDao.upsertInspection(
            InspectionEntity(
                id = inspectionId.value,
                assetId = assetId,
                templateRevisionId = templateRevisionId,
                lifecycleStatus = LIFECYCLE_NOT_STARTED,
                syncStatus = SYNC_NOT_REQUIRED,
                currentSectionId = null,
                startedAtMillis = startedAtMillis,
                updatedAtMillis = startedAtMillis,
                completedAtMillis = null,
                earnedWeight = null,
                totalWeight = null,
            ),
        )
        return inspectionId
    }

    override suspend fun saveDraft(session: InspectionSession) {
        val existingInspection = inspectionDao.getInspection(session.id.value)
        val templateRevisionId = existingInspection?.templateRevisionId
            ?: inspectionDao.getLatestTemplateRevisionId(session.templateId.value)
            ?: error("Template ${session.templateId.value} not found")
        val existingEvidence = inspectionDao.getEvidence(session.id.value).associateBy(EvidenceEntity::id)

        inspectionDao.saveDraft(
            inspection = session.toEntity(templateRevisionId),
            answers = session.answers.map(InspectionAnswer::toEntity),
            evidence = session.answers.toEvidenceEntities(existingEvidence),
        )
    }

    override suspend fun complete(completed: CompletedInspection) {
        val existingInspection = inspectionDao.getInspection(completed.id.value)
            ?: error("Inspection ${completed.id.value} not found")
        val existingEvidence = inspectionDao.getEvidence(completed.id.value).associateBy(EvidenceEntity::id)
        val completedInspection = existingInspection.copy(
            lifecycleStatus = LIFECYCLE_COMPLETED,
            syncStatus = SYNC_PENDING,
            updatedAtMillis = completed.completedAtMillis,
            completedAtMillis = completed.completedAtMillis,
            earnedWeight = completed.score.earnedWeight.toDouble(),
            totalWeight = completed.score.totalWeight.toDouble(),
        )

        inspectionDao.completeInspection(
            inspection = completedInspection,
            answers = completed.answers.map(InspectionAnswer::toEntity),
            evidence = completed.answers.toEvidenceEntities(existingEvidence),
            issues = completed.issues.map { issue ->
                MaintenanceIssueEntity(
                    id = issue.id.value,
                    inspectionId = issue.inspectionId.value,
                    assetId = issue.assetId.value,
                    checklistItemId = issue.checklistItemId?.value,
                    severity = issue.severity.name,
                    title = issue.title,
                    description = issue.description.orEmpty(),
                    status = issue.status.name,
                    createdAtMillis = issue.createdAtMillis,
                    updatedAtMillis = completed.completedAtMillis,
                )
            },
            pendingSync = listOf(
                PendingSyncEntity(
                    id = "inspection-${completed.id.value}-complete-v1",
                    aggregateType = "INSPECTION",
                    aggregateId = completed.id.value,
                    operation = "COMPLETE",
                    payloadVersion = 1,
                    payloadJson = "{\"inspectionId\":\"${completed.id.value}\"}",
                    state = SYNC_PENDING,
                    attemptCount = 0,
                    lastErrorCode = null,
                    createdAtMillis = completed.completedAtMillis,
                    updatedAtMillis = completed.completedAtMillis,
                ),
            ),
        )
    }
}

private fun InspectionSession.toEntity(templateRevisionId: String): InspectionEntity {
    val persistenceStatus = status.toPersistenceStatus()
    return InspectionEntity(
        id = id.value,
        assetId = assetId.value,
        templateRevisionId = templateRevisionId,
        lifecycleStatus = persistenceStatus.lifecycleStatus,
        syncStatus = persistenceStatus.syncStatus,
        currentSectionId = currentSectionId?.value,
        startedAtMillis = startedAtMillis,
        updatedAtMillis = updatedAtMillis,
        completedAtMillis = completedAtMillis,
        earnedWeight = score?.earnedWeight?.toDouble(),
        totalWeight = score?.totalWeight?.toDouble(),
    )
}

private fun List<InspectionAnswer>.toEvidenceEntities(
    existingEvidence: Map<String, EvidenceEntity>,
): List<EvidenceEntity> {
    return flatMap { answer ->
        answer.evidenceIds.map { evidenceId ->
            existingEvidence[evidenceId.value] ?: EvidenceEntity(
                id = evidenceId.value,
                inspectionId = answer.inspectionId.value,
                checklistItemId = answer.checklistItemId.value,
                storageKey = evidenceId.value,
                mimeType = null,
                createdAtMillis = answer.updatedAtMillis,
            )
        }
    }.distinctBy(EvidenceEntity::id)
}

private fun InspectionStatus.toPersistenceStatus(): PersistenceStatus = when (this) {
    InspectionStatus.NOT_STARTED -> PersistenceStatus(LIFECYCLE_NOT_STARTED, SYNC_NOT_REQUIRED)
    InspectionStatus.IN_PROGRESS -> PersistenceStatus(LIFECYCLE_IN_PROGRESS, SYNC_NOT_REQUIRED)
    InspectionStatus.REVIEWING -> PersistenceStatus(LIFECYCLE_REVIEWING, SYNC_NOT_REQUIRED)
    InspectionStatus.COMPLETED -> PersistenceStatus(LIFECYCLE_COMPLETED, SYNC_SYNCED)
    InspectionStatus.SYNC_PENDING -> PersistenceStatus(LIFECYCLE_COMPLETED, SYNC_PENDING)
}

private data class PersistenceStatus(
    val lifecycleStatus: String,
    val syncStatus: String,
)

private const val LIFECYCLE_NOT_STARTED = "NOT_STARTED"
private const val LIFECYCLE_IN_PROGRESS = "IN_PROGRESS"
private const val LIFECYCLE_REVIEWING = "REVIEWING"
private const val LIFECYCLE_COMPLETED = "COMPLETED"

private const val SYNC_NOT_REQUIRED = "NOT_REQUIRED"
private const val SYNC_PENDING = "PENDING"
private const val SYNC_SYNCED = "SYNCED"
