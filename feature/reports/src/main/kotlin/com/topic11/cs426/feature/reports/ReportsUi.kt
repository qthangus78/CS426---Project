package com.topic11.cs426.feature.reports

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.topic11.cs426.core.designsystem.FeaturePlaceholder

@Composable
internal fun ReportsUi(
    state: ReportsState,
    modifier: Modifier = Modifier,
) {
    FeaturePlaceholder(
        title = state.title,
        message = state.message,
        details = state.details,
        futureResponsibilities = state.futureResponsibilities,
        onBackClick = { state.eventSink(ReportsEvent.BackSelected) },
        modifier = modifier,
    )
}
