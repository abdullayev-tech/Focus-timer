package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.GentleSoundMode
import com.example.ui.components.TimeWheelPicker
import com.example.ui.theme.AppleTimerOrange
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTimerSheet(
    defaultSound: GentleSoundMode,
    onPreviewSound: (GentleSoundMode?) -> Unit,
    onDismiss: () -> Unit,
    onCreateTimer: (label: String, totalSeconds: Long, autoStart: Boolean, sound: GentleSoundMode?) -> Unit,
    onStartOneTime: (label: String, totalSeconds: Long, sound: GentleSoundMode?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Default to clean 2 minutes
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(2) }
    var seconds by remember { mutableIntStateOf(0) }
    var label by remember { mutableStateOf("Focus") }
    var sound by remember { mutableStateOf<GentleSoundMode?>(null) }
    var showSoundSheet by remember { mutableStateOf(false) }
    var isOneTime by remember { mutableStateOf(false) }

    val totalSeconds = hours * 3600L + minutes * 60L + seconds
    val hasTime = totalSeconds > 0L
    // A one-time timer can only be started: there's nothing to save
    val canSave = hasTime && !isOneTime

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = { SheetDragHandle() }
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
                        onCreateTimer(label, totalSeconds, false, sound)
                        onDismiss()
                    },
                    enabled = canSave,
                    modifier = Modifier.testTag("save_timer_button")
                ) {
                    Text(
                        text = "Save",
                        color = if (canSave) AppleTimerOrange else TextMuted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TimeWheelPicker(
                initialHours = hours,
                initialMinutes = minutes,
                initialSeconds = seconds,
                onHoursChange = { hours = it },
                onMinutesChange = { minutes = it },
                onSecondsChange = { seconds = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

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

            Spacer(modifier = Modifier.height(12.dp))

            // One row, as tall as a single cell: this timer's own sound, and whether it's kept
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceVariant)
                        .clickable { showSoundSheet = true }
                        .padding(start = 14.dp, end = 8.dp)
                        .testTag("timer_sound_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SheetCellText(
                        title = "Sound",
                        value = sound?.title ?: "Default",
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceVariant)
                        .toggleable(
                            value = isOneTime,
                            role = Role.Switch,
                            onValueChange = { isOneTime = it }
                        )
                        .padding(start = 14.dp, end = 10.dp)
                        .testTag("one_time_toggle"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SheetCellText(
                        title = "One-time",
                        value = if (isOneTime) "Won't be saved" else "Off",
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (isOneTime) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isOneTime) AppleTimerOrange else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (isOneTime) {
                        onStartOneTime(label, totalSeconds, sound)
                    } else {
                        onCreateTimer(label, totalSeconds, true, sound)
                    }
                },
                enabled = hasTime,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppleTimerOrange,
                    contentColor = Color.Black,
                    disabledContainerColor = DarkSurfaceVariant,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_timer_button")
            ) {
                Text(
                    text = "Start",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showSoundSheet) {
        TimerSoundSheet(
            selected = sound,
            defaultSound = defaultSound,
            onSelect = {
                sound = it
                onPreviewSound(it)
            },
            onDismiss = { showSoundSheet = false }
        )
    }
}

// Two short lines, so half a row holds what a whole row used to
@Composable
private fun SheetCellText(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
