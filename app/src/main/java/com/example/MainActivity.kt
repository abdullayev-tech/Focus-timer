package com.example

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

    var showCreateSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

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
                    onDismissAlert = { viewModel.dismissAlert() }
                )
            } else {
                TimerListScreen(
                    timers = timers,
                    onAddTimerClick = { showCreateSheet = true },
                    onTimerSelect = { timer -> viewModel.openTimer(timer, autoStart = false) },
                    onDeleteTimer = { timer -> viewModel.deleteTimer(timer) },
                    onReorderTimers = { from, to -> viewModel.reorderTimers(from, to) },
                    onOpenSettings = { showSettingsDialog = true }
                )
            }
        }

        // Create Timer Sheet
        if (showCreateSheet) {
            CreateTimerSheet(
                onDismiss = { showCreateSheet = false },
                onCreateTimer = { label, totalSeconds, autoStart ->
                    viewModel.createTimer(label, totalSeconds, autoOpen = autoStart)
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
                isTestingAlert = isAlerting,
                testVolume = alertVolume,
                onSelectMode = { viewModel.updateSoundMode(it) },
                onSelectCrescendoDuration = { viewModel.updateCrescendoDuration(it) },
                onToggleHaptics = { viewModel.updateHapticsEnabled(it) },
                onSelectVibrationMode = { viewModel.updateVibrationMode(it) },
                onToggleTestAlert = { viewModel.testAlertSound() },
                onDismiss = {
                    if (isAlerting) viewModel.dismissAlert()
                    showSettingsDialog = false
                }
            )
        }
    }
}
