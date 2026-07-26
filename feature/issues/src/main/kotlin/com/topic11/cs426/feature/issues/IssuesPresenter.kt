package com.topic11.cs426.feature.issues

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.topic11.cs426.core.designsystem.StatusTone
import com.topic11.cs426.core.navigation.IssueDetailScreen
import com.topic11.cs426.domain.model.AssetId
import com.topic11.cs426.domain.model.AssetSummary
import com.topic11.cs426.domain.model.InspectionSummary
import com.topic11.cs426.domain.model.IssueId
import com.topic11.cs426.domain.model.IssueSeverity
import com.topic11.cs426.domain.model.MaintenanceIssue
import com.topic11.cs426.domain.model.MaintenanceIssueStatus
import com.topic11.cs426.domain.usecase.IssueStatusUpdateResult
import com.topic11.cs426.domain.usecase.ObserveAssetsUseCase
import com.topic11.cs426.domain.usecase.ObserveInspectionSummariesUseCase
import com.topic11.cs426.domain.usecase.ObserveIssueUseCase
import com.topic11.cs426.domain.usecase.ObserveIssuesUseCase
import com.topic11.cs426.domain.usecase.UpdateIssueStatusUseCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal class IssuesPresenter(
    private val observeIssues: ObserveIssuesUseCase,
    private val observeAssets: ObserveAssetsUseCase,
    private val observeInspectionSummaries: ObserveInspectionSummariesUseCase,
    private val navigator: Navigator,
) : Presenter<IssuesState> {
    @Composable
    override fun present(): IssuesState {
        var selectedFilter by remember { mutableStateOf(IssueFilterUi.All) }
        var retryToken by remember { mutableIntStateOf(0) }

        val eventSink = remember(navigator) {
            { event: IssuesEvent ->
                when (event) {
                    IssuesEvent.BackSelected -> navigator.pop()
                    is IssuesEvent.FilterSelected -> selectedFilter = event.filter
                    is IssuesEvent.IssueSelected -> navigator.goTo(IssueDetailScreen(event.issueId.value))
                    IssuesEvent.RetrySelected -> retryToken += 1
                }
                Unit
            }
        }

        val state by remember(
            observeIssues,
            observeAssets,
            observeInspectionSummaries,
            selectedFilter,
            retryToken,
            eventSink,
        ) {
            combine(
                observeIssues(),
                observeAssets(),
                observeInspectionSummaries(),
            ) { issues, assets, inspections ->
                val uiIssues = issues.map { issue ->
                    issue.toListItemUi(
                        assets = assets,
                        inspections = inspections,
                    )
                }
                val filtered = uiIssues.filter { issue -> selectedFilter.matches(issue) }
                when {
                    filtered.isEmpty() -> IssuesState.Empty(
                        filters = issueFilters,
                        selectedFilter = selectedFilter,
                        eventSink = eventSink,
                    )
                    else -> IssuesState.Content(
                        issues = filtered,
                        filters = issueFilters,
                        selectedFilter = selectedFilter,
                        eventSink = eventSink,
                    )
                }
            }.catch {
                emit(
                    IssuesState.Error(
                        message = "Issues could not be loaded.",
                        eventSink = eventSink,
                    ),
                )
            }
        }.collectAsState(initial = IssuesState.Loading)

        return state
    }
}

internal class IssueDetailPresenter(
    private val screen: IssueDetailScreen,
    private val observeIssue: ObserveIssueUseCase,
    private val observeAssets: ObserveAssetsUseCase,
    private val observeInspectionSummaries: ObserveInspectionSummariesUseCase,
    private val updateIssueStatus: UpdateIssueStatusUseCase,
    private val navigator: Navigator,
) : Presenter<IssueDetailState> {
    @Composable
    override fun present(): IssueDetailState {
        val issueId = remember(screen.issueId) { IssueId(screen.issueId) }
        val coroutineScope = rememberCoroutineScope()
        var isSaving by remember(issueId) { mutableStateOf(false) }
        var updateMessage by remember(issueId) { mutableStateOf<String?>(null) }

        val eventSink = remember(issueId, navigator, coroutineScope, updateIssueStatus) {
            { event: IssueDetailEvent ->
                when (event) {
                    IssueDetailEvent.BackSelected -> navigator.pop()
                    is IssueDetailEvent.StatusChangeSelected -> {
                        coroutineScope.launch {
                            try {
                                isSaving = true
                                updateMessage = null
                                when (val result = updateIssueStatus(issueId, event.status)) {
                                    is IssueStatusUpdateResult.Success -> {
                                        updateMessage = "Status updated."
                                    }
                                    IssueStatusUpdateResult.MissingIssue -> {
                                        updateMessage = "Issue could not be found."
                                    }
                                    is IssueStatusUpdateResult.InvalidTransition -> {
                                        updateMessage = "That status change is not available."
                                    }
                                    IssueStatusUpdateResult.UpdateFailed -> {
                                        updateMessage = "Issue status could not be updated."
                                    }
                                }
                                isSaving = false
                            } catch (exception: Exception) {
                                if (exception is CancellationException) throw exception
                                isSaving = false
                                updateMessage = "Issue status could not be updated."
                            }
                        }
                    }
                }
                Unit
            }
        }

        val state by remember(
            observeIssue,
            observeAssets,
            observeInspectionSummaries,
            issueId,
            isSaving,
            updateMessage,
            eventSink,
        ) {
            combine(
                observeIssue(issueId),
                observeAssets(),
                observeInspectionSummaries(),
            ) { issue, assets, inspections ->
                if (issue == null) {
                    IssueDetailState.Missing(eventSink)
                } else {
                    IssueDetailState.Content(
                        issue = issue.toDetailUi(assets, inspections),
                        allowedTransitions = updateIssueStatus
                            .allowedNextStatuses(issue.status)
                            .map { status -> IssueStatusActionUi(status, status.actionLabel()) },
                        isSaving = isSaving,
                        updateMessage = updateMessage,
                        eventSink = eventSink,
                    )
                }
            }.catch {
                emit(IssueDetailState.Missing(eventSink))
            }
        }.collectAsState(initial = IssueDetailState.Loading)

        return state
    }
}

private fun IssueFilterUi.matches(issue: IssueListItemUi): Boolean = when (this) {
    IssueFilterUi.All -> true
    IssueFilterUi.Active -> issue.status == MaintenanceIssueStatus.OPEN ||
        issue.status == MaintenanceIssueStatus.IN_PROGRESS
    IssueFilterUi.Resolved -> issue.status == MaintenanceIssueStatus.RESOLVED ||
        issue.status == MaintenanceIssueStatus.CLOSED
    IssueFilterUi.Critical -> issue.severity == IssueSeverity.CRITICAL
}

private fun MaintenanceIssue.toListItemUi(
    assets: List<AssetSummary>,
    inspections: List<InspectionSummary>,
): IssueListItemUi =
    IssueListItemUi(
        id = id,
        title = title,
        severity = severity,
        severityLabel = severity.displayLabel(),
        status = status,
        statusLabel = status.displayLabel(),
        statusTone = status.statusTone(),
        assetLabel = assets.labelFor(assetId),
        inspectionLabel = inspections.firstOrNull { it.id == inspectionId }?.title
            ?: "Inspection ${inspectionId.value}",
        createdLabel = createdAtMillis.dateLabel("Created"),
        updatedLabel = updatedAtMillis.dateLabel("Updated"),
    )

private fun MaintenanceIssue.toDetailUi(
    assets: List<AssetSummary>,
    inspections: List<InspectionSummary>,
): IssueDetailItemUi =
    IssueDetailItemUi(
        id = id,
        title = title,
        description = description?.takeIf { it.isNotBlank() } ?: "No description recorded.",
        severityLabel = severity.displayLabel(),
        status = status,
        statusLabel = status.displayLabel(),
        statusTone = status.statusTone(),
        assetLabel = assets.labelFor(assetId),
        inspectionLabel = inspections.firstOrNull { it.id == inspectionId }?.title
            ?: "Inspection ${inspectionId.value}",
        checklistItemLabel = checklistItemId?.value ?: "Inspection-level issue",
        createdLabel = createdAtMillis.dateLabel("Created"),
        updatedLabel = updatedAtMillis.dateLabel("Updated"),
    )

private fun List<AssetSummary>.labelFor(assetId: AssetId): String =
    firstOrNull { asset -> asset.id == assetId }?.let { asset ->
        val locationName = asset.locationName
        if (locationName.isNullOrBlank()) {
            asset.name
        } else {
            "${asset.name} - $locationName"
        }
    } ?: "Asset ${assetId.value}"

private fun IssueSeverity.displayLabel(): String = when (this) {
    IssueSeverity.CRITICAL -> "Critical"
    IssueSeverity.MAJOR -> "Major"
    IssueSeverity.MINOR -> "Minor"
    IssueSeverity.OBSERVATION -> "Observation"
}

private fun MaintenanceIssueStatus.displayLabel(): String = when (this) {
    MaintenanceIssueStatus.OPEN -> "Open"
    MaintenanceIssueStatus.IN_PROGRESS -> "In progress"
    MaintenanceIssueStatus.RESOLVED -> "Resolved"
    MaintenanceIssueStatus.CLOSED -> "Closed"
}

private fun MaintenanceIssueStatus.actionLabel(): String = when (this) {
    MaintenanceIssueStatus.OPEN -> "Reopen"
    MaintenanceIssueStatus.IN_PROGRESS -> "Start work"
    MaintenanceIssueStatus.RESOLVED -> "Mark resolved"
    MaintenanceIssueStatus.CLOSED -> "Close issue"
}

private fun MaintenanceIssueStatus.statusTone(): StatusTone = when (this) {
    MaintenanceIssueStatus.OPEN -> StatusTone.Warning
    MaintenanceIssueStatus.IN_PROGRESS -> StatusTone.InProgress
    MaintenanceIssueStatus.RESOLVED -> StatusTone.Success
    MaintenanceIssueStatus.CLOSED -> StatusTone.Neutral
}

private fun Long.dateLabel(prefix: String): String =
    "$prefix ${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(this))}"
