package com.topic11.cs426.data

import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FakeInspectionRepositoryTest {
    @Test
    fun `observeInspectionSummaries emits deterministic sample data`() = runTest {
        val repository = FakeInspectionRepository()

        val summaries = repository.observeInspectionSummaries().first()

        assertEquals(3, summaries.size)
        assertEquals("Computer Lab I.44", summaries[0].title)
        assertEquals(InspectionStatus.IN_PROGRESS, summaries[0].status)
        assertEquals(6, summaries[0].completedItems)
        assertEquals(10, summaries[0].totalItems)
        assertEquals("Projector P-204", summaries[1].title)
        assertEquals(InspectionStatus.NOT_STARTED, summaries[1].status)
        assertEquals("Laboratory A2 Safety Check", summaries[2].title)
        assertEquals(InspectionStatus.SYNC_PENDING, summaries[2].status)
        assertEquals(12, summaries[2].completedItems)
        assertEquals(12, summaries[2].totalItems)
    }

    @Test
    fun `observeInspection returns expected item by id`() = runTest {
        val repository = FakeInspectionRepository()

        val inspection = repository
            .observeInspection(InspectionId("projector-p-204"))
            .first()

        assertEquals("Projector P-204", inspection?.assetName)
        assertEquals(InspectionStatus.NOT_STARTED, inspection?.status)
    }

    @Test
    fun `observeInspection emits null for unknown id`() = runTest {
        val repository = FakeInspectionRepository()

        val inspection = repository
            .observeInspection(InspectionId("unknown-inspection"))
            .first()

        assertNull(inspection)
    }
}
