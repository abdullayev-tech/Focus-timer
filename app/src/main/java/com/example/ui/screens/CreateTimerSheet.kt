package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onDismiss: () -> Unit,
    onCreateTimer: (label: String, totalSeconds: Long, autoStart: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Default to clean 2 minutes
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(2) }
    var seconds by remember { mutableIntStateOf(0) }
    var label by remember { mutableStateOf("Focus") }

    val totalSeconds = hours * 3600L + minutes * 60L + seconds
    val hasTime = totalSeconds > 0L

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
                        onCreateTimer(label, totalSeconds, false)
                        onDismiss()
                    },
                    enabled = hasTime,
                    modifier = Modifier.testTag("save_timer_button")
                ) {
                    Text(
                        text = "Save",
                        color = if (hasTime) AppleTimerOrange else TextMuted,
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

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    onCreateTimer(label, totalSeconds, true)
                    onDismiss()
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
}
