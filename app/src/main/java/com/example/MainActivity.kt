package com.example

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import com.example.ui.screens.CreateTimerSheet
import com.example.ui.screens.FocusTimerScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.TimerListScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OLEDBlack
import com.example.viewmodel.TimerViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: TimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // The volume buttons change the volume the alert actually plays at
        volumeControlStream = AudioManager.STREAM_ALARM

        setContent {
            MyApplicationTheme {
                TimerApp(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.alertManager.stopAlert()
    }
}

@Composable
fun TimerApp(viewModel: TimerViewModel) {
    val timers by viewModel.timers.collectAsState()
    val activeState by viewModel.activeState.collectAsState()
    val isFullScreen by viewModel.isFullScreen.collectAsState()
    val isAlerting by viewModel.alertManager.isAlerting.collectAsState()
    val alertVolume by viewModel.alertManager.currentVolume.collectAsState()
    val soundMode by viewModel.selectedSoundMode.collectAsState()
    val crescendoDuration by viewModel.crescendoDuration.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val vibrationMode by viewModel.vibrationMode.collectAsState()
    val ringtoneUri by viewModel.ringtoneUri.collectAsState()
    val autoStopMinutes by viewModel.autoStopMinutes.collectAsState()

    var showCreateSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Keep the screen awake while the clock runs and while the alert sounds.
    // Once the alert has stopped it can sleep, or a timer left alone would hold it on forever
    val view = LocalView.current
    val keepAwake = (activeState.isRunning && !activeState.isCompleted) || isAlerting
    DisposableEffect(view, keepAwake) {
        view.keepScreenOn = keepAwake
        onDispose { view.keepScreenOn = false }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OLEDBlack)
    ) {
        AnimatedContent(
            targetState = isFullScreen,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "screen_transition"
        ) { fullScreen ->
            if (fullScreen) {
                FocusTimerScreen(
                    state = activeState,
                    isAlerting = isAlerting,
                    onBack = { viewModel.closeFullScreen() },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onReset = { viewModel.resetTimer() },
                    onSilenceAlert = { viewModel.silenceAlert() },
                    defaultSound = soundMode,
                    onSelectSound = { viewModel.setActiveTimerSound(it) }
                )
            } else {
                TimerListScreen(
                    timers = timers,
                    runningTimerId = activeState.timerEntity?.id.takeIf { activeState.isRunning },
                    runningTimeText = activeState.formattedRemaining(),
                    onAddTimerClick = { showCreateSheet = true },
                    onTimerSelect = { timer -> viewModel.openTimer(timer, autoStart = false) },
                    onDeleteTimer = { timer -> viewModel.deleteTimer(timer) },
                    onSaveOrder = { ordered -> viewModel.saveTimerOrder(ordered) },
                    onOpenSettings = { showSettingsDialog = true }
                )
            }
        }

        // Create Timer Sheet
        if (showCreateSheet) {
            CreateTimerSheet(
                defaultSound = soundMode,
                onPreviewSound = { viewModel.previewSound(it) },
                onDismiss = { showCreateSheet = false },
                onCreateTimer = { label, totalSeconds, autoStart, sound ->
                    viewModel.createTimer(label, totalSeconds, autoOpen = autoStart, sound = sound)
                }
            )
        }

        // Settings Dialog
        if (showSettingsDialog) {
            SettingsDialog(
                selectedMode = soundMode,
                crescendoDuration = crescendoDuration,
                hapticsEnabled = hapticsEnabled,
                vibrationMode = vibrationMode,
                ringtoneUri = ringtoneUri,
                autoStopMinutes = autoStopMinutes,
                isTestingAlert = isAlerting,
                testVolume = alertVolume,
                onSelectMode = { viewModel.updateSoundMode(it) },
                onPickRingtone = { viewModel.updateRingtone(it) },
                onSelectAutoStop = { viewModel.updateAutoStopMinutes(it) },
                onSelectCrescendoDuration = { viewModel.updateCrescendoDuration(it) },
                onToggleHaptics = { viewModel.updateHapticsEnabled(it) },
                onSelectVibrationMode = { viewModel.updateVibrationMode(it) },
                onToggleTestAlert = { viewModel.testAlertSound() },
                onDismiss = {
                    viewModel.silenceAlert()
                    showSettingsDialog = false
                }
            )
        }
    }
}
