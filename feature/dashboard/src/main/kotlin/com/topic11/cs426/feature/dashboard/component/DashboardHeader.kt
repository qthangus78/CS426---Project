package com.topic11.cs426.feature.dashboard.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.topic11.cs426.core.designsystem.StatusBadge
import com.topic11.cs426.core.designsystem.StatusTone

@Composable
internal fun DashboardTopArea(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard-top-area"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "FieldFlow",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Facility inspection workspace",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Manage active inspections and operational follow-up",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StatusBadge(
            label = "Architecture Bootstrap",
            tone = StatusTone.Neutral,
        )
    }
}

@Composable
internal fun DashboardSectionHeader(
    title: String,
    countLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        if (countLabel != null) {
            Text(
                text = countLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
