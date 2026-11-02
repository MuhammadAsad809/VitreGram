package com.vitregram.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val ObsidianTvColorScheme = darkColorScheme(
    primary = InstagramPink,
    onPrimary = Color.White,
    primaryContainer = ObsidianCard,
    onPrimaryContainer = TextPrimary,
    secondary = InstagramBlue,
    onSecondary = Color.White,
    secondaryContainer = ObsidianSurface,
    onSecondaryContainer = TextSecondary,
    tertiary = InstagramYellow,
    onTertiary = Color.Black,
    tertiaryContainer = ObsidianCard,
    onTertiaryContainer = TextPrimary,
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    error = ErrorColor,
    onError = Color.White,
    errorContainer = Color(0x33EF4444),
    onErrorContainer = ErrorColor,
    border = GlassBorder,
    borderVariant = GlassBorderFocused,
    scrim = Color.Black
)

@Composable
fun VitreGramTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ObsidianTvColorScheme,
        typography = TvTypography,
        content = content
    )
}
