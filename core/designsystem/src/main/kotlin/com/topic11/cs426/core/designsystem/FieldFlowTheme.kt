package com.topic11.cs426.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF256B45),
    onPrimary = Color.White,
    secondary = Color(0xFF315C88),
    onSecondary = Color.White,
    tertiary = Color(0xFF8A5A00),
    onTertiary = Color.White,
    background = Color(0xFFF7FAF8),
    onBackground = Color(0xFF17211B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17211B),
    surfaceVariant = Color(0xFFE2E9E1),
    onSurfaceVariant = Color(0xFF424C43),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7DD99E),
    onPrimary = Color(0xFF00391C),
    secondary = Color(0xFF9FC9F2),
    onSecondary = Color(0xFF003253),
    tertiary = Color(0xFFFFC46A),
    onTertiary = Color(0xFF472A00),
    background = Color(0xFF101612),
    onBackground = Color(0xFFE0E7DF),
    surface = Color(0xFF182019),
    onSurface = Color(0xFFE0E7DF),
    surfaceVariant = Color(0xFF3F493F),
    onSurfaceVariant = Color(0xFFC3CCC1),
)

@Composable
fun FieldFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
