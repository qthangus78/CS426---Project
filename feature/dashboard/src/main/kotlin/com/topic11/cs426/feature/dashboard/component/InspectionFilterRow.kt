package com.topic11.cs426.feature.dashboard.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.topic11.cs426.feature.dashboard.DashboardEvent
import com.topic11.cs426.feature.dashboard.InspectionFilterUi

@Composable
internal fun InspectionFilterRow(
    selectedFilter: InspectionFilterUi,
    eventSink: (DashboardEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .testTag("dashboard-filters"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InspectionFilterUi.entries.forEach { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = {
                    eventSink(DashboardEvent.FilterSelected(filter))
                },
                label = {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .testTag(filter.testTag()),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

private fun InspectionFilterUi.testTag(): String {
    return "dashboard-filter-${name.lowercase().replace('_', '-')}"
}
