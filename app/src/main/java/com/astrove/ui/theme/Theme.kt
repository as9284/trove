package com.astrove.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Locked editorial palette: no dynamic color, no purple. Every Material role is
// mapped onto Ink / Paper / Brass so nothing can fall back to a baseline accent.
// Brass is the single spice; surfaceTint is pinned to the surface itself so
// elevated surfaces never pick up a tint (keeps the flat, paper-like feel).
private val LightColors = lightColorScheme(
    primary = Brass,
    onPrimary = Ink,
    primaryContainer = PaperDim,
    onPrimaryContainer = Ink,
    inversePrimary = Brass,
    secondary = Brass,
    onSecondary = Ink,
    secondaryContainer = PaperDim,
    onSecondaryContainer = Ink,
    tertiary = Brass,
    onTertiary = Ink,
    tertiaryContainer = PaperDim,
    onTertiaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceTint = Paper,
    surfaceVariant = PaperDim,
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = Paper,
    surfaceContainerLow = Linen,
    surfaceContainer = Linen,
    surfaceContainerHigh = PaperDim,
    surfaceContainerHighest = PaperDim,
    inverseSurface = Ink,
    inverseOnSurface = Paper,
    outline = HairlineLight,
    outlineVariant = HairlineLightSoft,
)

private val DarkColors = darkColorScheme(
    primary = Brass,
    onPrimary = Ink,
    primaryContainer = InkElevated,
    onPrimaryContainer = PaperOnInk,
    inversePrimary = Brass,
    secondary = Brass,
    onSecondary = Ink,
    secondaryContainer = InkElevated,
    onSecondaryContainer = PaperOnInk,
    tertiary = Brass,
    onTertiary = Ink,
    tertiaryContainer = InkElevated,
    onTertiaryContainer = PaperOnInk,
    background = Ink,
    onBackground = PaperOnInk,
    surface = Ink,
    onSurface = PaperOnInk,
    surfaceTint = Ink,
    surfaceVariant = InkElevated,
    onSurfaceVariant = PaperMutedOnInk,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = InkRaised,
    surfaceContainer = InkRaised,
    surfaceContainerHigh = InkElevated,
    surfaceContainerHighest = InkElevated,
    inverseSurface = Paper,
    inverseOnSurface = Ink,
    outline = InkSoft,
    outlineVariant = InkSoft,
)

@Composable
fun TroveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = TroveTypography,
        shapes = TroveShapes,
        content = content,
    )
}
