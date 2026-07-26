package com.topic11.cs426.data

import com.topic11.cs426.core.database.FieldFlowDatabase
import com.topic11.cs426.core.database.entity.InspectionEntity
import com.topic11.cs426.core.database.entity.PendingSyncEntity
import com.topic11.cs426.data.mapping.toDomain
import com.topic11.cs426.data.mapping.toEntity
import com.topic11.cs426.domain.model.CompletedInspection
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionSession
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
    private val database: FieldFlowDatabase,
    private val inspectionIdFactory: () -> String = { "inspection-${UUID.randomUUID()}" },
) : InspectionRepository {
    private val inspectionDao = database.inspectionDao()
    private val catalogDao = database.catalogDao()

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
        val asset = requireNotNull(catalogDao.getAsset(assetId)) {
            "Asset does not exist: $assetId"
        }
        require(asset.name == assetName) {
            "Asset name does not match persisted asset $assetId"
        }
        val template = requireNotNull(catalogDao.getTemplateAggregate(templateId)) {
            "Template does not exist: $templateId"
        }
        val id = InspectionId(inspectionIdFactory())
        inspectionDao.upsertInspection(
            InspectionEntity(
                id = id.value,
                assetId = asset.id,
                templateRevisionId = template.template.revisionId,
                lifecycleStatus = "NOT_STARTED",
                syncStatus = "NOT_REQUIRED",
                currentSectionId = null,
                startedAtMillis = startedAtMillis,
                updatedAtMillis = startedAtMillis,
                completedAtMillis = null,
                earnedWeight = null,
                totalWeight = null,
            ),
        )
        return id
    }

    override suspend fun saveDraft(session: InspectionSession) {
        val existing = requireNotNull(inspectionDao.getInspection(session.id.value)) {
            "Inspection does not exist: ${session.id.value}"
        }
        require(existing.assetId == session.assetId.value) {
            "Cannot move an inspection to a different asset"
        }
        require(existing.templateRevisionId == session.templateId.value) {
            "Cannot move an inspection to a different template revision"
        }
        val answerTypes = answerTypesFor(existing.templateRevisionId)
        val existingEvidence = inspectionDao.getEvidence(session.id.value)

        inspectionDao.saveDraft(
            inspection = session.toEntity(existing),
            answers = session.answers.map { answer ->
                answer.toEntity(
                    answerType = requireNotNull(answerTypes[answer.checklistItemId.value]) {
                        "Checklist item is not part of the inspection template: " +
                            answer.checklistItemId.value
                    },
                )
            },
            evidence = existingEvidence,
        )
    }

    override suspend fun complete(completed: CompletedInspection) {
        val existing = requireNotNull(inspectionDao.getInspection(completed.id.value)) {
            "Inspection does not exist: ${completed.id.value}"
        }
        val answerTypes = answerTypesFor(existing.templateRevisionId)
        val completedEntity = existing.copy(
            lifecycleStatus = "COMPLETED",
            syncStatus = "PENDING",
            updatedAtMillis = completed.completedAtMillis,
            completedAtMillis = completed.completedAtMillis,
            earnedWeight = completed.score.earnedWeight.toDouble(),
            totalWeight = completed.score.totalWeight.toDouble(),
        )
        val pendingSync = PendingSyncEntity(
            id = "sync-complete-${completed.id.value}",
            aggregateType = "INSPECTION",
            aggregateId = completed.id.value,
            operation = "COMPLETE",
            payloadVersion = 1,
            payloadJson = """{"inspectionId":"${completed.id.value}"}""",
            state = "PENDING",
            attemptCount = 0,
            lastErrorCode = null,
            createdAtMillis = completed.completedAtMillis,
            updatedAtMillis = completed.completedAtMillis,
        )

        inspectionDao.completeInspection(
            inspection = completedEntity,
            answers = completed.answers.map { answer ->
                answer.toEntity(
                    answerType = requireNotNull(answerTypes[answer.checklistItemId.value]) {
                        "Checklist item is not part of the inspection template: " +
                            answer.checklistItemId.value
                    },
                )
            },
            evidence = inspectionDao.getEvidence(completed.id.value),
            issues = completed.issues.map { it.toEntity(completed.completedAtMillis) },
            pendingSync = listOf(pendingSync),
            nextInspectionDueAtMillis = completed.nextInspectionDueAtMillis,
        )
    }

    private suspend fun answerTypesFor(templateRevisionId: String): Map<String, String> {
        val template = requireNotNull(catalogDao.getTemplateAggregate(templateRevisionId)) {
            "Template revision no longer exists: $templateRevisionId"
        }
        return template.sections
            .flatMap { it.items }
            .associate { it.id to it.answerType }
    }
}
