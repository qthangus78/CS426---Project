package com.topic11.cs426.data

import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.InspectionSummary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FakeInspectionRepositoryTest {
    @Test
    fun `observeInspectionSummaries emits deterministic sample data`() = runTest {
        val firstRepository = FakeInspectionRepository()
        val secondRepository = FakeInspectionRepository()

        val firstEmission = firstRepository.observeInspectionSummaries().first()
        val secondEmission = secondRepository.observeInspectionSummaries().first()

        assertEquals(expectedInspections, firstEmission)
        assertEquals(expectedInspections, secondEmission)
    }

    @Test
    fun `observeInspection returns expected item by id`() = runTest {
        val repository = FakeInspectionRepository()

        val inspection = repository
            .observeInspection(InspectionId("projector-p-204"))
            .first()

        assertEquals(expectedInspections[1], inspection)
    }

    @Test
    fun `observeInspection emits null for unknown id`() = runTest {
        val repository = FakeInspectionRepository()

        val inspection = repository
            .observeInspection(InspectionId("unknown-inspection"))
            .first()

        assertNull(inspection)
    }

    private companion object {
        val expectedInspections = listOf(
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
