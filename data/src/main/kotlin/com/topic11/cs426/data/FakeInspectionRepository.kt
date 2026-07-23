package com.topic11.cs426.data

import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.ChecklistAnswerValue
import com.topic11.cs426.domain.model.CompletedInspection
import com.topic11.cs426.domain.model.InspectionAnswer
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionSession
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.InspectionSummary
import com.topic11.cs426.domain.model.TemplateId
import com.topic11.cs426.domain.repository.InspectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class FakeInspectionRepository : InspectionRepository {
    private val sessions = MutableStateFlow(sampleSessions)

    override fun observeInspectionSummaries(): Flow<List<InspectionSummary>> {
        return sessions.map { list ->
            list.map { session ->
                InspectionSummary(
                    id = session.id,
                    title = session.assetName,
                    status = session.status,
                    completedItems = session.answers.count { it.value != null },
                    totalItems = if (session.id.value == "computer-lab-i-44") 10
                    else if (session.id.value == "projector-p-204") 8
                    else session.answers.size.coerceAtLeast(12),
                )
            }
        }
    }

    override fun observeInspection(inspectionId: InspectionId): Flow<InspectionSession?> {
        return sessions
            .map { list -> list.firstOrNull { it.id == inspectionId } }
            .distinctUntilChanged()
    }

    override suspend fun getInspection(inspectionId: InspectionId): InspectionSession? {
        return sessions.value.firstOrNull { it.id == inspectionId }
    }

    override suspend fun createInspection(
        assetId: String,
        assetName: String,
        templateId: String,
        startedAtMillis: Long,
    ): InspectionId {
        val id = InspectionId("inspection-${System.currentTimeMillis()}")
        val newSession = InspectionSession(
            id = id,
            assetId = AssetId(assetId),
            assetName = assetName,
            templateId = TemplateId(templateId),
            status = InspectionStatus.NOT_STARTED,
            startedAtMillis = startedAtMillis,
            updatedAtMillis = startedAtMillis,
        )
        sessions.value = sessions.value + newSession
        return id
    }

    override suspend fun saveDraft(session: InspectionSession) {
        sessions.value = sessions.value.map { if (it.id == session.id) session else it }
    }

    override suspend fun complete(completed: CompletedInspection) {
        sessions.value = sessions.value.map { session ->
            if (session.id == completed.id) {
                session.copy(
                    status = InspectionStatus.COMPLETED,
                    answers = completed.answers,
                    completedAtMillis = completed.completedAtMillis,
                    score = completed.score,
                )
            } else {
                session
            }
        }
    }

    private companion object {
        val sampleSessions = listOf(
            InspectionSession(
                id = InspectionId("computer-lab-i-44"),
                assetId = AssetId("asset-lab-1"),
                assetName = "Computer Lab I.44",
                templateId = TemplateId("template-standard"),
                status = InspectionStatus.IN_PROGRESS,
                answers = List(10) { i ->
                    InspectionAnswer(
                        inspectionId = InspectionId("computer-lab-i-44"),
                        checklistItemId = com.topic11.cs426.domain.model.ChecklistItemId("item-$i"),
                        value = if (i < 6) ChecklistAnswerValue.Pass else null,
                        updatedAtMillis = 0L,
                    )
                },
                startedAtMillis = 0L,
                updatedAtMillis = 0L,
            ),
            InspectionSession(
                id = InspectionId("projector-p-204"),
                assetId = AssetId("asset-proj-1"),
                assetName = "Projector P-204",
                templateId = TemplateId("template-standard"),
                status = InspectionStatus.NOT_STARTED,
                answers = emptyList(), // totalItems will be forced to 8 in summary
                startedAtMillis = 0L,
                updatedAtMillis = 0L,
            ),
            InspectionSession(
                id = InspectionId("laboratory-a2-safety-check"),
                assetId = AssetId("asset-lab-2"),
                assetName = "Laboratory A2 Safety Check",
                templateId = TemplateId("template-standard"),
                status = InspectionStatus.SYNC_PENDING,
                answers = List(12) { i ->
                    InspectionAnswer(
                        inspectionId = InspectionId("laboratory-a2-safety-check"),
                        checklistItemId = com.topic11.cs426.domain.model.ChecklistItemId("item-$i"),
                        value = ChecklistAnswerValue.Pass,
                        updatedAtMillis = 0L,
                    )
                },
                startedAtMillis = 0L,
                updatedAtMillis = 0L,
            ),
        )
    }
}
