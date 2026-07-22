package com.topic11.cs426.feature.dashboard.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.topic11.cs426.core.designsystem.StatusBadge
import com.topic11.cs426.core.designsystem.StatusTone
import com.topic11.cs426.feature.dashboard.DashboardEvent
import com.topic11.cs426.feature.dashboard.DashboardOverviewUi

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
            text = "Facility inspection workspace",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Current inspection work queue",
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
internal fun DashboardOverviewCard(
    overview: DashboardOverviewUi,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard-overview"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Inspection overview",
                style = MaterialTheme.typography.titleMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                DashboardStatItem(
                    label = "Total",
                    value = overview.totalInspections.toString(),
                    modifier = Modifier.weight(1f),
                )
                DashboardStatItem(
                    label = "In progress",
                    value = overview.inProgressInspections.toString(),
                    modifier = Modifier.weight(1f),
                )
                DashboardStatItem(
                    label = "Sync pending",
                    value = overview.syncPendingInspections.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DashboardStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.heightIn(min = 64.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun DashboardQuickActions(
    eventSink: (DashboardEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard-quick-access"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DashboardSectionHeader(title = "Quick actions")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuickActionButton(
                label = "Assets",
                testTag = "dashboard-assets",
                onClick = { eventSink(DashboardEvent.AssetsSelected) },
                modifier = Modifier.weight(1f),
            )
            QuickActionButton(
                label = "Templates",
                testTag = "dashboard-templates",
                onClick = { eventSink(DashboardEvent.TemplatesSelected) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuickActionButton(
                label = "Issues",
                testTag = "dashboard-issues",
                onClick = { eventSink(DashboardEvent.IssuesSelected) },
                modifier = Modifier.weight(1f),
            )
            QuickActionButton(
                label = "Reports",
                testTag = "dashboard-reports",
                onClick = { eventSink(DashboardEvent.ReportsSelected) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 56.dp)
            .testTag(testTag),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
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
