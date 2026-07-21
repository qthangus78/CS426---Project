package com.topic11.cs426.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.topic11.cs426.core.designsystem.EmptyState
import com.topic11.cs426.core.designsystem.FieldFlowTopAppBar
import com.topic11.cs426.core.designsystem.InspectionSummaryCard
import com.topic11.cs426.core.designsystem.LoadingContent
import com.topic11.cs426.core.designsystem.StatusBadge
import com.topic11.cs426.core.designsystem.StatusTone

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
                .fillMaxWidth()
                .testTag("dashboard-content"),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                DashboardHeader()
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
                        QuickAccessButtons(eventSink = state.eventSink)
                    }
                    item {
                        EmptyState(
                            title = "No inspections available",
                            message = "Inspection summaries will appear here when a repository emits them.",
                            modifier = Modifier.testTag("dashboard-empty"),
                        )
                    }
                }

                is DashboardState.Content -> {
                    item {
                        QuickAccessButtons(eventSink = state.eventSink)
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
private fun DashboardHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusBadge(
            label = "Architecture Bootstrap",
            tone = StatusTone.Neutral,
        )
        Text(
            text = "Inspection overview",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Bootstrap slice with deterministic fake inspection summaries and read-only inspection navigation.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QuickAccessButtons(
    eventSink: (DashboardEvent) -> Unit,
) {
    Column(
        modifier = Modifier.testTag("dashboard-quick-access"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Quick access",
            style = MaterialTheme.typography.titleMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                modifier = Modifier
                    .weight(1f)
                    .testTag("dashboard-assets"),
                onClick = { eventSink(DashboardEvent.AssetsSelected) },
            ) {
                Text("Assets")
            }
            OutlinedButton(
                modifier = Modifier
                    .weight(1f)
                    .testTag("dashboard-templates"),
                onClick = { eventSink(DashboardEvent.TemplatesSelected) },
            ) {
                Text("Templates")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                modifier = Modifier
                    .weight(1f)
                    .testTag("dashboard-issues"),
                onClick = { eventSink(DashboardEvent.IssuesSelected) },
            ) {
                Text("Issues")
            }
            OutlinedButton(
                modifier = Modifier
                    .weight(1f)
                    .testTag("dashboard-reports"),
                onClick = { eventSink(DashboardEvent.ReportsSelected) },
            ) {
                Text("Reports")
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
        onClick = onClick,
        modifier = Modifier.testTag("inspection-card-${inspection.id.value}"),
    )
}
