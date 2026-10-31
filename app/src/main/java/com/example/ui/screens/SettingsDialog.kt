package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.GentleSoundMode
import com.example.audio.GentleVibrationMode
import com.example.ui.theme.BrightWhite
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.StoppedRingArc
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

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
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Timer Alert & Haptics",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sound Options
            Text(
                text = "GENTLE SOUND",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            GentleSoundMode.values().forEach { mode ->
                val isSelected = mode == selectedMode
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) DarkSurfaceVariant else DarkSurface)
                        .border(
                            1.dp,
                            if (isSelected) BrightWhite else DarkBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectMode(mode) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("sound_mode_${mode.name}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.title,
                                color = if (isSelected) BrightWhite else TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = mode.description,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(BrightWhite, CircleShape)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Crescendo Ramp Speed (Slow progressive volume increase)
            Text(
                text = "VOLUME FADE-IN SPEED",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Gradual, whisper-soft crescendo ramp",
                color = TextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15 to "15s", 25 to "25s", 40 to "40s", 60 to "60s").forEach { (sec, label) ->
                    val isSelected = crescendoDuration == sec
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) BrightWhite else DarkSurfaceVariant)
                            .border(
                                1.dp,
                                if (isSelected) BrightWhite else DarkBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectCrescendoDuration(sec) }
                            .padding(vertical = 10.dp)
                            .testTag("crescendo_${sec}s"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Gentle Haptics Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Gentle Vibration",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Calm, non-jarring haptic pulse",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = hapticsEnabled,
                    onCheckedChange = onToggleHaptics,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BrightWhite,
                        checkedTrackColor = StoppedRingArc,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurface
                    ),
                    modifier = Modifier.testTag("haptics_switch")
                )
            }

            // Vibration Styles
            if (hapticsEnabled) {
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "VIBRATION STYLE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                GentleVibrationMode.values().forEach { mode ->
                    val isSelected = mode == vibrationMode
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) DarkSurfaceVariant else DarkSurface)
                            .border(
                                1.dp,
                                if (isSelected) BrightWhite else DarkBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectVibrationMode(mode) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("vibration_${mode.name}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.title,
                                    color = if (isSelected) BrightWhite else TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = mode.description,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(BrightWhite, CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Test Alert & Vibration Button
            Button(
                onClick = onToggleTestAlert,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTestingAlert) StoppedRingArc else DarkSurfaceVariant,
                    contentColor = BrightWhite
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                    .testTag("test_alert_button")
            ) {
                Icon(
                    imageVector = if (isTestingAlert) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isTestingAlert) "Stop Preview" else "Preview Sound & Vibration",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isTestingAlert) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { testVolume.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = BrightWhite,
                    trackColor = DarkSurfaceVariant
                )
            }
        }
    }
}
