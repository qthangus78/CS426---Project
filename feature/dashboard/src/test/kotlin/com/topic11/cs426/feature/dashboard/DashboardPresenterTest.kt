package com.topic11.cs426.feature.dashboard

import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import com.topic11.cs426.core.navigation.AssetsScreen
import com.topic11.cs426.core.navigation.DashboardScreen
import com.topic11.cs426.core.navigation.InspectionScreen
import com.topic11.cs426.core.navigation.IssuesScreen
import com.topic11.cs426.core.navigation.LocationsScreen
import com.topic11.cs426.core.navigation.ReportsScreen
import com.topic11.cs426.core.navigation.SettingsScreen
import com.topic11.cs426.core.navigation.TemplatesScreen
import com.topic11.cs426.core.testing.FakeAssetRepository
import com.topic11.cs426.core.testing.FakeIssueRepository
import com.topic11.cs426.core.testing.FakeTemplateRepository
import com.topic11.cs426.core.testing.InspectionTestFixtures
import com.topic11.cs426.core.testing.RecordingInspectionRepository
import com.topic11.cs426.domain.model.Asset
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.InspectionSummary
import com.topic11.cs426.domain.model.IssueId
import com.topic11.cs426.domain.model.IssueSeverity
import com.topic11.cs426.domain.model.MaintenanceIssue
import com.topic11.cs426.domain.model.MaintenanceIssueStatus
import com.topic11.cs426.domain.model.TemplateId
import com.topic11.cs426.domain.repository.IssueRepository
import com.topic11.cs426.domain.usecase.ObserveAssetsUseCase
import com.topic11.cs426.domain.usecase.ObserveInspectionSummariesUseCase
import com.topic11.cs426.domain.usecase.ObserveIssuesUseCase
import com.topic11.cs426.domain.usecase.ObserveTemplatesUseCase
import com.topic11.cs426.domain.usecase.StartInspectionUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardPresenterTest {
    @Test
    fun `present emits loading then content with sample inspections`() = runTest {
        val repository = RecordingInspectionRepository()
        val navigator = FakeNavigator(DashboardScreen)
        val presenter = DashboardPresenter(
            observeInspectionSummaries = ObserveInspectionSummariesUseCase(repository),
            observeAssets = ObserveAssetsUseCase(FakeAssetRepository()),
            observeTemplates = ObserveTemplatesUseCase(fakeTemplateRepository()),
            observeIssues = ObserveIssuesUseCase(FakeIssueRepository()),
            startInspection = StartInspectionUseCase(repository, fakeTemplateRepository()),
            navigator = navigator,
        )

        presenter.test {
            val loading = awaitItem()
            assertEquals(DashboardState.Loading, loading)

            val content = awaitItem() as DashboardState.Content
            assertEquals(3, content.overview.totalInspections)
            assertEquals(1, content.overview.inProgressInspections)
            assertEquals(1, content.overview.syncPendingInspections)
            assertEquals(0, content.overview.pendingIssues)
            assertEquals(InspectionFilterUi.ALL, content.selectedFilter)
            assertEquals(false, content.isAboutVisible)
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
            assertEquals(listOf(InspectionTestFixtures.asset1Id), content.startInspection.assets.map { it.id })
            assertEquals(listOf(InspectionTestFixtures.templateId), content.startInspection.templates.map { it.id })
            assertNotNull(content.eventSink)
        }
    }

    @Test
    fun `present emits empty state when repository has no inspections`() = runTest {
        val repository = RecordingInspectionRepository(initialSummaries = emptyList())
        val navigator = FakeNavigator(DashboardScreen)
        val presenter = DashboardPresenter(
            observeInspectionSummaries = ObserveInspectionSummariesUseCase(repository),
            observeAssets = ObserveAssetsUseCase(FakeAssetRepository()),
            observeTemplates = ObserveTemplatesUseCase(fakeTemplateRepository()),
            observeIssues = ObserveIssuesUseCase(FakeIssueRepository()),
            startInspection = StartInspectionUseCase(repository, fakeTemplateRepository()),
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
            assertEquals(0, empty.overview.pendingIssues)
            assertEquals(InspectionFilterUi.ALL, empty.selectedFilter)
            assertEquals(false, empty.isAboutVisible)
            assertEquals(false, empty.startInspection.isVisible)
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
    fun `about action opens about presentation without changing dashboard data`() = runTest {
        val presenter = presenter()

        presenter.test {
            awaitItem()
            val content = awaitItem() as DashboardState.Content

            content.eventSink(DashboardEvent.AboutSelected)

            val aboutVisible = awaitItem() as DashboardState.Content
            assertEquals(true, aboutVisible.isAboutVisible)
            assertEquals(content.overview, aboutVisible.overview)
            assertEquals(content.heroInspection, aboutVisible.heroInspection)
            assertEquals(content.selectedFilter, aboutVisible.selectedFilter)
            assertEquals(content.filteredInspections, aboutVisible.filteredInspections)
        }
    }

    @Test
    fun `about dismiss closes about presentation without changing dashboard data`() = runTest {
        val presenter = presenter()

        presenter.test {
            awaitItem()
            val content = awaitItem() as DashboardState.Content

            content.eventSink(DashboardEvent.AboutSelected)
            val aboutVisible = awaitItem() as DashboardState.Content

            aboutVisible.eventSink(DashboardEvent.AboutDismissed)

            val aboutHidden = awaitItem() as DashboardState.Content
            assertEquals(false, aboutHidden.isAboutVisible)
            assertEquals(content.overview, aboutHidden.overview)
            assertEquals(content.heroInspection, aboutHidden.heroInspection)
            assertEquals(content.selectedFilter, aboutHidden.selectedFilter)
            assertEquals(content.filteredInspections, aboutHidden.filteredInspections)
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
    fun `start inspection action opens selector with repository options`() = runTest {
        val presenter = presenter()

        presenter.test {
            awaitItem()
            val content = awaitItem() as DashboardState.Content

            content.eventSink(DashboardEvent.StartInspectionSelected)

            val selecting = awaitItem() as DashboardState.Content
            assertEquals(true, selecting.startInspection.isVisible)
            assertEquals(InspectionTestFixtures.asset1Id, selecting.startInspection.selectedAssetId)
            assertEquals(InspectionTestFixtures.templateId, selecting.startInspection.selectedTemplateId)
            assertEquals(true, selecting.startInspection.canConfirm)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `start inspection confirmed creates inspection from exposed template revision and navigates`() = runTest {
        val navigator = FakeNavigator(DashboardScreen)
        val inspectionRepository = RecordingInspectionRepository()
        val revisionId = TemplateId("sample-template-v2")
        val asset = Asset(
            id = AssetId("asset-projector-p204"),
            name = "Projector P-204",
        )
        val templateRepository = fakeTemplateRepository(revisionId)
        val presenter = presenter(
            inspectionRepository = inspectionRepository,
            assetRepository = FakeAssetRepository(initialAssets = listOf(asset)),
            templateRepository = templateRepository,
            navigator = navigator,
        )

        presenter.test {
            awaitItem()
            val content = awaitItem() as DashboardState.Content

            content.eventSink(DashboardEvent.StartInspectionSelected)
            val selecting = awaitItem() as DashboardState.Content
            selecting.eventSink(DashboardEvent.StartInspectionConfirmed)
            advanceUntilIdle()

            assertEquals(InspectionScreen("inspection-1"), navigator.awaitNextScreen())
            assertEquals(1, inspectionRepository.createInspectionCalls)
            assertEquals(1, inspectionRepository.saveDraftCalls)
            val savedSession = inspectionRepository.savedSessions.single()
            assertEquals(asset.id, savedSession.assetId)
            assertEquals(asset.name, savedSession.assetName)
            assertEquals(revisionId, savedSession.templateId)
            assertEquals(InspectionStatus.IN_PROGRESS, savedSession.status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dashboard actions navigate to product screens`() = runTest {
        assertQuickAccessNavigation(DashboardEvent.AssetsSelected, AssetsScreen)
        assertQuickAccessNavigation(DashboardEvent.LocationsSelected, LocationsScreen)
        assertQuickAccessNavigation(DashboardEvent.TemplatesSelected, TemplatesScreen)
        assertQuickAccessNavigation(DashboardEvent.IssuesSelected, IssuesScreen)
        assertQuickAccessNavigation(DashboardEvent.ReportsSelected, ReportsScreen)
        assertQuickAccessNavigation(DashboardEvent.SettingsSelected, SettingsScreen)
    }

    @Test
    fun `overview counts only open and in progress issues as pending`() = runTest {
        val issueRepository = FakeIssueRepository().apply {
            addIssue(issue("issue-open", MaintenanceIssueStatus.OPEN))
            addIssue(issue("issue-in-progress", MaintenanceIssueStatus.IN_PROGRESS))
            addIssue(issue("issue-resolved", MaintenanceIssueStatus.RESOLVED))
            addIssue(issue("issue-closed", MaintenanceIssueStatus.CLOSED))
        }
        val presenter = presenter(issueRepository = issueRepository)

        presenter.test {
            awaitItem()
            val content = awaitItem() as DashboardState.Content

            assertEquals(2, content.overview.pendingIssues)
        }
    }

    @Test
    fun `overview pending issue count updates when an issue is resolved`() = runTest {
        val issueRepository = FakeIssueRepository()
        val openIssue = issue("issue-open", MaintenanceIssueStatus.OPEN)
        issueRepository.addIssue(openIssue)
        val presenter = presenter(issueRepository = issueRepository)

        presenter.test {
            awaitItem()
            assertEquals(1, (awaitItem() as DashboardState.Content).overview.pendingIssues)

            issueRepository.updateIssue(openIssue.copy(status = MaintenanceIssueStatus.RESOLVED))

            assertEquals(0, (awaitItem() as DashboardState.Content).overview.pendingIssues)
        }
    }

    @Test
    fun `present emits error state when an observed flow fails`() = runTest {
        val presenter = presenter(issueRepository = FlakyIssueRepository(failuresBeforeSuccess = 1))

        presenter.test {
            assertEquals(DashboardState.Loading, awaitItem())

            val error = awaitItem() as DashboardState.Error
            assertEquals("Dashboard could not be loaded.", error.message)
            assertNotNull(error.eventSink)
        }
    }

    @Test
    fun `retry after failure re-observes and emits content`() = runTest {
        val issueRepository = FlakyIssueRepository(failuresBeforeSuccess = 1)
        issueRepository.addIssue(issue("issue-open", MaintenanceIssueStatus.OPEN))
        val presenter = presenter(issueRepository = issueRepository)

        presenter.test {
            awaitItem()
            val error = awaitItem() as DashboardState.Error

            error.eventSink(DashboardEvent.RetrySelected)

            val content = awaitItem() as DashboardState.Content
            assertEquals(3, content.overview.totalInspections)
            assertEquals(1, content.overview.pendingIssues)
            assertEquals(2, issueRepository.observeIssuesCalls)
        }
    }

    private fun issue(
        id: String,
        status: MaintenanceIssueStatus,
    ): MaintenanceIssue {
        return MaintenanceIssue(
            id = IssueId(id),
            inspectionId = InspectionTestFixtures.computerLab.id,
            assetId = InspectionTestFixtures.asset1Id,
            severity = IssueSeverity.CRITICAL,
            title = "Issue $id",
            status = status,
            createdAtMillis = 0L,
        )
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
        inspectionRepository: RecordingInspectionRepository = RecordingInspectionRepository(
            initialSummaries = initialSummaries,
        ),
        assetRepository: FakeAssetRepository = FakeAssetRepository(),
        templateRepository: FakeTemplateRepository = fakeTemplateRepository(),
        issueRepository: IssueRepository = FakeIssueRepository(),
        navigator: FakeNavigator = FakeNavigator(DashboardScreen),
    ): DashboardPresenter {
        return DashboardPresenter(
            observeInspectionSummaries = ObserveInspectionSummariesUseCase(inspectionRepository),
            observeAssets = ObserveAssetsUseCase(assetRepository),
            observeTemplates = ObserveTemplatesUseCase(templateRepository),
            observeIssues = ObserveIssuesUseCase(issueRepository),
            startInspection = StartInspectionUseCase(inspectionRepository, templateRepository),
            navigator = navigator,
        )
    }

    private fun fakeTemplateRepository(
        templateId: TemplateId = InspectionTestFixtures.templateId,
    ): FakeTemplateRepository {
        val template = InspectionTestFixtures.sampleTemplate.copy(id = templateId)
        return FakeTemplateRepository(mapOf(templateId to template))
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

/**
 * Fails the first [failuresBeforeSuccess] subscriptions to `observeIssues`, so a retry —
 * which rebuilds the combined flow — can be observed recovering.
 */
private class FlakyIssueRepository(
    private val failuresBeforeSuccess: Int,
) : IssueRepository {
    private val issues = mutableListOf<MaintenanceIssue>()

    var observeIssuesCalls: Int = 0
        private set

    fun addIssue(issue: MaintenanceIssue) {
        issues.add(issue)
    }

    override fun observeIssues(): Flow<List<MaintenanceIssue>> = flow {
        observeIssuesCalls += 1
        if (observeIssuesCalls <= failuresBeforeSuccess) {
            throw IllegalStateException("Issue stream unavailable")
        }
        emit(issues.toList())
    }

    override fun observeIssue(issueId: IssueId): Flow<MaintenanceIssue?> =
        flowOf(issues.firstOrNull { it.id == issueId })

    override suspend fun getIssue(issueId: IssueId): MaintenanceIssue? =
        issues.firstOrNull { it.id == issueId }

    override suspend fun getIssuesForInspection(inspectionId: InspectionId): List<MaintenanceIssue> =
        issues.filter { it.inspectionId == inspectionId }

    override suspend fun createIssue(issue: MaintenanceIssue): IssueId {
        issues.add(issue)
        return issue.id
    }

    override suspend fun updateIssue(issue: MaintenanceIssue) {
        val index = issues.indexOfFirst { it.id == issue.id }
        if (index >= 0) {
            issues[index] = issue
        }
    }
}
