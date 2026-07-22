package com.topic11.cs426.feature.dashboard

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import com.topic11.cs426.core.navigation.AssetsScreen
import com.topic11.cs426.core.navigation.DashboardScreen
import com.topic11.cs426.core.navigation.InspectionScreen
import com.topic11.cs426.core.navigation.IssuesScreen
import com.topic11.cs426.core.navigation.ReportsScreen
import com.topic11.cs426.core.navigation.TemplatesScreen
import com.topic11.cs426.core.testing.InspectionTestFixtures
import com.topic11.cs426.core.testing.RecordingInspectionRepository
import com.topic11.cs426.domain.usecase.ObserveInspectionSummariesUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardPresenterTest {
    @Test
    fun `present emits loading then content with sample inspections`() = runTest {
        val repository = RecordingInspectionRepository()
        val navigator = FakeNavigator(DashboardScreen)
        val presenter = DashboardPresenter(
            observeInspectionSummaries = ObserveInspectionSummariesUseCase(repository),
            navigator = navigator,
        )

        presenter.test {
            val loading = awaitItem()
            assertEquals(DashboardState.Loading, loading)

            val content = awaitItem() as DashboardState.Content
            assertEquals(3, content.overview.totalInspections)
            assertEquals(1, content.overview.inProgressInspections)
            assertEquals(1, content.overview.syncPendingInspections)
            assertEquals(InspectionTestFixtures.inspectionSummaries.size, content.inspections.size)
            assertEquals(InspectionTestFixtures.computerLab.id, content.inspections[0].id)
            assertEquals("Computer Lab I.44", content.inspections[0].title)
            assertEquals("In progress", content.inspections[0].statusLabel)
            assertEquals(0.6f, content.inspections[0].progressFraction, 0.0f)
            assertEquals("Projector P-204", content.inspections[1].title)
            assertEquals("Not started", content.inspections[1].statusLabel)
            assertEquals(0.0f, content.inspections[1].progressFraction, 0.0f)
            assertEquals("Laboratory A2 Safety Check", content.inspections[2].title)
            assertEquals("Sync pending", content.inspections[2].statusLabel)
            assertEquals(1.0f, content.inspections[2].progressFraction, 0.0f)
            assertNotNull(content.eventSink)
        }
    }

    @Test
    fun `present emits empty state when repository has no inspections`() = runTest {
        val repository = RecordingInspectionRepository(initialSummaries = emptyList())
        val navigator = FakeNavigator(DashboardScreen)
        val presenter = DashboardPresenter(
            observeInspectionSummaries = ObserveInspectionSummariesUseCase(repository),
            navigator = navigator,
        )

        presenter.test {
            assertEquals(DashboardState.Loading, awaitItem())

            val empty = awaitItem()
            assertTrue(empty is DashboardState.Empty)
            empty as DashboardState.Empty
            assertEquals(0, empty.overview.totalInspections)
            assertEquals(0, empty.overview.inProgressInspections)
            assertEquals(0, empty.overview.syncPendingInspections)
            assertNotNull(empty.eventSink)
        }
    }

    @Test
    fun `inspection selection navigates to typed inspection screen`() = runTest {
        val repository = RecordingInspectionRepository()
        val navigator = FakeNavigator(DashboardScreen)
        val presenter = DashboardPresenter(
            observeInspectionSummaries = ObserveInspectionSummariesUseCase(repository),
            navigator = navigator,
        )

        presenter.test {
            awaitItem()
            val content = awaitItem() as DashboardState.Content
            val selectedInspection = content.inspections.first()

            content.eventSink(DashboardEvent.InspectionSelected(selectedInspection.id))

            assertEquals(
                InspectionScreen(selectedInspection.id.value),
                navigator.awaitNextScreen(),
            )
        }
    }

    @Test
    fun `quick access actions navigate to placeholder screens`() = runTest {
        assertQuickAccessNavigation(DashboardEvent.AssetsSelected, AssetsScreen)
        assertQuickAccessNavigation(DashboardEvent.TemplatesSelected, TemplatesScreen)
        assertQuickAccessNavigation(DashboardEvent.IssuesSelected, IssuesScreen)
        assertQuickAccessNavigation(DashboardEvent.ReportsSelected, ReportsScreen)
    }

    private suspend fun assertQuickAccessNavigation(
        event: DashboardEvent,
        expectedScreen: com.slack.circuit.runtime.screen.Screen,
    ) {
        val repository = RecordingInspectionRepository()
        val navigator = FakeNavigator(DashboardScreen)
        val presenter = DashboardPresenter(
            observeInspectionSummaries = ObserveInspectionSummariesUseCase(repository),
            navigator = navigator,
        )

        presenter.test {
            awaitItem()
            val content = awaitItem() as DashboardState.Content

            content.eventSink(event)

            assertEquals(expectedScreen, navigator.awaitNextScreen())
        }
    }
}
