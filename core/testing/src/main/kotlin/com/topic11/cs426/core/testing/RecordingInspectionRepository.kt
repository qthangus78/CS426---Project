package com.topic11.cs426.core.testing

import com.topic11.cs426.domain.model.InspectionId
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
    private val _observedInspectionIds = mutableListOf<InspectionId>()

    var observeInspectionSummariesCalls: Int = 0
        private set

    val observedInspectionIds: List<InspectionId>
        get() = _observedInspectionIds.toList()

    override fun observeInspectionSummaries(): Flow<List<InspectionSummary>> {
        observeInspectionSummariesCalls += 1
        return summaries
    }

    override fun observeInspection(inspectionId: InspectionId): Flow<InspectionSummary?> {
        _observedInspectionIds += inspectionId
        return summaries
            .map { currentSummaries -> currentSummaries.firstOrNull { it.id == inspectionId } }
            .distinctUntilChanged()
    }

    fun replaceSummaries(nextSummaries: List<InspectionSummary>) {
        summaries.value = nextSummaries
    }
}
