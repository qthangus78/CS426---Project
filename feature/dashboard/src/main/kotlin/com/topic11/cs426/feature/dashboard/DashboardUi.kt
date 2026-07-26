package com.topic11.cs426.feature.dashboard

import android.content.res.Configuration
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import com.topic11.cs426.core.designsystem.InspectionSummaryCard
import com.topic11.cs426.core.designsystem.LoadingContent
import com.topic11.cs426.core.designsystem.StatusTone
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.feature.dashboard.component.DashboardAboutDialog
import com.topic11.cs426.feature.dashboard.component.ContinueInspectionCard
import com.topic11.cs426.feature.dashboard.component.DashboardOverview
import com.topic11.cs426.feature.dashboard.component.DashboardQuickActions
import com.topic11.cs426.feature.dashboard.component.DashboardSectionHeader
import com.topic11.cs426.feature.dashboard.component.DashboardTopArea
import com.topic11.cs426.feature.dashboard.component.InspectionFilterRow

@Composable
internal fun DashboardUi(
    state: DashboardState,
    modifier: Modifier = Modifier,
) {
    val aboutEventSink = when (state) {
        DashboardState.Loading -> null
        is DashboardState.Empty -> state.eventSink
        is DashboardState.Content -> state.eventSink
        is DashboardState.Error -> state.eventSink
    }
    val isAboutVisible = when (state) {
        DashboardState.Loading -> false
        is DashboardState.Empty -> state.isAboutVisible
        is DashboardState.Content -> state.isAboutVisible
        is DashboardState.Error -> false
    }
    val startInspection = when (state) {
        DashboardState.Loading -> null
        is DashboardState.Empty -> state.startInspection
        is DashboardState.Content -> state.startInspection
        is DashboardState.Error -> null
    }

    Scaffold(
        modifier = modifier.testTag("dashboard-root"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FieldFlowTopAppBar(
                title = "Dashboard",
                actions = {
                    aboutEventSink?.let { eventSink ->
                        TextButton(onClick = { eventSink(DashboardEvent.SettingsSelected) }) {
                            Text("Settings")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .testTag("dashboard-content"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                DashboardTopArea(
                    onAboutClick = aboutEventSink?.let { eventSink ->
                        { eventSink(DashboardEvent.AboutSelected) }
                    },
                )
            }

            when (state) {
                DashboardState.Loading -> {
                    item {
                        LoadingContent(
                            label = "Loading inspections",
                            modifier = Modifier.testTag("dashboard-loading"),
                        )
                    }
                }

                is DashboardState.Error -> {
                    item {
                        Column(
                            modifier = Modifier.testTag("dashboard-error"),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            EmptyState(
                                title = "Dashboard unavailable",
                                message = state.message,
                            )
                            OutlinedButton(
                                onClick = { state.eventSink(DashboardEvent.RetrySelected) },
                                modifier = Modifier.testTag("dashboard-retry"),
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }

                is DashboardState.Empty -> {
                    item {
                        DashboardOverview(overview = state.overview)
                    }
                    item {
                        DashboardQuickActions(eventSink = state.eventSink)
                    }
                    item {
                        InspectionFilterRow(
                            selectedFilter = state.selectedFilter,
                            eventSink = state.eventSink,
                        )
                    }
                    item {
                        DashboardSectionHeader(
                            title = "Inspections",
                            countLabel = "0 shown",
                            modifier = Modifier.testTag("dashboard-inspection-list"),
                        )
                    }
                    item {
                        EmptyState(
                            title = "No inspections available",
                            message = "Start an inspection to begin tracking field work.",
                            modifier = Modifier.testTag("dashboard-empty"),
                        )
                    }
                }

                is DashboardState.Content -> {
                    state.heroInspection?.let { heroInspection ->
                        item {
                            ContinueInspectionCard(
                                inspection = heroInspection,
                                onClick = {
                                    state.eventSink(
                                        DashboardEvent.InspectionSelected(heroInspection.id),
                                    )
                                },
                            )
                        }
                    }
                    item {
                        DashboardOverview(overview = state.overview)
                    }
                    item {
                        DashboardQuickActions(eventSink = state.eventSink)
                    }
                    item {
                        InspectionFilterRow(
                            selectedFilter = state.selectedFilter,
                            eventSink = state.eventSink,
                        )
                    }
                    item {
                        DashboardSectionHeader(
                            title = "Inspections",
                            countLabel = "${state.filteredInspections.size} shown",
                            modifier = Modifier.testTag("dashboard-inspection-list"),
                        )
                    }
                    if (state.filteredInspections.isEmpty()) {
                        item {
                            EmptyState(
                                title = "No inspections match this filter",
                                message = "Choose another status filter to view available inspection summaries.",
                                modifier = Modifier.testTag("dashboard-filtered-empty"),
                            )
                        }
                    } else {
                        items(
                            items = state.filteredInspections,
                            key = { inspection -> inspection.id.value },
                        ) { inspection ->
                            InspectionSummaryRow(
                                inspection = inspection,
                                onClick = {
                                    state.eventSink(DashboardEvent.InspectionSelected(inspection.id))
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (aboutEventSink != null && isAboutVisible) {
        DashboardAboutDialog(
            onDismiss = {
                aboutEventSink(DashboardEvent.AboutDismissed)
            },
        )
    }
    if (aboutEventSink != null && startInspection?.isVisible == true) {
        StartInspectionDialog(
            state = startInspection,
            eventSink = aboutEventSink,
        )
    }
}

@Composable
private fun InspectionSummaryRow(
    inspection: InspectionSummaryUi,
    onClick: () -> Unit,
) {
    InspectionSummaryCard(
        title = inspection.title,
        statusLabel = inspection.statusLabel,
        statusTone = inspection.statusTone,
        completedItems = inspection.completedItems,
        totalItems = inspection.totalItems,
        progressFraction = inspection.progressFraction,
        actionLabel = "Open",
        onClick = onClick,
        modifier = Modifier.testTag("inspection-card-${inspection.id.value}"),
    )
}

@Composable
private fun StartInspectionDialog(
    state: StartInspectionUi,
    eventSink: (DashboardEvent) -> Unit,
) {
    AlertDialog(
        modifier = Modifier.testTag("start-inspection-dialog"),
        onDismissRequest = { eventSink(DashboardEvent.StartInspectionDismissed) },
        title = { Text("Start inspection") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    text = "Asset",
                    style = MaterialTheme.typography.labelLarge,
                )
                if (state.assets.isEmpty()) {
                    Text(
                        text = "No assets available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.assets.forEach { asset ->
                        StartInspectionOptionRow(
                            title = asset.name,
                            subtitle = asset.subtitle,
                            selected = asset.id == state.selectedAssetId,
                            onClick = {
                                eventSink(DashboardEvent.StartInspectionAssetSelected(asset.id))
                            },
                        )
                    }
                }
                Text(
                    text = "Template revision",
                    style = MaterialTheme.typography.labelLarge,
                )
                if (state.templates.isEmpty()) {
                    Text(
                        text = "No templates available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.templates.forEach { template ->
                        StartInspectionOptionRow(
                            title = template.name,
                            subtitle = template.versionLabel,
                            selected = template.id == state.selectedTemplateId,
                            onClick = {
                                eventSink(DashboardEvent.StartInspectionTemplateSelected(template.id))
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                enabled = state.canConfirm,
                onClick = { eventSink(DashboardEvent.StartInspectionConfirmed) },
                modifier = Modifier.testTag("start-inspection-confirm"),
            ) {
                Text(if (state.isCreating) "Starting..." else "Start")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                enabled = !state.isCreating,
                onClick = { eventSink(DashboardEvent.StartInspectionDismissed) },
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun StartInspectionOptionRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(
    name = "Dashboard Content",
    showBackground = true,
    widthDp = 411,
    heightDp = 891,
)
@Composable
private fun DashboardContentPreview() {
    FieldFlowTheme {
        DashboardUi(
            state = DashboardState.Content(
                overview = previewOverview,
                heroInspection = previewInspections.first(),
                selectedFilter = InspectionFilterUi.ALL,
                filteredInspections = previewInspections,
                isAboutVisible = false,
                startInspection = previewStartInspection,
                eventSink = {},
            ),
        )
    }
}

@Preview(
    name = "Dashboard Loading",
    showBackground = true,
    widthDp = 411,
    heightDp = 700,
)
@Composable
private fun DashboardLoadingPreview() {
    FieldFlowTheme {
        DashboardUi(state = DashboardState.Loading)
    }
}

@Preview(
    name = "Dashboard With Hero",
    showBackground = true,
    widthDp = 411,
    heightDp = 760,
)
@Composable
private fun DashboardWithHeroPreview() {
    FieldFlowTheme {
        DashboardUi(
            state = DashboardState.Content(
                overview = previewOverview,
                heroInspection = previewInspections.first(),
                selectedFilter = InspectionFilterUi.ALL,
                filteredInspections = previewInspections,
                isAboutVisible = false,
                startInspection = previewStartInspection,
                eventSink = {},
            ),
        )
    }
}

@Preview(
    name = "Dashboard Filtered",
    showBackground = true,
    widthDp = 411,
    heightDp = 700,
)
@Composable
private fun DashboardFilteredPreview() {
    val filteredInspections = previewInspections.filter {
        it.filter == InspectionFilterUi.SYNC_PENDING
    }
    FieldFlowTheme {
        DashboardUi(
            state = DashboardState.Content(
                overview = previewOverview,
                heroInspection = previewInspections.first(),
                selectedFilter = InspectionFilterUi.SYNC_PENDING,
                filteredInspections = filteredInspections,
                isAboutVisible = false,
                startInspection = previewStartInspection,
                eventSink = {},
            ),
        )
    }
}

@Preview(
    name = "Dashboard Filtered Empty",
    showBackground = true,
    widthDp = 411,
    heightDp = 700,
)
@Composable
private fun DashboardFilteredEmptyPreview() {
    FieldFlowTheme {
        DashboardUi(
            state = DashboardState.Content(
                overview = previewFilteredEmptyOverview,
                heroInspection = previewInspections.first(),
                selectedFilter = InspectionFilterUi.SYNC_PENDING,
                filteredInspections = emptyList(),
                isAboutVisible = false,
                startInspection = previewStartInspection,
                eventSink = {},
            ),
        )
    }
}

@Preview(
    name = "Dashboard Empty",
    showBackground = true,
    widthDp = 411,
    heightDp = 891,
)
@Composable
private fun DashboardEmptyPreview() {
    FieldFlowTheme {
        DashboardUi(
            state = previewEmptyState(isAboutVisible = false),
        )
    }
}

@Preview(
    name = "Dashboard Error",
    showBackground = true,
    widthDp = 411,
    heightDp = 700,
)
@Composable
private fun DashboardErrorPreview() {
    FieldFlowTheme {
        DashboardUi(
            state = DashboardState.Error(
                message = "Dashboard could not be loaded.",
                eventSink = {},
            ),
        )
    }
}

@Preview(
    name = "Dashboard About Dialog",
    showBackground = true,
    widthDp = 411,
    heightDp = 760,
)
@Composable
private fun DashboardAboutDialogPreview() {
    FieldFlowTheme {
        DashboardUi(
            state = previewContentState(isAboutVisible = true),
        )
    }
}

@Preview(
    name = "Dashboard Dark",
    showBackground = true,
    widthDp = 411,
    heightDp = 760,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun DashboardDarkPreview() {
    FieldFlowTheme(darkTheme = true) {
        DashboardUi(
            state = previewContentState(isAboutVisible = false),
        )
    }
}

@Preview(
    name = "Dashboard Long Title",
    showBackground = true,
    widthDp = 411,
    heightDp = 760,
)
@Composable
private fun DashboardLongTitlePreview() {
    FieldFlowTheme {
        DashboardUi(
            state = DashboardState.Content(
                overview = DashboardOverviewUi(
                    totalInspections = 1,
                    inProgressInspections = 1,
                    syncPendingInspections = 0,
                    pendingIssues = 2,
                ),
                heroInspection = previewLongInspection,
                selectedFilter = InspectionFilterUi.ALL,
                filteredInspections = listOf(previewLongInspection),
                isAboutVisible = false,
                startInspection = previewStartInspection,
                eventSink = {},
            ),
        )
    }
}

@Preview(
    name = "Dashboard Narrow",
    showBackground = true,
    widthDp = 320,
    heightDp = 760,
)
@Composable
private fun DashboardNarrowPreview() {
    FieldFlowTheme {
        DashboardUi(
            state = DashboardState.Content(
                overview = previewOverview,
                heroInspection = previewInspections.first(),
                selectedFilter = InspectionFilterUi.ALL,
                filteredInspections = previewInspections,
                isAboutVisible = false,
                startInspection = previewStartInspection,
                eventSink = {},
            ),
        )
    }
}

private val previewOverview = DashboardOverviewUi(
    totalInspections = 3,
    inProgressInspections = 1,
    syncPendingInspections = 1,
    pendingIssues = 2,
)

private val previewFilteredEmptyOverview = DashboardOverviewUi(
    totalInspections = 1,
    inProgressInspections = 1,
    syncPendingInspections = 0,
    pendingIssues = 1,
)

private fun previewContentState(
    isAboutVisible: Boolean,
): DashboardState.Content {
    return DashboardState.Content(
        overview = previewOverview,
        heroInspection = previewInspections.first(),
        selectedFilter = InspectionFilterUi.ALL,
        filteredInspections = previewInspections,
        isAboutVisible = isAboutVisible,
        startInspection = previewStartInspection,
        eventSink = {},
    )
}

private fun previewEmptyState(
    isAboutVisible: Boolean,
): DashboardState.Empty {
    return DashboardState.Empty(
        overview = DashboardOverviewUi(
            totalInspections = 0,
            inProgressInspections = 0,
            syncPendingInspections = 0,
            pendingIssues = 0,
        ),
        selectedFilter = InspectionFilterUi.ALL,
        isAboutVisible = isAboutVisible,
        startInspection = previewStartInspection,
        eventSink = {},
    )
}

private val previewInspections = listOf(
    InspectionSummaryUi(
        id = InspectionId("computer-lab-i-44"),
        title = "Computer Lab I.44",
        statusLabel = "In progress",
        statusTone = StatusTone.InProgress,
        completedItems = 6,
        totalItems = 10,
        progressFraction = 0.6f,
        filter = InspectionFilterUi.IN_PROGRESS,
    ),
    InspectionSummaryUi(
        id = InspectionId("projector-p-204"),
        title = "Projector P-204",
        statusLabel = "Not started",
        statusTone = StatusTone.Neutral,
        completedItems = 0,
        totalItems = 8,
        progressFraction = 0f,
        filter = InspectionFilterUi.NOT_STARTED,
    ),
    InspectionSummaryUi(
        id = InspectionId("laboratory-a2-safety-check"),
        title = "Laboratory A2 Safety Check",
        statusLabel = "Sync pending",
        statusTone = StatusTone.Warning,
        completedItems = 12,
        totalItems = 12,
        progressFraction = 1f,
        filter = InspectionFilterUi.SYNC_PENDING,
    ),
)

private val previewLongInspection = InspectionSummaryUi(
    id = InspectionId("long-title"),
    title = "Facility wide life safety inspection for Building A Level 2 Mechanical Plant Area",
    statusLabel = "In progress",
    statusTone = StatusTone.InProgress,
    completedItems = 3,
    totalItems = 14,
    progressFraction = 0.21f,
    filter = InspectionFilterUi.IN_PROGRESS,
)

private val previewStartInspection = StartInspectionUi(
    isVisible = false,
    isCreating = false,
    assets = listOf(
        StartInspectionAssetUi(
            id = com.topic11.cs426.domain.model.AssetId("sample-asset-lab-i44"),
            name = "Computer Lab I.44",
            subtitle = "LAB-I44 • HCMUS",
        ),
    ),
    templates = listOf(
        StartInspectionTemplateUi(
            id = com.topic11.cs426.domain.model.TemplateId("sample-template-v1"),
            name = "Sample Facility Inspection",
            versionLabel = "v1 • 1 sections",
        ),
    ),
    selectedAssetId = com.topic11.cs426.domain.model.AssetId("sample-asset-lab-i44"),
    selectedTemplateId = com.topic11.cs426.domain.model.TemplateId("sample-template-v1"),
    errorMessage = null,
)
