package com.topic11.cs426.feature.issues

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.topic11.cs426.core.designsystem.EmptyState
import com.topic11.cs426.core.designsystem.FieldFlowTheme
import com.topic11.cs426.core.designsystem.FieldFlowTopAppBar
import com.topic11.cs426.core.designsystem.LoadingContent
import com.topic11.cs426.core.designsystem.StatusBadge
import com.topic11.cs426.core.designsystem.StatusTone
import com.topic11.cs426.domain.model.IssueId
import com.topic11.cs426.domain.model.IssueSeverity
import com.topic11.cs426.domain.model.MaintenanceIssueStatus

@Composable
internal fun IssuesUi(
    state: IssuesState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("issues-root"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FieldFlowTopAppBar(
                title = "Issues",
                onBackClick = state.eventSinkOrNull()?.let { eventSink ->
                    { eventSink(IssuesEvent.BackSelected) }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("issues-content"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state) {
                IssuesState.Loading -> item {
                    LoadingContent(label = "Loading issues")
                }
                is IssuesState.Empty -> {
                    item {
                        IssueFilters(
                            filters = state.filters,
                            selectedFilter = state.selectedFilter,
                            onSelected = { state.eventSink(IssuesEvent.FilterSelected(it)) },
                        )
                    }
                    item {
                        EmptyState(
                            title = "No issues",
                            message = "Maintenance issues from completed inspections will appear here.",
                        )
                    }
                }
                is IssuesState.Content -> {
                    item {
                        IssueFilters(
                            filters = state.filters,
                            selectedFilter = state.selectedFilter,
                            onSelected = { state.eventSink(IssuesEvent.FilterSelected(it)) },
                        )
                    }
                    items(
                        items = state.issues,
                        key = { issue -> issue.id.value },
                    ) { issue ->
                        IssueListCard(
                            issue = issue,
                            onClick = { state.eventSink(IssuesEvent.IssueSelected(issue.id)) },
                        )
                    }
                }
                is IssuesState.Error -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        EmptyState(
                            title = "Issues unavailable",
                            message = state.message,
                        )
                        OutlinedButton(onClick = { state.eventSink(IssuesEvent.RetrySelected) }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun IssueDetailUi(
    state: IssueDetailState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("issue-detail-root"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FieldFlowTopAppBar(
                title = "Issue details",
                onBackClick = state.eventSinkOrNull()?.let { eventSink ->
                    { eventSink(IssueDetailEvent.BackSelected) }
                },
            )
        },
    ) { innerPadding ->
        when (state) {
            IssueDetailState.Loading -> {
                LoadingContent(
                    label = "Loading issue",
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(20.dp),
                )
            }
            is IssueDetailState.Missing -> {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(20.dp),
                ) {
                    EmptyState(
                        title = "Issue not found",
                        message = "This issue may have been removed.",
                    )
                }
            }
            is IssueDetailState.Content -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        IssueDetailCard(issue = state.issue)
                    }
                    item {
                        StatusActions(
                            actions = state.allowedTransitions,
                            isSaving = state.isSaving,
                            eventSink = state.eventSink,
                        )
                    }
                    state.updateMessage?.let { message ->
                        item {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IssueFilters(
    filters: List<IssueFilterUi>,
    selectedFilter: IssueFilterUi,
    onSelected: (IssueFilterUi) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(filters) { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onSelected(filter) },
                label = { Text(filter.label) },
            )
        }
    }
}

@Composable
private fun IssueListCard(
    issue: IssueListItemUi,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("issue-card-${issue.id.value}"),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = issue.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusBadge(issue.statusLabel, issue.statusTone)
            }
            Text(
                text = "${issue.severityLabel} - ${issue.assetLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${issue.inspectionLabel} - ${issue.updatedLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IssueDetailCard(issue: IssueDetailItemUi) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = issue.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                )
                StatusBadge(issue.statusLabel, issue.statusTone)
            }
            Text(
                text = issue.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DetailRow("Severity", issue.severityLabel)
            DetailRow("Asset", issue.assetLabel)
            DetailRow("Inspection", issue.inspectionLabel)
            DetailRow("Checklist item", issue.checklistItemLabel)
            DetailRow("Created", issue.createdLabel.removePrefix("Created "))
            DetailRow("Updated", issue.updatedLabel.removePrefix("Updated "))
        }
    }
}

@Composable
private fun StatusActions(
    actions: List<IssueStatusActionUi>,
    isSaving: Boolean,
    eventSink: (IssueDetailEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Update status",
            style = MaterialTheme.typography.titleMedium,
        )
        if (actions.isEmpty()) {
            Text(
                text = "No further status changes are available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            actions.forEach { action ->
                Button(
                    onClick = {
                        eventSink(IssueDetailEvent.StatusChangeSelected(action.status))
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (isSaving) "Updating" else action.label)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun IssuesState.eventSinkOrNull(): ((IssuesEvent) -> Unit)? = when (this) {
    IssuesState.Loading -> null
    is IssuesState.Empty -> eventSink
    is IssuesState.Content -> eventSink
    is IssuesState.Error -> eventSink
}

private fun IssueDetailState.eventSinkOrNull(): ((IssueDetailEvent) -> Unit)? = when (this) {
    IssueDetailState.Loading -> null
    is IssueDetailState.Missing -> eventSink
    is IssueDetailState.Content -> eventSink
}

@Preview(name = "Issues", showBackground = true, widthDp = 411, heightDp = 760)
@Composable
private fun IssuesPreview() {
    FieldFlowTheme {
        IssuesUi(
            state = IssuesState.Content(
                issues = previewIssues,
                filters = issueFilters,
                selectedFilter = IssueFilterUi.All,
                eventSink = {},
            ),
        )
    }
}

@Preview(
    name = "Issue Detail Dark",
    showBackground = true,
    widthDp = 411,
    heightDp = 760,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun IssueDetailPreview() {
    FieldFlowTheme(darkTheme = true) {
        IssueDetailUi(
            state = IssueDetailState.Content(
                issue = IssueDetailItemUi(
                    id = IssueId("issue-preview"),
                    title = "Critical failure: Fire extinguisher present",
                    description = "Evidence was attached during inspection.",
                    severityLabel = "Critical",
                    status = MaintenanceIssueStatus.OPEN,
                    statusLabel = "Open",
                    statusTone = StatusTone.Warning,
                    assetLabel = "Computer Lab I.44 - HCMUS",
                    inspectionLabel = "Computer Lab I.44",
                    checklistItemLabel = "sample-item-0",
                    createdLabel = "Created 2026-07-26",
                    updatedLabel = "Updated 2026-07-26",
                ),
                allowedTransitions = listOf(
                    IssueStatusActionUi(MaintenanceIssueStatus.IN_PROGRESS, "Start work"),
                ),
                isSaving = false,
                updateMessage = null,
                eventSink = {},
            ),
        )
    }
}

private val previewIssues = listOf(
    IssueListItemUi(
        id = IssueId("issue-preview"),
        title = "Critical failure: Fire extinguisher present",
        severity = IssueSeverity.CRITICAL,
        severityLabel = "Critical",
        status = MaintenanceIssueStatus.OPEN,
        statusLabel = "Open",
        statusTone = StatusTone.Warning,
        assetLabel = "Computer Lab I.44 - HCMUS",
        inspectionLabel = "Computer Lab I.44",
        createdLabel = "Created 2026-07-26",
        updatedLabel = "Updated 2026-07-26",
    ),
)
