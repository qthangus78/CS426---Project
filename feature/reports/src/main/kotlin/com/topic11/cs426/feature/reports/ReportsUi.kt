package com.topic11.cs426.feature.reports

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.topic11.cs426.core.designsystem.FieldFlowTheme
import com.topic11.cs426.core.designsystem.FieldFlowTopAppBar

@Composable
internal fun ReportsUi(
    state: ReportsState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag("reports-root"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FieldFlowTopAppBar(
                title = state.topBarTitle,
                onBackClick = { state.eventSink(ReportsEvent.BackSelected) },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .testTag("reports-content"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ReportsEmptyHeader(state = state)
            }
            item {
                FutureCapabilitiesHeader()
            }
            itemsIndexed(
                items = state.futureCapabilities,
                key = { _, capability -> capability.title },
            ) { index, capability ->
                ReportCapabilityCard(
                    index = index,
                    capability = capability,
                )
            }
        }
    }
}

@Composable
private fun ReportsEmptyHeader(
    state: ReportsState,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reports-empty"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ReportPlaceholderIllustration()
            Column(
                modifier = Modifier.testTag("reports-empty-state"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = state.details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReportPlaceholderIllustration(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
            .semantics { contentDescription = "Report placeholder illustration" },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(width = 128.dp, height = 96.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Report",
                    style = MaterialTheme.typography.labelLarge,
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.24f),
                    content = {},
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.24f),
                    content = {},
                )
            }
        }
    }
}

@Composable
private fun FutureCapabilitiesHeader() {
    Column(
        modifier = Modifier.testTag("reports-capabilities"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Future capabilities",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Shown as product direction only, not active export history.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReportCapabilityCard(
    index: Int,
    capability: ReportCapabilityUi,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "0${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = capability.title,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = capability.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(
    name = "Reports Placeholder",
    showBackground = true,
    widthDp = 411,
    heightDp = 760,
)
@Composable
private fun ReportsPlaceholderPreview() {
    FieldFlowTheme {
        ReportsUi(state = previewReportsState)
    }
}

@Preview(
    name = "Reports Dark",
    showBackground = true,
    widthDp = 411,
    heightDp = 760,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ReportsDarkPreview() {
    FieldFlowTheme(darkTheme = true) {
        ReportsUi(state = previewReportsState)
    }
}

@Preview(
    name = "Reports Narrow",
    showBackground = true,
    widthDp = 320,
    heightDp = 700,
)
@Composable
private fun ReportsNarrowPreview() {
    FieldFlowTheme {
        ReportsUi(state = previewReportsState)
    }
}

@Preview(
    name = "Reports Large Font",
    showBackground = true,
    widthDp = 411,
    heightDp = 760,
    fontScale = 1.3f,
)
@Composable
private fun ReportsLargeFontPreview() {
    FieldFlowTheme {
        ReportsUi(state = previewReportsState)
    }
}

private val previewReportsState = ReportsState(eventSink = {})
