package com.example.ui.screens

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

// 0 is no fade-in: full level at once
private val FADE_IN_OPTIONS = listOf(0, 15, 25, 40, 60)

// In minutes; 0 is never
private val AUTO_STOP_OPTIONS = listOf(1, 3, 5, 0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    selectedMode: GentleSoundMode,
    crescendoDuration: Int,
    hapticsEnabled: Boolean,
    vibrationMode: GentleVibrationMode,
    ringtoneUri: Uri?,
    autoStopMinutes: Int,
    isTestingAlert: Boolean,
    testVolume: Float,
    onSelectMode: (GentleSoundMode) -> Unit,
    onPickRingtone: (Uri) -> Unit,
    onSelectAutoStop: (Int) -> Unit,
    onSelectCrescendoDuration: (Int) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onSelectVibrationMode: (GentleVibrationMode) -> Unit,
    onToggleTestAlert: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val ringtonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        @Suppress("DEPRECATION")
        val picked = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (picked != null) onPickRingtone(picked)
    }

    val ringtoneTitle = remember(ringtoneUri) {
        try {
            ringtoneUri?.let { RingtoneManager.getRingtone(context, it)?.getTitle(context) }
        } catch (e: Exception) {
            null
        }
    }

    fun openRingtonePicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri)
        try {
            ringtonePicker.launch(intent)
        } catch (e: Exception) {
            // No picker on this phone: its alarm sound still plays
            onSelectMode(GentleSoundMode.SYSTEM_GENTLE)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = { SheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            SheetHeader(title = "Alert", onDone = onDismiss)

            SettingsGroup(title = "Sound", footer = "Plays at your alarm volume") {
                GentleSoundMode.values().forEach { mode ->
                    val isFromPhone = mode == GentleSoundMode.SYSTEM_GENTLE
                    CheckRow(
                        title = mode.title,
                        value = if (isFromPhone) ringtoneTitle else null,
                        isSelected = mode == selectedMode,
                        onClick = {
                            if (isFromPhone) openRingtonePicker() else onSelectMode(mode)
                        },
                        modifier = Modifier.testTag("sound_mode_${mode.name}")
                    )
                    RowDivider()
                }
                SegmentRow(
                    title = "Fade in",
                    options = FADE_IN_OPTIONS,
                    selected = crescendoDuration,
                    optionLabel = { if (it == 0) "Off" else "${it}s" },
                    onSelect = onSelectCrescendoDuration,
                    testTagPrefix = "crescendo"
                )
                RowDivider()
                SegmentRow(
                    title = "Stop after",
                    options = AUTO_STOP_OPTIONS,
                    selected = autoStopMinutes,
                    optionLabel = { if (it == 0) "Never" else "${it}m" },
                    onSelect = onSelectAutoStop,
                    testTagPrefix = "auto_stop"
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
internal fun SheetDragHandle() {
    Box(
        modifier = Modifier
            .padding(vertical = 10.dp)
            .width(36.dp)
            .height(4.dp)
            .clip(CircleShape)
            .background(DarkBorder)
    )
}

@Composable
internal fun SheetHeader(title: String, onDone: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center)
        )

        TextButton(
            onClick = onDone,
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
}

@Composable
internal fun SettingsGroup(
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
internal fun CheckRow(
    title: String,
    isSelected: Boolean,
    value: String? = null,
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

        Spacer(modifier = Modifier.width(12.dp))

        if (value != null) {
            Text(
                text = value,
                color = TextSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

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
private fun SegmentRow(
    title: String,
    options: List<Int>,
    selected: Int,
    optionLabel: (Int) -> String,
    onSelect: (Int) -> Unit,
    testTagPrefix: String
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
            text = title,
            color = TextPrimary,
            fontSize = 16.sp
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface)
                .padding(2.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) TextMuted else DarkSurface)
                        .clickable { onSelect(option) }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("${testTagPrefix}_$option"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = optionLabel(option),
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
internal fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        thickness = 0.5.dp,
        color = DarkBorder
    )
}
