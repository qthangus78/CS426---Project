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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardPresenterTest {
    @Test
    fun `present emits loading then sample inspections`() = runTest {
        val repository = RecordingInspectionRepository()
        val navigator = FakeNavigator(DashboardScreen)
        val presenter = DashboardPresenter(
            observeInspectionSummaries = ObserveInspectionSummariesUseCase(repository),
            navigator = navigator,
        )

        presenter.test {
            val loading = awaitItem()
            assertTrue(loading.isLoading)
            assertEquals(emptyList<Any>(), loading.inspections)

            val content = awaitItem()
            assertFalse(content.isLoading)
            assertEquals(InspectionTestFixtures.inspectionSummaries, content.inspections)
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
            val content = awaitItem()
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
            val content = awaitItem()

            content.eventSink(event)

            assertEquals(expectedScreen, navigator.awaitNextScreen())
        }
    }
}
