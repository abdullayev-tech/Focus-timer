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

// When stopped/paused; picked on the dev screen. Every colour here must:
// - stay clear of orange and amber, which is what running and finished look like; no red either
// - hold at least 3:1 on black, and sit quieter than the running orange
// - show the arc against its track when paused: track is the same hue, nearly black
// - be told apart from the others on a 6dp ring
enum class StoppedRingColor(val title: String, val arc: Color, val track: Color) {
    // Neutrals
    PEARL("Pearl", Color(0xFFF1EEE6), Color(0xFF1E1D1A)),
    SILVER_WHITE("Silver White", Color(0xFFD8DCE3), Color(0xFF26282C)),
    STONE("Stone", Color(0xFFABA498), Color(0xFF1F1D19)),
    GRAPHITE("Graphite", Color(0xFF8A8F98), Color(0xFF1C1D20)),
    SMOKE("Smoke", Color(0xFF616368), Color(0xFF1C1D1F)),
    // Blues
    ICE("Ice", Color(0xFF9CC4E4), Color(0xFF16222C)),
    SKY("Sky", Color(0xFF61AFDA), Color(0xFF0E1F29)),
    STEEL("Steel", Color(0xFF5C85A8), Color(0xFF101E2A)),
    SLATE_BLUE("Slate Blue", Color(0xFF3E5A82), Color(0xFF141C2B)),
    INDIGO("Indigo", Color(0xFF525CA1), Color(0xFF191C2B)),
    // Cyans
    AQUA("Aqua", Color(0xFF89DAE3), Color(0xFF092124)),
    LAGOON("Lagoon", Color(0xFF37A1B8), Color(0xFF0A2026)),
    TEAL("Teal", Color(0xFF3C8888), Color(0xFF092121)),
    // Greens: grey enough not to read as "go"
    SEAFOAM("Seafoam", Color(0xFF97CCAE), Color(0xFF102118)),
    SAGE("Sage", Color(0xFF7FA08C), Color(0xFF151F19)),
    MOSS("Moss", Color(0xFF627A56), Color(0xFF172013)),
    OLIVE("Olive", Color(0xFF888A5F), Color(0xFF1E1E0F)),
    // Violets
    WISTERIA("Wisteria", Color(0xFFB9A6DD), Color(0xFF1F1A29)),
    PERIWINKLE("Periwinkle", Color(0xFF809DDD), Color(0xFF161D2B)),
    LAVENDER("Lavender", Color(0xFF9A8FC7), Color(0xFF1B1826)),
    PLUM("Plum", Color(0xFF7A537C), Color(0xFF241825)),
    // Warm: pinks and browns only, never clay or gold
    BLUSH("Blush", Color(0xFFE9BEC3), Color(0xFF26191A)),
    ROSE("Rose", Color(0xFFC08A96), Color(0xFF241719)),
    ROSEWOOD("Rosewood", Color(0xFF885B61), Color(0xFF28181A)),
    SAND("Sand", Color(0xFFC4AE8A), Color(0xFF231E15)),
    DRIFTWOOD("Driftwood", Color(0xFF887769), Color(0xFF221C17))
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
