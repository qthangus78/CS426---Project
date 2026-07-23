package com.topic11.cs426.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class StatusTone {
    Neutral,
    InProgress,
    Success,
    Warning,
}

@Composable
fun StatusBadge(
    label: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    val colors = statusColors(tone)
    Surface(
        modifier = modifier.heightIn(min = 28.dp),
        shape = RoundedCornerShape(20.dp),
        color = colors.container,
        contentColor = colors.content,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(PaddingValues(horizontal = 12.dp, vertical = 4.dp)),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private data class BadgeColors(
    val container: Color,
    val content: Color,
    val border: Color,
)

@Composable
private fun statusColors(tone: StatusTone): BadgeColors {
    val colorScheme = MaterialTheme.colorScheme
    return when (tone) {
        StatusTone.Neutral -> BadgeColors(
            container = colorScheme.surfaceVariant,
            content = colorScheme.onSurfaceVariant,
            border = colorScheme.outlineVariant,
        )
        StatusTone.InProgress -> BadgeColors(
            container = colorScheme.secondaryContainer,
            content = colorScheme.onSecondaryContainer,
            border = colorScheme.secondary.copy(alpha = 0.32f),
        )
        StatusTone.Success -> BadgeColors(
            container = colorScheme.primaryContainer,
            content = colorScheme.onPrimaryContainer,
            border = colorScheme.primary.copy(alpha = 0.32f),
        )
        StatusTone.Warning -> BadgeColors(
            container = colorScheme.tertiaryContainer,
            content = colorScheme.onTertiaryContainer,
            border = colorScheme.tertiary.copy(alpha = 0.32f),
        )
    }
}
