package com.topic11.cs426.feature.reports

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.domain.model.ReportFormat
import com.topic11.cs426.domain.model.ReportHistoryEntry
import com.topic11.cs426.domain.model.ReportId

@Composable
internal fun ReportsUi(
    state: ReportsState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("reports-root"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FieldFlowTopAppBar(
                title = "Reports",
                onBackClick = state.eventSinkOrNull()?.let { eventSink ->
                    { eventSink(ReportsEvent.BackSelected) }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .testTag("reports-content"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state) {
                ReportsState.Loading -> item {
                    LoadingContent(label = "Loading reports")
                }
                is ReportsState.Empty -> item {
                    EmptyState(
                        title = "No reports yet",
                        message = "Complete an inspection to make it available for export.",
                    )
                }
                is ReportsState.Error -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        EmptyState(
                            title = "Reports unavailable",
                            message = state.message,
                        )
                        OutlinedButton(onClick = { state.eventSink(ReportsEvent.RetrySelected) }) {
                            Text("Retry")
                        }
                    }
                }
                is ReportsState.Content -> {
                    item {
                        ReportsSearchField(
                            query = state.query,
                            onQueryChanged = { state.eventSink(ReportsEvent.SearchQueryChanged(it)) },
                            onClearQuery = { state.eventSink(ReportsEvent.SearchCleared) },
                        )
                    }
                    if (state.hasNoSearchResults) {
                        item {
                            EmptyState(
                                title = "No reports match this search",
                                message = "Clear the search to view all report candidates and exports.",
                            )
                        }
                    } else {
                        if (state.candidates.isNotEmpty()) {
                            item { SectionHeader("Ready to export") }
                            items(
                                items = state.candidates,
                                key = { candidate -> candidate.inspectionId.value },
                            ) { candidate ->
                                ReportCandidateCard(
                                    candidate = candidate,
                                    onClick = {
                                        state.eventSink(ReportsEvent.CandidateSelected(candidate.inspectionId))
                                    },
                                )
                            }
                        }
                        if (state.history.isNotEmpty()) {
                            item { SectionHeader("Export history") }
                            items(
                                items = state.history,
                                key = { history -> history.entry.id.value },
                            ) { history ->
                                ReportHistoryCard(
                                    history = history,
                                    onOpen = { state.eventSink(ReportsEvent.OpenHistorySelected(history.entry)) },
                                    onShare = { state.eventSink(ReportsEvent.ShareHistorySelected(history.entry)) },
                                )
                            }
                        }
                    }
                    state.actionMessage?.let { message ->
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
internal fun ReportDetailUi(
    state: ReportDetailState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("report-detail-root"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FieldFlowTopAppBar(
                title = "Report details",
                onBackClick = state.eventSinkOrNull()?.let { eventSink ->
                    { eventSink(ReportDetailEvent.BackSelected) }
                },
            )
        },
    ) { innerPadding ->
        when (state) {
            ReportDetailState.Loading -> LoadingContent(
                label = "Loading report",
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(20.dp),
            )
            is ReportDetailState.Error -> {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    EmptyState(
                        title = state.title,
                        message = state.message,
                    )
                    OutlinedButton(onClick = { state.eventSink(ReportDetailEvent.RetrySelected) }) {
                        Text("Retry")
                    }
                }
            }
            is ReportDetailState.Content -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item { ReportSummaryCard(state.report) }
                    item { ExportActions(state.exportState, state.eventSink) }
                    items(
                        items = state.report.sections,
                        key = { section -> section.title },
                    ) { section ->
                        ReportSectionCard(section)
                    }
                    item { ReportIssuesCard(state.report.issues) }
                }
            }
        }
    }
}

@Composable
private fun ReportsSearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Search reports") },
        singleLine = true,
        trailingIcon = {
            if (query.isNotBlank()) {
                TextButton(onClick = onClearQuery) {
                    Text("Clear")
                }
            }
        },
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
    )
}

@Composable
private fun ReportCandidateCard(
    candidate: ReportCandidateUi,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("report-candidate-${candidate.inspectionId.value}"),
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
                    text = candidate.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusBadge(candidate.statusLabel, candidate.statusTone)
            }
            Text(
                text = candidate.progressLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReportHistoryCard(
    history: ReportHistoryItemUi,
    onOpen: () -> Unit,
    onShare: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("report-history-${history.entry.id.value}"),
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
            Text(
                text = history.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${history.formatLabel} - ${history.generatedLabel} - ${history.sizeLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Open report")
                }
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Share report")
                }
            }
        }
    }
}

@Composable
private fun ReportSummaryCard(report: ReportDetailUi) {
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = report.assetName,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = report.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DetailRow("Template", report.templateName)
            DetailRow("Score", report.scoreLabel)
            DetailRow("Completed", report.completedLabel.removePrefix("Completed "))
            DetailRow("Generated", report.generatedLabel.removePrefix("Generated "))
        }
    }
}

@Composable
private fun ExportActions(
    exportState: ReportExportUiState,
    eventSink: (ReportDetailEvent) -> Unit,
) {
    val isExporting = exportState is ReportExportUiState.Exporting
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { eventSink(ReportDetailEvent.ExportSelected(ReportFormat.JSON)) },
                enabled = !isExporting,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (isExporting) "Exporting" else "Export JSON")
            }
            Button(
                onClick = { eventSink(ReportDetailEvent.ExportSelected(ReportFormat.PDF)) },
                enabled = !isExporting,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (isExporting) "Exporting" else "Export PDF")
            }
        }
        when (exportState) {
            ReportExportUiState.Idle -> Unit
            is ReportExportUiState.Exporting -> Text(
                text = "Exporting ${exportState.format.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            is ReportExportUiState.Failed -> Text(
                text = exportState.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            is ReportExportUiState.Succeeded -> {
                Text(
                    text = exportState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { eventSink(ReportDetailEvent.OpenLastExportSelected) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Open report")
                    }
                    OutlinedButton(
                        onClick = { eventSink(ReportDetailEvent.ShareLastExportSelected) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Share report")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportSectionCard(section: ReportSectionUi) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
            )
            section.items.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "${item.semanticsLabel} - ${item.answerLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    item.noteLabel?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item.evidenceLabel?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportIssuesCard(issues: List<ReportIssueUi>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Issues",
                style = MaterialTheme.typography.titleMedium,
            )
            if (issues.isEmpty()) {
                Text(
                    text = "No maintenance issues recorded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                issues.forEach { issue ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = issue.title,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "${issue.severityLabel} - ${issue.statusLabel}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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

private fun ReportsState.eventSinkOrNull(): ((ReportsEvent) -> Unit)? = when (this) {
    ReportsState.Loading -> null
    is ReportsState.Empty -> eventSink
    is ReportsState.Content -> eventSink
    is ReportsState.Error -> eventSink
}

private fun ReportDetailState.eventSinkOrNull(): ((ReportDetailEvent) -> Unit)? = when (this) {
    ReportDetailState.Loading -> null
    is ReportDetailState.Error -> eventSink
    is ReportDetailState.Content -> eventSink
}

@Preview(name = "Reports", showBackground = true, widthDp = 411, heightDp = 760)
@Composable
private fun ReportsPreview() {
    FieldFlowTheme {
        ReportsUi(
            state = ReportsState.Content(
                candidates = listOf(previewCandidate),
                history = listOf(previewHistory),
                query = "",
                hasNoSearchResults = false,
                actionMessage = null,
                eventSink = {},
            ),
        )
    }
}

@Preview(
    name = "Report Detail Dark",
    showBackground = true,
    widthDp = 411,
    heightDp = 760,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ReportDetailPreview() {
    FieldFlowTheme(darkTheme = true) {
        ReportDetailUi(
            state = ReportDetailState.Content(
                report = previewReportDetail,
                exportState = ReportExportUiState.Idle,
                eventSink = {},
            ),
        )
    }
}

private val previewCandidate = ReportCandidateUi(
    inspectionId = InspectionId("inspection-preview"),
    title = "Computer Lab I.44",
    statusLabel = "Completed",
    statusTone = StatusTone.Success,
    progressLabel = "7 of 7 items",
)

private val previewHistoryEntry = ReportHistoryEntry(
    id = ReportId("report-preview"),
    inspectionId = InspectionId("inspection-preview"),
    format = ReportFormat.PDF,
    generatedAtMillis = 1_800_000L,
    displayFilename = "fieldflow-inspection-preview-report-preview.pdf",
    storageKey = "reports/fieldflow-inspection-preview-report-preview.pdf",
    mimeType = "application/pdf",
    sizeBytes = 24_000L,
)

private val previewHistory = ReportHistoryItemUi(
    entry = previewHistoryEntry,
    title = previewHistoryEntry.displayFilename,
    formatLabel = "PDF",
    generatedLabel = "Generated 2026-07-26",
    sizeLabel = "23 KB",
)

private val previewReportDetail = ReportDetailUi(
    inspectionId = InspectionId("inspection-preview"),
    assetName = "Computer Lab I.44",
    templateName = "Lab Safety Checklist",
    summary = "Inspection report for Computer Lab I.44",
    scoreLabel = "6/10",
    completedLabel = "Completed 2026-07-26",
    generatedLabel = "Generated 2026-07-26",
    sections = listOf(
        ReportSectionUi(
            title = "Safety",
            items = listOf(
                ReportItemUi(
                    title = "Fire extinguisher present",
                    semanticsLabel = "Required - Critical - Weight 5",
                    answerLabel = "Fail",
                    noteLabel = "Note: Missing at rear door.",
                    evidenceLabel = "1 evidence reference(s)",
                ),
            ),
        ),
    ),
    issues = listOf(
        ReportIssueUi(
            title = "Critical failure: Fire extinguisher present",
            severityLabel = "Critical",
            statusLabel = "Open",
        ),
    ),
)
