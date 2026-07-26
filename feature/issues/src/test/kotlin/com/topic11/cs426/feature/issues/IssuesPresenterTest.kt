package com.topic11.cs426.feature.issues

import app.cash.turbine.ReceiveTurbine
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import com.topic11.cs426.core.navigation.DashboardScreen
import com.topic11.cs426.core.navigation.IssueDetailScreen
import com.topic11.cs426.core.navigation.IssuesScreen
import com.topic11.cs426.core.testing.FakeAssetRepository
import com.topic11.cs426.core.testing.FakeIssueRepository
import com.topic11.cs426.core.testing.InspectionTestFixtures
import com.topic11.cs426.core.testing.RecordingInspectionRepository
import com.topic11.cs426.domain.model.IssueId
import com.topic11.cs426.domain.model.IssueSeverity
import com.topic11.cs426.domain.model.MaintenanceIssue
import com.topic11.cs426.domain.model.MaintenanceIssueStatus
import com.topic11.cs426.domain.usecase.ObserveAssetsUseCase
import com.topic11.cs426.domain.usecase.ObserveInspectionSummariesUseCase
import com.topic11.cs426.domain.usecase.ObserveIssueUseCase
import com.topic11.cs426.domain.usecase.ObserveIssuesUseCase
import com.topic11.cs426.domain.usecase.UpdateIssueStatusUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IssuesPresenterTest {
    @Test
    fun `list presents persisted issues and opens detail`() = runTest {
        val issueRepository = FakeIssueRepository().apply { addIssue(issue()) }
        val navigator = FakeNavigator(DashboardScreen, IssuesScreen)
        val presenter = issuesPresenter(issueRepository, navigator)

        presenter.test {
            assertEquals(IssuesState.Loading, awaitItem())
            val content = awaitItem() as IssuesState.Content

            assertEquals(listOf(IssueId("issue-critical")), content.issues.map { it.id })
            assertEquals("Computer Lab I.44", content.issues.single().assetLabel)
            content.eventSink(IssuesEvent.IssueSelected(IssueId("issue-critical")))

            assertEquals(IssueDetailScreen("issue-critical"), navigator.awaitNextScreen())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filter hides non matching issues`() = runTest {
        val issueRepository = FakeIssueRepository().apply {
            addIssue(issue())
            addIssue(
                issue(
                    id = IssueId("issue-minor"),
                    severity = IssueSeverity.MINOR,
                    status = MaintenanceIssueStatus.RESOLVED,
                ),
            )
        }
        val presenter = issuesPresenter(issueRepository, FakeNavigator(DashboardScreen, IssuesScreen))

        presenter.test {
            awaitItem()
            val content = awaitItem() as IssuesState.Content

            content.eventSink(IssuesEvent.FilterSelected(IssueFilterUi.Critical))
            val filtered = awaitItem() as IssuesState.Content

            assertEquals(listOf(IssueId("issue-critical")), filtered.issues.map { it.id })
            assertEquals(IssueFilterUi.Critical, filtered.selectedFilter)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `detail valid transition persists status`() = runTest {
        val issueRepository = FakeIssueRepository().apply { addIssue(issue()) }
        val screen = IssueDetailScreen("issue-critical")
        val presenter = issueDetailPresenter(issueRepository, FakeNavigator(DashboardScreen, IssuesScreen, screen), screen)

        presenter.test {
            val content = awaitDetailContent()

            content.eventSink(IssueDetailEvent.StatusChangeSelected(MaintenanceIssueStatus.IN_PROGRESS))
            advanceUntilIdle()
            val updated = awaitDetailMessage()

            assertEquals(MaintenanceIssueStatus.IN_PROGRESS, issueRepository.getIssue(IssueId("issue-critical"))?.status)
            assertEquals("Status updated.", updated.updateMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `detail invalid transition reports update error`() = runTest {
        val issueRepository = FakeIssueRepository().apply { addIssue(issue()) }
        val screen = IssueDetailScreen("issue-critical")
        val presenter = issueDetailPresenter(issueRepository, FakeNavigator(DashboardScreen, IssuesScreen, screen), screen)

        presenter.test {
            val content = awaitDetailContent()

            content.eventSink(IssueDetailEvent.StatusChangeSelected(MaintenanceIssueStatus.RESOLVED))
            advanceUntilIdle()
            val updated = awaitDetailMessage()

            assertEquals(MaintenanceIssueStatus.OPEN, issueRepository.getIssue(IssueId("issue-critical"))?.status)
            assertEquals("That status change is not available.", updated.updateMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `missing detail supports back navigation`() = runTest {
        val screen = IssueDetailScreen("missing-issue")
        val navigator = FakeNavigator(DashboardScreen, IssuesScreen, screen)
        val presenter = issueDetailPresenter(FakeIssueRepository(), navigator, screen)

        presenter.test {
            var missing = awaitItem()
            while (missing !is IssueDetailState.Missing) {
                missing = awaitItem()
            }

            (missing as IssueDetailState.Missing).eventSink(IssueDetailEvent.BackSelected)
            assertEquals(screen, navigator.awaitPop().poppedScreen)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun issuesPresenter(
        issueRepository: FakeIssueRepository,
        navigator: FakeNavigator,
    ) = IssuesPresenter(
        observeIssues = ObserveIssuesUseCase(issueRepository),
        observeAssets = ObserveAssetsUseCase(FakeAssetRepository()),
        observeInspectionSummaries = ObserveInspectionSummariesUseCase(RecordingInspectionRepository()),
        navigator = navigator,
    )

    private fun issueDetailPresenter(
        issueRepository: FakeIssueRepository,
        navigator: FakeNavigator,
        screen: IssueDetailScreen,
    ) = IssueDetailPresenter(
        screen = screen,
        observeIssue = ObserveIssueUseCase(issueRepository),
        observeAssets = ObserveAssetsUseCase(FakeAssetRepository()),
        observeInspectionSummaries = ObserveInspectionSummariesUseCase(RecordingInspectionRepository()),
        updateIssueStatus = UpdateIssueStatusUseCase(issueRepository, clock = { 4_000L }),
        navigator = navigator,
    )

    private suspend fun ReceiveTurbine<IssueDetailState>.awaitDetailContent(): IssueDetailState.Content {
        var state = awaitItem()
        while (state !is IssueDetailState.Content) {
            state = awaitItem()
        }
        return state
    }

    private suspend fun ReceiveTurbine<IssueDetailState>.awaitDetailMessage(): IssueDetailState.Content {
        var state = awaitDetailContent()
        while (state.updateMessage == null) {
            state = awaitDetailContent()
        }
        return state
    }

    private fun issue(
        id: IssueId = IssueId("issue-critical"),
        severity: IssueSeverity = IssueSeverity.CRITICAL,
        status: MaintenanceIssueStatus = MaintenanceIssueStatus.OPEN,
    ) = MaintenanceIssue(
        id = id,
        inspectionId = InspectionTestFixtures.computerLab.id,
        assetId = InspectionTestFixtures.asset1Id,
        checklistItemId = InspectionTestFixtures.itemRequiredId,
        severity = severity,
        title = "Critical failure: Fire extinguisher present",
        status = status,
        createdAtMillis = 1_000L,
        updatedAtMillis = 2_000L,
    )
}
