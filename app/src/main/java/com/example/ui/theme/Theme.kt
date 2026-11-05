package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppleMinimalColorScheme = darkColorScheme(
    primary = BrightWhite,
    onPrimary = OLEDBlack,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = BrightWhite,
    secondary = StoppedRingColor.STEEL.arc,
    onSecondary = BrightWhite,
    background = OLEDBlack,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorderSubtle
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppleMinimalColorScheme,
        typography = Typography,
        content = content
    )
}
