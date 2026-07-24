package com.topic11.cs426.domain

import com.topic11.cs426.core.testing.InspectionTestFixtures
import com.topic11.cs426.core.testing.RecordingInspectionRepository
import com.topic11.cs426.domain.usecase.ObserveInspectionSummariesUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveInspectionSummariesUseCaseTest {
    @Test
    fun `invoke delegates to repository and emits summaries`() = runTest {
        val repository = RecordingInspectionRepository()
        val useCase = ObserveInspectionSummariesUseCase(repository)

        val result = useCase().first()

        assertEquals(1, repository.observeInspectionSummariesCalls)
        assertEquals(InspectionTestFixtures.inspectionSummaries, result)
    }
}
