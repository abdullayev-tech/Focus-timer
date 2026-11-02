package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.GentleSoundMode
import com.example.audio.GentleVibrationMode
import com.example.ui.theme.AppleTimerOrange
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private val FADE_IN_OPTIONS = listOf(15, 25, 40, 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    selectedMode: GentleSoundMode,
    crescendoDuration: Int,
    hapticsEnabled: Boolean,
    vibrationMode: GentleVibrationMode,
    isTestingAlert: Boolean,
    testVolume: Float,
    onSelectMode: (GentleSoundMode) -> Unit,
    onSelectCrescendoDuration: (Int) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onSelectVibrationMode: (GentleVibrationMode) -> Unit,
    onToggleTestAlert: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

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
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Alert",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center)
                )

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text(
                        text = "Done",
                        color = AppleTimerOrange,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            SettingsGroup(title = "Sound", footer = "Plays at your alarm volume") {
                GentleSoundMode.values().forEach { mode ->
                    CheckRow(
                        title = mode.title,
                        isSelected = mode == selectedMode,
                        onClick = { onSelectMode(mode) },
                        modifier = Modifier.testTag("sound_mode_${mode.name}")
                    )
                    RowDivider()
                }
                FadeInRow(
                    selectedSeconds = crescendoDuration,
                    onSelect = onSelectCrescendoDuration
                )
            }

            SettingsGroup(title = "Vibration") {
                CheckRow(
                    title = "Off",
                    isSelected = !hapticsEnabled,
                    onClick = { onToggleHaptics(false) },
                    modifier = Modifier.testTag("vibration_off")
                )
                GentleVibrationMode.values().forEach { mode ->
                    RowDivider()
                    CheckRow(
                        title = mode.title,
                        isSelected = hapticsEnabled && mode == vibrationMode,
                        onClick = {
                            onToggleHaptics(true)
                            onSelectVibrationMode(mode)
                        },
                        modifier = Modifier.testTag("vibration_${mode.name}")
                    )
                }
            }

            SettingsGroup {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clickable(onClick = onToggleTestAlert)
                        .testTag("test_alert_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isTestingAlert) "Stop Test" else "Test Alert",
                        color = AppleTimerOrange,
                        fontSize = 16.sp
                    )
                }

                // The fade-in starts almost silent, so show that the test is really running
                if (isTestingAlert) {
                    LinearProgressIndicator(
                        progress = { testVolume.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = AppleTimerOrange,
                        trackColor = DarkSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Spacer(modifier = Modifier.height(16.dp))

    if (title != null) {
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant),
        content = content
    )

    if (footer != null) {
        Text(
            text = footer,
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 16.dp, top = 6.dp)
        )
    }
}

@Composable
private fun CheckRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 16.sp
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = AppleTimerOrange,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FadeInRow(
    selectedSeconds: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(start = 16.dp, end = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Fade in",
            color = TextPrimary,
            fontSize = 16.sp
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface)
                .padding(2.dp)
        ) {
            FADE_IN_OPTIONS.forEach { seconds ->
                val isSelected = seconds == selectedSeconds
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) TextMuted else DarkSurface)
                        .clickable { onSelect(seconds) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("crescendo_${seconds}s"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${seconds}s",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        thickness = 0.5.dp,
        color = DarkBorder
    )
}
