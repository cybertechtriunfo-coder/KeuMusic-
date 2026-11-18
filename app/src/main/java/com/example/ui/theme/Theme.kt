package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PulseDarkColorScheme = darkColorScheme(
    primary = PulseGreen,
    onPrimary = Color(0xFF000000),
    primaryContainer = PulseGreenMuted,
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = YouTubeCrimson,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = SurfaceHighlight,
    onSecondaryContainer = TextPrimary,
    tertiary = LikedGradientMid,
    onTertiary = Color(0xFF000000),
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = SurfaceBase,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBackground,
    surfaceContainerLow = SurfaceBase,
    surfaceContainer = SurfaceElevated,
    surfaceContainerHigh = SurfaceCard,
    surfaceContainerHighest = SurfaceHighlight,
    outline = DividerSubtle,
    outlineVariant = Color(0xFF3A3A3A),
    error = ErrorRed,
    onError = Color(0xFFFFFFFF)
)

private val PulseLightColorScheme = lightColorScheme(
    primary = PulseGreenLightMode,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC8F7D6),
    onPrimaryContainer = Color(0xFF00210B),
    secondary = YouTubeCrimsonLight,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = LightSurfaceHighlight,
    onSecondaryContainer = LightTextPrimary,
    tertiary = LikedGradientStart,
    onTertiary = Color(0xFFFFFFFF),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurfaceBase,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceCard,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainerLowest = LightSurfaceBase,
    surfaceContainerLow = LightBackground,
    surfaceContainer = LightSurfaceElevated,
    surfaceContainerHigh = LightSurfaceCard,
    surfaceContainerHighest = LightSurfaceHighlight,
    outline = LightDivider,
    outlineVariant = Color(0xFFE4E8EF),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun PulseTubeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PulseDarkColorScheme else PulseLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = PulseTubeTypography,
        content = content
    )
}
