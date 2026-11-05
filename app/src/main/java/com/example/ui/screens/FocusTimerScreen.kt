package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.session.ActiveTimerState
import com.example.ui.components.SimpleFocusRing
import com.example.ui.theme.CompletedRingArc
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OLEDBlack
import com.example.ui.theme.StoppedRingColor
import com.example.ui.theme.TextLabel
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun FocusTimerScreen(
    state: ActiveTimerState,
    isAlerting: Boolean,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onReset: () -> Unit,
    onSilenceAlert: () -> Unit,
    stoppedRingColor: StoppedRingColor,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    // Handle system back navigation
    BackHandler {
        onBack()
    }

    // When time is up:
    // Tap 1 (when alert sounding): stops the alert; "Over by" in the corner keeps counting
    // Tap 2: resets to the actual time, ready to run
    // Tap 3: immediately starts the timer
    fun handleTap() {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (e: Exception) {
            // Ignore
        }

        when {
            isAlerting -> onSilenceAlert()
            state.isCompleted -> onReset()
            else -> onTogglePlayPause()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OLEDBlack)
            .statusBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { handleTap() }
    ) {
        // Minimalist Back Navigation (top left)
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
                .size(44.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant)
                .testTag("focus_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        // Once time is up: how long ago, in the corner. The main clock stays at zero
        val overtime = state.formattedOvertime()
        AnimatedVisibility(
            visible = overtime != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Over by",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    text = overtime.orEmpty(),
                    color = CompletedRingArc,
                    fontSize = 26.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("overtime_display")
                )
            }
        }

        // Center Stage: Pure clean circular ring showing actual time
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            SimpleFocusRing(
                progress = state.progress,
                timeText = state.formattedRemaining(),
                isRunning = state.isRunning,
                isCompleted = state.isCompleted || isAlerting,
                isAlerting = isAlerting,
                stoppedColor = stoppedRingColor,
                onTap = { handleTap() },
                size = 320.dp
            )

            // What this session is for, inside the ring above the clock
            state.timerEntity?.let { timer ->
                Text(
                    text = timer.label,
                    color = TextLabel,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(bottom = 140.dp)
                        .widthIn(max = 220.dp)
                        .testTag("focus_label")
                )
            }
        }

        // Subtle prompt once time is up
        AnimatedVisibility(
            visible = isAlerting || state.isCompleted,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = if (isAlerting) "Tap to silence" else "Tap to reset",
                color = TextSecondary,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.testTag("silence_alert_hint")
            )
        }
    }
}
