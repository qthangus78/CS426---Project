package com.topic11.cs426.core.testing

import com.topic11.cs426.domain.model.CompletedInspection
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionSession
import com.topic11.cs426.domain.model.InspectionSummary
import com.topic11.cs426.domain.repository.InspectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class RecordingInspectionRepository(
    initialSummaries: List<InspectionSummary> = InspectionTestFixtures.inspectionSummaries,
) : InspectionRepository {
    private val summaries = MutableStateFlow(initialSummaries)
    private val sessions = mutableMapOf<InspectionId, InspectionSession>()
    private val _observedInspectionIds = mutableListOf<InspectionId>()

    var observeInspectionSummariesCalls: Int = 0
        private set

    var createInspectionCalls: Int = 0
        private set

    var saveDraftCalls: Int = 0
        private set

    var completeCalls: Int = 0
        private set

    val observedInspectionIds: List<InspectionId>
        get() = _observedInspectionIds.toList()

    val savedSessions: List<InspectionSession>
        get() = sessions.values.toList()

    override fun observeInspectionSummaries(): Flow<List<InspectionSummary>> {
        observeInspectionSummariesCalls += 1
        return summaries
    }

    override fun observeInspection(inspectionId: InspectionId): Flow<InspectionSession?> {
        _observedInspectionIds += inspectionId
        return MutableStateFlow(sessions[inspectionId])
    }

    override suspend fun getInspection(inspectionId: InspectionId): InspectionSession? {
        return sessions[inspectionId]
    }

    override suspend fun createInspection(
        assetId: String,
        assetName: String,
        templateId: String,
        startedAtMillis: Long,
    ): InspectionId {
        createInspectionCalls += 1
        val id = InspectionId("inspection-$createInspectionCalls")
        return id
    }

    override suspend fun saveDraft(session: InspectionSession) {
        saveDraftCalls += 1
        sessions[session.id] = session
    }

    override suspend fun complete(completed: CompletedInspection) {
        completeCalls += 1
    }

    fun replaceSummaries(nextSummaries: List<InspectionSummary>) {
        summaries.value = nextSummaries
    }

    fun addSession(session: InspectionSession) {
        sessions[session.id] = session
    }
}
