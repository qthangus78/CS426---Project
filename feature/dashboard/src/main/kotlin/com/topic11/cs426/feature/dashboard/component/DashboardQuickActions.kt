package com.topic11.cs426.feature.dashboard.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.topic11.cs426.feature.dashboard.DashboardEvent

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
        ) {
            QuickActionCard(
                title = "Assets",
                description = "Facilities and equipment",
                testTag = "dashboard-assets",
                onClick = { eventSink(DashboardEvent.AssetsSelected) },
                modifier = Modifier.weight(1f),
            )
            QuickActionCard(
                title = "Templates",
                description = "Inspection checklists",
                testTag = "dashboard-templates",
                onClick = { eventSink(DashboardEvent.TemplatesSelected) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickActionCard(
                title = "Issues",
                description = "Maintenance follow-up",
                testTag = "dashboard-issues",
                onClick = { eventSink(DashboardEvent.IssuesSelected) },
                modifier = Modifier.weight(1f),
            )
            QuickActionCard(
                title = "Reports",
                description = "Inspection summaries",
                testTag = "dashboard-reports",
                onClick = { eventSink(DashboardEvent.ReportsSelected) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    description: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 104.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
