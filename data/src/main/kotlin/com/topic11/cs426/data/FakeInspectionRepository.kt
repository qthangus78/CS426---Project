package com.topic11.cs426.data

import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.InspectionSummary
import com.topic11.cs426.domain.repository.InspectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class FakeInspectionRepository : InspectionRepository {
    private val inspections = MutableStateFlow(sampleInspections)

    override fun observeInspectionSummaries(): Flow<List<InspectionSummary>> {
        return inspections
    }

    override fun observeInspection(inspectionId: InspectionId): Flow<InspectionSummary?> {
        return inspections
            .map { summaries -> summaries.firstOrNull { it.id == inspectionId } }
            .distinctUntilChanged()
    }

    private companion object {
        val sampleInspections = listOf(
            InspectionSummary(
                id = InspectionId("computer-lab-i-44"),
                title = "Computer Lab I.44",
                status = InspectionStatus.IN_PROGRESS,
                completedItems = 6,
                totalItems = 10,
            ),
            InspectionSummary(
                id = InspectionId("projector-p-204"),
                title = "Projector P-204",
                status = InspectionStatus.NOT_STARTED,
                completedItems = 0,
                totalItems = 8,
            ),
            InspectionSummary(
                id = InspectionId("laboratory-a2-safety-check"),
                title = "Laboratory A2 Safety Check",
                status = InspectionStatus.SYNC_PENDING,
                completedItems = 12,
                totalItems = 12,
            ),
        )
    }
}
