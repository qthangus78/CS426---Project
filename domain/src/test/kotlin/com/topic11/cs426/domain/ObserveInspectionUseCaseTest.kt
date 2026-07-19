package com.topic11.cs426.domain

import com.topic11.cs426.core.testing.InspectionTestFixtures
import com.topic11.cs426.core.testing.RecordingInspectionRepository
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.usecase.ObserveInspectionUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObserveInspectionUseCaseTest {
    @Test
    fun `invoke returns matching inspection from repository`() = runTest {
        val repository = RecordingInspectionRepository()
        val useCase = ObserveInspectionUseCase(repository)

        val result = useCase(InspectionTestFixtures.projector.id).first()

        assertEquals(listOf(InspectionTestFixtures.projector.id), repository.observedInspectionIds)
        assertEquals(InspectionTestFixtures.projector, result)
    }

    @Test
    fun `invoke returns null for unknown inspection`() = runTest {
        val repository = RecordingInspectionRepository()
        val useCase = ObserveInspectionUseCase(repository)
        val unknownId = InspectionId("unknown-inspection")

        val result = useCase(unknownId).first()

        assertEquals(listOf(unknownId), repository.observedInspectionIds)
        assertNull(result)
    }
}
