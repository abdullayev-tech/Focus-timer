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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SimpleFocusRing
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OLEDBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.ActiveTimerState

@Composable
fun FocusTimerScreen(
    state: ActiveTimerState,
    isAlerting: Boolean,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onReset: () -> Unit,
    onDismissAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    // Handle system back navigation
    BackHandler {
        onBack()
    }

    // Pure Steve Jobs simplicity: tapping the screen or ring starts / pauses
    fun handleTap() {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (e: Exception) {
            // Ignore
        }

        if (isAlerting) {
            onDismissAlert()
        } else if (state.isCompleted) {
            onReset()
        } else {
            onTogglePlayPause()
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

        // Center Stage: Pure clean circular ring with ONLY time inside
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            SimpleFocusRing(
                progress = state.progress,
                timeText = state.formattedRemaining(),
                isRunning = state.isRunning,
                isCompleted = state.isCompleted || isAlerting,
                onTap = { handleTap() },
                size = 320.dp
            )
        }

        // Subtle alert dismissal prompt (only visible when time is up and alert is playing)
        AnimatedVisibility(
            visible = isAlerting,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = "Tap to silence",
                color = TextSecondary,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.testTag("silence_alert_hint")
            )
        }
    }
}
