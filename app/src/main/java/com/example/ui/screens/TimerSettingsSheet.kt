package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.FADE_IN_STOPS
import com.example.audio.GentleSoundMode
import com.example.audio.GentleVibrationMode
import com.example.audio.VIBRATION_OFF_NAME
import com.example.audio.soundModeNamed
import com.example.audio.vibrationModeNamed
import com.example.data.TimerEntity
import com.example.ui.theme.DarkSurface

private val TimerDeleteRed = Color(0xFFFF453A)

/**
 * One timer's own alert, laid out like the settings sheet. Every choice starts on "Default",
 * which is whatever is set there; the row says what that is at the moment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerSettingsSheet(
    timer: TimerEntity,
    defaultSound: GentleSoundMode,
    defaultFadeInSeconds: Int,
    defaultVibration: GentleVibrationMode?,
    isTestingAlert: Boolean,
    testVolume: Float,
    onUpdate: (TimerEntity) -> Unit,
    onPreviewSound: (GentleSoundMode?) -> Unit,
    onPreviewVibration: (GentleVibrationMode) -> Unit,
    onToggleTestAlert: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val selectedSound = soundModeNamed(timer.soundName)
    val selectedVibration = vibrationModeNamed(timer.vibrationName)
    val fadeInStops = listOf<Int?>(null) + FADE_IN_STOPS
    val autoStopOptions = listOf<Int?>(null) + AUTO_STOP_OPTIONS

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSurface,
        dragHandle = { SheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            SheetHeader(title = timer.label, onDone = onDismiss)

            SettingsGroup(title = "Sound", footer = "Default follows the Alert settings") {
                CheckRow(
                    title = "Default",
                    value = defaultSound.title,
                    isSelected = selectedSound == null,
                    onClick = {
                        onUpdate(timer.copy(soundName = null))
                        onPreviewSound(null)
                    },
                    modifier = Modifier.testTag("timer_sound_default")
                )
                GentleSoundMode.values().forEach { mode ->
                    RowDivider()
                    CheckRow(
                        title = mode.title,
                        isSelected = mode == selectedSound,
                        onClick = {
                            onUpdate(timer.copy(soundName = mode.name))
                            onPreviewSound(mode)
                        },
                        modifier = Modifier.testTag("timer_sound_${mode.name}")
                    )
                }
                RowDivider()
                FadeInSlider(
                    stops = fadeInStops,
                    selected = timer.fadeInSeconds,
                    stopLabel = { seconds ->
                        if (seconds == null) "Default (${defaultFadeInSeconds}s)" else "${seconds}s"
                    },
                    onSelect = { onUpdate(timer.copy(fadeInSeconds = it)) }
                )
                RowDivider()
                SegmentRow(
                    title = "Stop after",
                    options = autoStopOptions,
                    selected = timer.autoStopMinutes,
                    optionLabel = { minutes ->
                        when (minutes) {
                            null -> "Default"
                            0 -> "Never"
                            else -> "${minutes}m"
                        }
                    },
                    onSelect = { onUpdate(timer.copy(autoStopMinutes = it)) },
                    testTagPrefix = "timer_auto_stop"
                )
            }

            SettingsGroup(title = "Vibration") {
                CheckRow(
                    title = "Default",
                    value = defaultVibration?.title ?: "Off",
                    isSelected = timer.vibrationName == null,
                    onClick = {
                        onUpdate(timer.copy(vibrationName = null))
                        defaultVibration?.let(onPreviewVibration)
                    },
                    modifier = Modifier.testTag("timer_vibration_default")
                )
                RowDivider()
                CheckRow(
                    title = "Off",
                    isSelected = timer.vibrationName == VIBRATION_OFF_NAME,
                    onClick = { onUpdate(timer.copy(vibrationName = VIBRATION_OFF_NAME)) },
                    modifier = Modifier.testTag("timer_vibration_off")
                )
                GentleVibrationMode.values().forEach { mode ->
                    RowDivider()
                    CheckRow(
                        title = mode.title,
                        isSelected = mode == selectedVibration,
                        onClick = {
                            onUpdate(timer.copy(vibrationName = mode.name))
                            onPreviewVibration(mode)
                        },
                        modifier = Modifier.testTag("timer_vibration_${mode.name}")
                    )
                }
            }

            TestAlertGroup(
                isTesting = isTestingAlert,
                testVolume = testVolume,
                onToggle = onToggleTestAlert
            )

            SettingsGroup {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp)
                        .clickable(onClick = onDelete)
                        .testTag("delete_timer_${timer.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Delete Timer",
                        color = TimerDeleteRed,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
