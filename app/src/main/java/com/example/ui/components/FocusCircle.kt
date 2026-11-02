package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrightWhite
import com.example.ui.theme.CompletedRingArc
import com.example.ui.theme.CompletedRingTrack
import com.example.ui.theme.RunningRingArc
import com.example.ui.theme.RunningRingTrack
import com.example.ui.theme.StoppedRingColor

/**
 * Pure, simple Apple/Steve Jobs-style circular timer.
 * - No dots around the ring.
 * - Only the time numeral inside the ring.
 * - Stopped/Paused: the colour picked on the dev screen.
 * - Running: Focused dark gray/silver tone.
 * - Tap directly on the circle to start / pause.
 */
@Composable
fun SimpleFocusRing(
    progress: Float,
    timeText: String,
    isRunning: Boolean,
    isCompleted: Boolean,
    isAlerting: Boolean,
    stoppedColor: StoppedRingColor,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 310.dp
) {
    // Dynamic color transitions for stopped vs running states
    val targetTrackColor = when {
        isCompleted -> CompletedRingTrack
        isRunning -> RunningRingTrack
        else -> stoppedColor.track
    }

    val targetArcColor = when {
        isCompleted -> CompletedRingArc
        isRunning -> RunningRingArc
        else -> stoppedColor.arc
    }

    val animatedTrackColor by animateColorAsState(
        targetValue = targetTrackColor,
        animationSpec = tween(400),
        label = "trackColor"
    )

    val animatedArcColor by animateColorAsState(
        targetValue = targetArcColor,
        animationSpec = tween(400),
        label = "arcColor"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(120),
        label = "progress"
    )

    // Gentle calm breathing pulse while the alert sounds
    val infiniteTransition = rememberInfiniteTransition(label = "completedGlow")
    val completionPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onTap() }
            .testTag("focus_ring_area"),
        contentAlignment = Alignment.Center
    ) {
        // Pure smooth circle drawing - no dots, no clutter
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 6.dp.toPx()
            val canvasMin = this.size.minDimension
            val radius = (canvasMin / 2f) - (strokeWidth / 2f) - 6.dp.toPx()
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            // 1. Base track circle
            drawCircle(
                color = animatedTrackColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // 2. Sweeping progress arc
            val sweep = 360f * animatedProgress
            if (sweep > 0.5f) {
                val effectiveArcColor = if (isAlerting) {
                    animatedArcColor.copy(alpha = completionPulse)
                } else {
                    animatedArcColor
                }

                drawArc(
                    color = effectiveArcColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Inside the ring: ONLY the time numerals (Pure Steve Jobs simplicity)
        Text(
            text = timeText,
            color = if (isCompleted) CompletedRingArc else BrightWhite,
            fontSize = if (timeText.length > 5) 44.sp else 58.sp,
            fontWeight = FontWeight.ExtraLight,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            modifier = Modifier.testTag("countdown_display")
        )
    }
}
