package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Minimalist Apple-inspired OLED palette
val OLEDBlack = Color(0xFF000000)
val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF161618)
val DarkSurfaceVariant = Color(0xFF242426)
val DarkBorder = Color(0xFF2C2C2E)
val DarkBorderSubtle = Color(0xFF1E1E22)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF8E8E93)
val TextMuted = Color(0xFF636366)

// The label on a home screen row: one step brighter than TextSecondary
val TextLabel = Color(0xFFAEAEB2)

// When stopped/paused; picked on the dev screen. No orange here: that's what running looks like
enum class StoppedRingColor(val title: String, val arc: Color, val track: Color) {
    SLATE_BLUE("Slate Blue", Color(0xFF3E5A82), Color(0xFF141C2B)),
    SILVER_WHITE("Silver White", Color(0xFFD8DCE3), Color(0xFF26282C)),
    GRAPHITE("Graphite", Color(0xFF8A8F98), Color(0xFF1C1D20)),
    ICE("Ice", Color(0xFF9CC4E4), Color(0xFF16222C)),
    SAGE("Sage", Color(0xFF7FA08C), Color(0xFF151F19)),
    LAVENDER("Lavender", Color(0xFF9A8FC7), Color(0xFF1B1826)),
    ROSE("Rose", Color(0xFFC08A96), Color(0xFF241719)),
    SAND("Sand", Color(0xFFC4AE8A), Color(0xFF231E15))
}

// Apple iOS Clock Timer System Orange (Dark Mode: #FF9F0A)
val AppleTimerOrange = Color(0xFFFF9F0A)
val AppleTimerOrangeTrack = Color(0xFF2B1906)

// When running: Apple timer orange
val RunningRingTrack = AppleTimerOrangeTrack
val RunningRingArc = AppleTimerOrange

// Completed state: soft serene amber glow
val CompletedRingArc = Color(0xFFFFB340)
val CompletedRingTrack = Color(0xFF332008)

val BrightWhite = Color(0xFFFFFFFF)
