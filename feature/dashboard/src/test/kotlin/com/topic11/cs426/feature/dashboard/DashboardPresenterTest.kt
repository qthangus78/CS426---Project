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
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.InspectionSummary
import com.topic11.cs426.domain.usecase.ObserveInspectionSummariesUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
            assertEquals(InspectionFilterUi.ALL, content.selectedFilter)
            assertEquals(InspectionTestFixtures.computerLab.id, content.heroInspection?.id)
            assertEquals(InspectionTestFixtures.inspectionSummaries.size, content.filteredInspections.size)
            assertEquals(InspectionTestFixtures.computerLab.id, content.filteredInspections[0].id)
            assertEquals("Computer Lab I.44", content.filteredInspections[0].title)
            assertEquals("In progress", content.filteredInspections[0].statusLabel)
            assertEquals(0.6f, content.filteredInspections[0].progressFraction, 0.0f)
            assertEquals("Projector P-204", content.filteredInspections[1].title)
            assertEquals("Not started", content.filteredInspections[1].statusLabel)
            assertEquals(0.0f, content.filteredInspections[1].progressFraction, 0.0f)
            assertEquals("Laboratory A2 Safety Check", content.filteredInspections[2].title)
            assertEquals("Sync pending", content.filteredInspections[2].statusLabel)
            assertEquals(1.0f, content.filteredInspections[2].progressFraction, 0.0f)
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
            assertEquals(InspectionFilterUi.ALL, empty.selectedFilter)
            assertNotNull(empty.eventSink)
        }
    }

    @Test
    fun `hero selection prefers in progress inspection`() = runTest {
        val presenter = presenter()

        presenter.test {
            awaitItem()
            val content = awaitItem() as DashboardState.Content

            assertEquals(InspectionTestFixtures.computerLab.id, content.heroInspection?.id)
            assertEquals("In progress", content.heroInspection?.statusLabel)
        }
    }

    @Test
    fun `hero selection falls back to sync pending inspection`() = runTest {
        val presenter = presenter(
            initialSummaries = listOf(
                InspectionTestFixtures.projector,
                InspectionTestFixtures.laboratorySafetyCheck,
            ),
        )

        presenter.test {
            awaitItem()
            val content = awaitItem() as DashboardState.Content

            assertEquals(InspectionTestFixtures.laboratorySafetyCheck.id, content.heroInspection?.id)
            assertEquals("Sync pending", content.heroInspection?.statusLabel)
        }
    }

    @Test
    fun `hero selection is absent when no inspection can continue`() = runTest {
        val presenter = presenter(
            initialSummaries = listOf(
                InspectionTestFixtures.projector,
                inspectionSummary(
                    id = "completed-inspection",
                    title = "Completed inspection",
                    status = InspectionStatus.COMPLETED,
                    completedItems = 4,
                    totalItems = 4,
                ),
            ),
        )

        presenter.test {
            awaitItem()
            val content = awaitItem() as DashboardState.Content

            assertNull(content.heroInspection)
        }
    }

    @Test
    fun `default filter is all inspections`() = runTest {
        val presenter = presenter()

        presenter.test {
            awaitItem()
            val content = awaitItem() as DashboardState.Content

            assertEquals(InspectionFilterUi.ALL, content.selectedFilter)
            assertEquals(
                InspectionTestFixtures.inspectionSummaries.map { summary -> summary.id },
                content.filteredInspections.map { inspection -> inspection.id },
            )
        }
    }

    @Test
    fun `selecting in progress filter shows in progress inspections`() = runTest {
        assertFilter(
            selectedFilter = InspectionFilterUi.IN_PROGRESS,
            expectedInspectionIds = listOf(InspectionTestFixtures.computerLab.id),
        )
    }

    @Test
    fun `selecting not started filter shows not started inspections`() = runTest {
        assertFilter(
            selectedFilter = InspectionFilterUi.NOT_STARTED,
            expectedInspectionIds = listOf(InspectionTestFixtures.projector.id),
        )
    }

    @Test
    fun `selecting sync pending filter shows sync pending inspections`() = runTest {
        assertFilter(
            selectedFilter = InspectionFilterUi.SYNC_PENDING,
            expectedInspectionIds = listOf(InspectionTestFixtures.laboratorySafetyCheck.id),
        )
    }

    @Test
    fun `filtered empty state keeps content metrics and empty visible list`() = runTest {
        val presenter = presenter(
            initialSummaries = listOf(InspectionTestFixtures.computerLab),
        )

        presenter.test {
            awaitItem()
            val content = awaitItem() as DashboardState.Content

            content.eventSink(DashboardEvent.FilterSelected(InspectionFilterUi.SYNC_PENDING))

            val filtered = awaitItem() as DashboardState.Content
            assertEquals(1, filtered.overview.totalInspections)
            assertEquals(1, filtered.overview.inProgressInspections)
            assertEquals(0, filtered.overview.syncPendingInspections)
            assertEquals(InspectionTestFixtures.computerLab.id, filtered.heroInspection?.id)
            assertEquals(InspectionFilterUi.SYNC_PENDING, filtered.selectedFilter)
            assertTrue(filtered.filteredInspections.isEmpty())
        }
    }

    @Test
    fun `inspection selection navigates to typed inspection screen`() = runTest {
        val navigator = FakeNavigator(DashboardScreen)
        val presenter = presenter(navigator = navigator)

        presenter.test {
            awaitItem()
            val content = awaitItem() as DashboardState.Content
            val selectedInspection = content.filteredInspections.first()

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

    private suspend fun assertFilter(
        selectedFilter: InspectionFilterUi,
        expectedInspectionIds: List<InspectionId>,
    ) {
        val presenter = presenter()

        presenter.test {
            awaitItem()
            val content = awaitItem() as DashboardState.Content

            content.eventSink(DashboardEvent.FilterSelected(selectedFilter))

            val filtered = awaitItem() as DashboardState.Content
            assertEquals(selectedFilter, filtered.selectedFilter)
            assertEquals(expectedInspectionIds, filtered.filteredInspections.map { inspection -> inspection.id })
            assertEquals(3, filtered.overview.totalInspections)
            assertEquals(InspectionTestFixtures.computerLab.id, filtered.heroInspection?.id)
        }
    }

    private suspend fun assertQuickAccessNavigation(
        event: DashboardEvent,
        expectedScreen: com.slack.circuit.runtime.screen.Screen,
    ) {
        val navigator = FakeNavigator(DashboardScreen)
        val presenter = presenter(navigator = navigator)

        presenter.test {
            awaitItem()
            val content = awaitItem() as DashboardState.Content

            content.eventSink(event)

            assertEquals(expectedScreen, navigator.awaitNextScreen())
        }
    }

    private fun presenter(
        initialSummaries: List<InspectionSummary> = InspectionTestFixtures.inspectionSummaries,
        navigator: FakeNavigator = FakeNavigator(DashboardScreen),
    ): DashboardPresenter {
        val repository = RecordingInspectionRepository(initialSummaries = initialSummaries)
        return DashboardPresenter(
            observeInspectionSummaries = ObserveInspectionSummariesUseCase(repository),
            navigator = navigator,
        )
    }

    private fun inspectionSummary(
        id: String,
        title: String,
        status: InspectionStatus,
        completedItems: Int,
        totalItems: Int,
    ): InspectionSummary {
        return InspectionSummary(
            id = InspectionId(id),
            title = title,
            status = status,
            completedItems = completedItems,
            totalItems = totalItems,
        )
    }
}
