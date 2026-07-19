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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.topic11.cs426.core.designsystem.FieldFlowTopAppBar
import com.topic11.cs426.core.designsystem.InspectionSummaryCard
import com.topic11.cs426.core.designsystem.StatusBadge
import com.topic11.cs426.core.designsystem.StatusTone
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.InspectionSummary

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
                .fillMaxWidth(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(
                        label = "Architecture Bootstrap",
                        tone = StatusTone.Neutral,
                    )
                    Text(
                        text = "Inspection overview",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }

            item {
                QuickAccessButtons(eventSink = state.eventSink)
            }

            if (state.isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
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

@Composable
private fun QuickAccessButtons(
    eventSink: (DashboardEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                modifier = Modifier.weight(1f),
                onClick = { eventSink(DashboardEvent.AssetsSelected) },
            ) {
                Text("Assets")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
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
                modifier = Modifier.weight(1f),
                onClick = { eventSink(DashboardEvent.IssuesSelected) },
            ) {
                Text("Issues")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { eventSink(DashboardEvent.ReportsSelected) },
            ) {
                Text("Reports")
            }
        }
    }
}

@Composable
private fun InspectionSummaryRow(
    inspection: InspectionSummary,
    onClick: () -> Unit,
) {
    InspectionSummaryCard(
        title = inspection.title,
        statusLabel = inspection.status.displayLabel(),
        statusTone = inspection.status.statusTone(),
        completedItems = inspection.completedItems,
        totalItems = inspection.totalItems,
        progressFraction = inspection.progressFraction,
        onClick = onClick,
    )
}

private fun InspectionStatus.displayLabel(): String {
    return when (this) {
        InspectionStatus.NOT_STARTED -> "Not started"
        InspectionStatus.IN_PROGRESS -> "In progress"
        InspectionStatus.COMPLETED -> "Completed"
        InspectionStatus.SYNC_PENDING -> "Sync pending"
    }
}

private fun InspectionStatus.statusTone(): StatusTone {
    return when (this) {
        InspectionStatus.NOT_STARTED -> StatusTone.Neutral
        InspectionStatus.IN_PROGRESS -> StatusTone.InProgress
        InspectionStatus.COMPLETED -> StatusTone.Success
        InspectionStatus.SYNC_PENDING -> StatusTone.Warning
    }
}
