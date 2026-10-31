package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppleTimerOrange
import com.example.ui.theme.AppleTimerOrangeTrack
import com.example.ui.theme.BrightWhite
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Intuitive Apple-style interactive circular time dial.
 * Instead of tasteless scrolling numbers, users can drag directly around the circle
 * or tap clean preset buttons, with smooth visual feedback and haptics.
 */
@Composable
fun InteractiveTimerDial(
    totalSeconds: Long,
    onSecondsChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
    maxMinutes: Int = 60,
    size: Dp = 260.dp
) {
    val view = LocalView.current
    val currentSeconds by rememberUpdatedState(totalSeconds)

    // Fraction of full 60-minute dial
    val maxSecs = maxMinutes * 60f
    val progress = (totalSeconds / maxSecs).coerceIn(0.005f, 1f)

    val animatedSweep by animateFloatAsState(
        targetValue = progress * 360f,
        animationSpec = tween(150),
        label = "dialSweep"
    )

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val centerX = this.size.width / 2f
                        val centerY = this.size.height / 2f
                        val angle = calculateAngle(offset.x - centerX, offset.y - centerY)
                        val newSeconds = angleToSeconds(angle, maxMinutes)
                        onSecondsChanged(newSeconds)
                        try {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        } catch (e: Exception) {}
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val centerX = this.size.width / 2f
                        val centerY = this.size.height / 2f
                        val angle = calculateAngle(change.position.x - centerX, change.position.y - centerY)
                        val newSeconds = angleToSeconds(angle, maxMinutes)
                        if (newSeconds != currentSeconds) {
                            onSecondsChanged(newSeconds)
                            if (newSeconds % 60L == 0L) {
                                try {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                } catch (e: Exception) {}
                            }
                        }
                    }
                )
            }
            .testTag("interactive_timer_dial"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val canvasMin = this.size.minDimension
            val radius = (canvasMin / 2f) - (strokeWidth / 2f) - 8.dp.toPx()
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            // 1. Background dark track
            drawCircle(
                color = AppleTimerOrangeTrack,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // 2. Active Apple Orange sweeping arc
            if (animatedSweep > 1f) {
                drawArc(
                    color = AppleTimerOrange,
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 3. Glowing thumb knob at the tip of the arc
                val thumbAngleRad = Math.toRadians((animatedSweep - 90f).toDouble())
                val thumbCenter = Offset(
                    x = (center.x + radius * cos(thumbAngleRad)).toFloat(),
                    y = (center.y + radius * sin(thumbAngleRad)).toFloat()
                )

                // Outer halo
                drawCircle(
                    color = AppleTimerOrange.copy(alpha = 0.35f),
                    radius = strokeWidth * 1.5f,
                    center = thumbCenter
                )

                // Inner bright knob
                drawCircle(
                    color = BrightWhite,
                    radius = strokeWidth * 0.9f,
                    center = thumbCenter
                )
            }
        }

        // Center formatted time readout
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val formattedTime = if (totalSeconds >= 3600) {
            val h = totalSeconds / 3600
            val m = (totalSeconds % 3600) / 60
            String.format("%02d:%02d:%02d", h, m, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }

        Text(
            text = formattedTime,
            color = BrightWhite,
            fontSize = 46.sp,
            fontWeight = FontWeight.ExtraLight,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

private fun calculateAngle(dx: Float, dy: Float): Double {
    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
    // Rotate so top is 0 degrees
    angle = (angle + 90.0) % 360.0
    if (angle < 0.0) angle += 360.0
    return angle
}

private fun angleToSeconds(angleDegrees: Double, maxMinutes: Int): Long {
    val fraction = (angleDegrees / 360.0).coerceIn(0.0, 1.0)
    val totalSeconds = fraction * (maxMinutes * 60)
    // Snap to 30-second steps for a clean, natural feel
    val snappedSeconds = (Math.round(totalSeconds / 30.0) * 30).coerceAtLeast(30)
    return snappedSeconds
}
