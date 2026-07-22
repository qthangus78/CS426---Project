package com.topic11.cs426.feature.dashboard.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
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
                    Text(text = filter.label)
                },
                modifier = Modifier.testTag(filter.testTag()),
            )
        }
    }
}

private fun InspectionFilterUi.testTag(): String {
    return "dashboard-filter-${name.lowercase().replace('_', '-')}"
}
