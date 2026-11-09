package com.example.ui.screens

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.FADE_IN_STOPS
import com.example.audio.GentleSoundMode
import com.example.audio.GentleVibrationMode
import com.example.ui.theme.AppleTimerOrange
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

// In minutes; 0 is never
internal val AUTO_STOP_OPTIONS = listOf(1, 3, 5, 0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    selectedMode: GentleSoundMode,
    crescendoDuration: Int,
    hapticsEnabled: Boolean,
    vibrationMode: GentleVibrationMode,
    ringtoneUri: Uri?,
    autoStopMinutes: Int,
    timeInStatusBar: Boolean,
    isTestingAlert: Boolean,
    testVolume: Float,
    onSelectMode: (GentleSoundMode) -> Unit,
    onPickRingtone: (Uri) -> Unit,
    onSelectAutoStop: (Int) -> Unit,
    onSelectCrescendoDuration: (Int) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onSelectVibrationMode: (GentleVibrationMode) -> Unit,
    onSelectTimeInStatusBar: (Boolean) -> Unit,
    onToggleTestAlert: () -> Unit,
    onOpenDev: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val devTaps = remember { RapidTaps(DEV_TAP_COUNT, DEV_TAP_GAP_MILLIS) }
    var showDevPassword by remember { mutableStateOf(false) }

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
            SheetHeader(title = "Alert", onDone = onDismiss) {
                // Left blank on purpose: only the owner knows it's here
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("dev_button")
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                if (devTaps.tap(SystemClock.uptimeMillis())) showDevPassword = true
                            })
                        }
                )
            }

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
                FadeInSlider(
                    stops = FADE_IN_STOPS,
                    selected = crescendoDuration,
                    stopLabel = { "${it}s" },
                    onSelect = onSelectCrescendoDuration
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

            // Only Android 16 and later can show the time up there
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                SettingsGroup(title = "Status bar", footer = "While a timer runs") {
                    CheckRow(
                        title = "Time left",
                        isSelected = timeInStatusBar,
                        onClick = { onSelectTimeInStatusBar(true) },
                        modifier = Modifier.testTag("status_bar_time")
                    )
                    RowDivider()
                    CheckRow(
                        title = "Icon only",
                        isSelected = !timeInStatusBar,
                        onClick = { onSelectTimeInStatusBar(false) },
                        modifier = Modifier.testTag("status_bar_icon")
                    )
                }
            }

            TestAlertGroup(
                isTesting = isTestingAlert,
                testVolume = testVolume,
                onToggle = onToggleTestAlert
            )
        }
    }

    if (showDevPassword) {
        DevPasswordDialog(
            onUnlock = {
                showDevPassword = false
                onOpenDev()
            },
            onDismiss = { showDevPassword = false }
        )
    }
}

@Composable
internal fun TestAlertGroup(isTesting: Boolean, testVolume: Float, onToggle: () -> Unit) {
    SettingsGroup {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clickable(onClick = onToggle)
                .testTag("test_alert_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isTesting) "Stop Test" else "Test Alert",
                color = AppleTimerOrange,
                fontSize = 16.sp
            )
        }

        // The fade-in starts almost silent, so show that the test is really running
        if (isTesting) {
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
internal fun SheetHeader(
    title: String,
    onDone: () -> Unit,
    leading: (@Composable () -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        if (leading != null) {
            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                leading()
            }
        }

        Text(
            text = title,
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            // A timer's label can be long: it stops short of the buttons on either side
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 72.dp)
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
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null
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
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(12.dp))
        }

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

/**
 * Fade-in as a slider, one stop at a time, with a tick under the finger at each.
 * Nothing is saved until the finger lifts.
 */
@Composable
internal fun <T> FadeInSlider(
    stops: List<T>,
    selected: T,
    stopLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    val view = LocalView.current
    var position by remember(selected) {
        mutableFloatStateOf(stops.indexOf(selected).coerceAtLeast(0).toFloat())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Fade in",
                color = TextPrimary,
                fontSize = 16.sp
            )
            Text(
                text = stopLabel(stops[position.roundToInt()]),
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.testTag("fade_in_value")
            )
        }

        Slider(
            value = position,
            onValueChange = { dragged ->
                if (dragged.roundToInt() != position.roundToInt()) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
                position = dragged
            },
            onValueChangeFinished = { onSelect(stops[position.roundToInt()]) },
            valueRange = 0f..stops.lastIndex.toFloat(),
            steps = stops.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = AppleTimerOrange,
                activeTrackColor = AppleTimerOrange,
                activeTickColor = DarkSurfaceVariant,
                inactiveTrackColor = DarkBorder,
                inactiveTickColor = TextMuted
            ),
            modifier = Modifier.testTag("fade_in_slider")
        )
    }
}

@Composable
internal fun <T> SegmentRow(
    title: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
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
