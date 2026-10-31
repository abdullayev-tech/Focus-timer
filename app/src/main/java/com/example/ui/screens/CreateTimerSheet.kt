package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.InteractiveTimerDial
import com.example.ui.theme.AppleTimerOrange
import com.example.ui.theme.BrightWhite
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class TimerPreset(val label: String, val seconds: Long)

val PRESET_OPTIONS = listOf(
    TimerPreset("1m", 60L),
    TimerPreset("2m", 120L),
    TimerPreset("5m", 300L),
    TimerPreset("10m", 600L),
    TimerPreset("15m", 900L),
    TimerPreset("25m", 1500L),
    TimerPreset("45m", 2700L)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTimerSheet(
    onDismiss: () -> Unit,
    onCreateTimer: (label: String, totalSeconds: Long, autoStart: Boolean) -> Unit
) {
    val view = LocalView.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Default to clean 2 minutes (120 seconds)
    var selectedSeconds by remember { mutableLongStateOf(120L) }
    var label by remember { mutableStateOf("Focus") }

    fun adjustSeconds(delta: Long) {
        val updated = (selectedSeconds + delta).coerceIn(30L, 7200L)
        selectedSeconds = updated
        try {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } catch (e: Exception) {}
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(DarkBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancel",
                        color = TextSecondary,
                        fontSize = 16.sp
                    )
                }

                Text(
                    text = "New Timer",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                TextButton(
                    onClick = {
                        if (selectedSeconds > 0) {
                            onCreateTimer(label, selectedSeconds, true)
                            onDismiss()
                        }
                    }
                ) {
                    Text(
                        text = "Start",
                        color = AppleTimerOrange,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Center Interactive Dial: Drag finger smoothly around the ring
            InteractiveTimerDial(
                totalSeconds = selectedSeconds,
                onSecondsChanged = { selectedSeconds = it },
                size = 230.dp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Fine-Tuning Pill Controls (-1m, +30s, +1m, +5m)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdjustmentPill(text = "-1m", onClick = { adjustSeconds(-60L) })
                AdjustmentPill(text = "+30s", onClick = { adjustSeconds(30L) })
                AdjustmentPill(text = "+1m", onClick = { adjustSeconds(60L) })
                AdjustmentPill(text = "+5m", onClick = { adjustSeconds(300L) })
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. One-Touch Common Presets
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(PRESET_OPTIONS) { preset ->
                    val isSelected = selectedSeconds == preset.seconds
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AppleTimerOrange else DarkSurfaceVariant)
                            .border(
                                1.dp,
                                if (isSelected) AppleTimerOrange else DarkBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedSeconds = preset.seconds
                                try {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                } catch (e: Exception) {}
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = preset.label,
                            color = if (isSelected) Color.Black else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Label input
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedBorderColor = AppleTimerOrange,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = AppleTimerOrange,
                    unfocusedLabelColor = TextSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timer_label_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Action Buttons (Save or Start)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (selectedSeconds > 0) {
                            onCreateTimer(label, selectedSeconds, false)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurfaceVariant,
                        contentColor = BrightWhite
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                        .testTag("save_timer_button")
                ) {
                    Text(
                        text = "Save",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = {
                        if (selectedSeconds > 0) {
                            onCreateTimer(label, selectedSeconds, true)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppleTimerOrange,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(50.dp)
                        .testTag("start_timer_button")
                ) {
                    Text(
                        text = "Start Focus",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AdjustmentPill(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}
