package com.topic11.cs426.feature.inspection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.topic11.cs426.core.designsystem.FieldFlowTopAppBar
import com.topic11.cs426.core.designsystem.StatusBadge
import com.topic11.cs426.core.designsystem.StatusTone
import com.topic11.cs426.domain.model.InspectionStatus
import com.topic11.cs426.domain.model.InspectionSummary

@Composable
internal fun InspectionUi(
    state: InspectionState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            FieldFlowTopAppBar(
                title = "Inspection",
                onBackClick = { state.eventSink(InspectionEvent.BackSelected) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(PaddingValues(horizontal = 20.dp, vertical = 16.dp))
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                state.isLoading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.inspection == null -> {
                    Text(
                        text = "Inspection not found.",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                else -> {
                    InspectionContent(inspection = state.inspection)
                }
            }
        }
    }
}

@Composable
private fun InspectionContent(
    inspection: InspectionSummary,
) {
    Text(
        text = inspection.title,
        style = MaterialTheme.typography.headlineSmall,
    )
    StatusBadge(
        label = inspection.status.displayLabel(),
        tone = inspection.status.statusTone(),
    )
    Text(
        text = "${inspection.completedItems} of ${inspection.totalItems} items complete",
        style = MaterialTheme.typography.bodyLarge,
    )
    LinearProgressIndicator(
        progress = { inspection.progressFraction.coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text = "Full checklist workflow will be implemented later.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
