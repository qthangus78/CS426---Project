package com.topic11.cs426.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.topic11.cs426.core.designsystem.EmptyState
import com.topic11.cs426.core.designsystem.FieldFlowTheme
import com.topic11.cs426.core.designsystem.FieldFlowTopAppBar
import com.topic11.cs426.core.designsystem.InspectionSummaryCard
import com.topic11.cs426.core.designsystem.LoadingContent
import com.topic11.cs426.core.designsystem.StatusTone
import com.topic11.cs426.domain.model.InspectionId
import com.topic11.cs426.feature.dashboard.component.DashboardOverviewCard
import com.topic11.cs426.feature.dashboard.component.DashboardQuickActions
import com.topic11.cs426.feature.dashboard.component.DashboardSectionHeader
import com.topic11.cs426.feature.dashboard.component.DashboardTopArea

@Composable
internal fun DashboardUi(
    state: DashboardState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { FieldFlowTopAppBar(title = "FieldFlow") },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .testTag("dashboard-content"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                DashboardTopArea()
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

                is DashboardState.Empty -> {
                    item {
                        DashboardOverviewCard(overview = state.overview)
                    }
                    item {
                        DashboardQuickActions(eventSink = state.eventSink)
                    }
                    item {
                        EmptyState(
                            title = "No inspections available",
                            message = "Inspection summaries will appear when the current repository emits them.",
                            modifier = Modifier.testTag("dashboard-empty"),
                        )
                    }
                }

                is DashboardState.Content -> {
                    item {
                        DashboardOverviewCard(overview = state.overview)
                    }
                    item {
                        DashboardQuickActions(eventSink = state.eventSink)
                    }
                    item {
                        DashboardSectionHeader(
                            title = "Inspections",
                            countLabel = "${state.inspections.size} total",
                            modifier = Modifier.testTag("dashboard-inspection-list"),
                        )
                    }
                    items(
                        items = state.inspections,
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
                inspections = previewInspections,
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
            state = DashboardState.Empty(
                overview = DashboardOverviewUi(
                    totalInspections = 0,
                    inProgressInspections = 0,
                    syncPendingInspections = 0,
                ),
                eventSink = {},
            ),
        )
    }
}

@Preview(
    name = "Dashboard Long Title",
    showBackground = true,
    widthDp = 320,
    heightDp = 700,
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
                ),
                inspections = listOf(previewLongInspection),
                eventSink = {},
            ),
        )
    }
}

private val previewOverview = DashboardOverviewUi(
    totalInspections = 3,
    inProgressInspections = 1,
    syncPendingInspections = 1,
)

private val previewInspections = listOf(
    InspectionSummaryUi(
        id = InspectionId("computer-lab-i-44"),
        title = "Computer Lab I.44",
        statusLabel = "In progress",
        statusTone = StatusTone.InProgress,
        completedItems = 6,
        totalItems = 10,
        progressFraction = 0.6f,
    ),
    InspectionSummaryUi(
        id = InspectionId("projector-p-204"),
        title = "Projector P-204",
        statusLabel = "Not started",
        statusTone = StatusTone.Neutral,
        completedItems = 0,
        totalItems = 8,
        progressFraction = 0f,
    ),
    InspectionSummaryUi(
        id = InspectionId("laboratory-a2-safety-check"),
        title = "Laboratory A2 Safety Check",
        statusLabel = "Sync pending",
        statusTone = StatusTone.Warning,
        completedItems = 12,
        totalItems = 12,
        progressFraction = 1f,
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
)
