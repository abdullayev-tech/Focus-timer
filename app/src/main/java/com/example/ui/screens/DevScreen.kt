package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.SimpleFocusRing
import com.example.ui.theme.AppleTimerOrange
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OLEDBlack
import com.example.ui.theme.StoppedRingColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

internal const val DEV_TAP_COUNT = 3
internal const val DEV_TAP_GAP_MILLIS = 600L
private const val DEV_PASSWORD = "1"

/** Counts a run of quick taps. A tap slower than [maxGapMillis] starts the run over. */
internal class RapidTaps(private val count: Int, private val maxGapMillis: Long) {
    private var taps = 0
    private var lastTapMillis = 0L

    /** True on the tap that completes the run. */
    fun tap(nowMillis: Long): Boolean {
        taps = if (taps > 0 && nowMillis - lastTapMillis <= maxGapMillis) taps + 1 else 1
        lastTapMillis = nowMillis
        if (taps < count) return false
        taps = 0
        return true
    }
}

@Composable
internal fun DevPasswordDialog(onUnlock: () -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var isWrong by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun submit() {
        if (password == DEV_PASSWORD) onUnlock() else isWrong = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        // A fourth quick tap lands outside the dialog and must not close it
        properties = DialogProperties(dismissOnClickOutside = false),
        containerColor = DarkSurface,
        title = {
            Text(
                text = "Dev password",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    isWrong = false
                },
                label = { Text("Password") },
                singleLine = true,
                isError = isWrong,
                supportingText = if (isWrong) ({ Text("Wrong password") }) else null,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    errorContainerColor = DarkSurfaceVariant,
                    focusedBorderColor = AppleTimerOrange,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    errorTextColor = TextPrimary,
                    focusedLabelColor = AppleTimerOrange,
                    unfocusedLabelColor = TextSecondary
                ),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .testTag("dev_password_field")
            )
        },
        confirmButton = {
            TextButton(
                onClick = { submit() },
                modifier = Modifier.testTag("dev_password_open")
            ) {
                Text(
                    text = "Open",
                    color = AppleTimerOrange,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = TextSecondary, fontSize = 16.sp)
            }
        }
    )
}

/**
 * The owner's test bench, reached from the top left of the Alert settings sheet:
 * three quick taps on the dev button, then the password.
 * Each thing being tried is one more SettingsGroup in the column below.
 */
@Composable
fun DevScreen(
    stoppedRingColor: StoppedRingColor,
    onSelectStoppedRingColor: (StoppedRingColor) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OLEDBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .testTag("dev_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "Dev",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Stays put above the list: the ring as it looks stopped, so a colour can be judged while picking
        SimpleFocusRing(
            progress = 1f,
            timeText = "25:00",
            isRunning = false,
            isCompleted = false,
            isAlerting = false,
            stoppedColor = stoppedRingColor,
            onTap = {},
            size = 240.dp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            SettingsGroup(title = "Stopped ring") {
                StoppedRingColor.values().forEachIndexed { index, color ->
                    if (index > 0) RowDivider()
                    CheckRow(
                        title = color.title,
                        isSelected = color == stoppedRingColor,
                        onClick = { onSelectStoppedRingColor(color) },
                        modifier = Modifier.testTag("ring_color_${color.name}"),
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(color.arc)
                            )
                        }
                    )
                }
            }
        }
    }
}
