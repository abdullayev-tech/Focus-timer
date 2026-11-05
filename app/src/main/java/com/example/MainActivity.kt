package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import com.example.session.FocusSession
import com.example.ui.screens.CreateTimerSheet
import com.example.ui.screens.DevScreen
import com.example.ui.screens.FocusTimerScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.SwitchSessionDialog
import com.example.ui.screens.TimerListScreen
import com.example.ui.screens.TimerSettingsSheet
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OLEDBlack
import com.example.viewmodel.TimerViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: TimerViewModel by viewModels()
    private val session by lazy { FocusSession.get(this) }

    // The app is always dark. Told so here, the system bars and every sheet's window get
    // light icons on a phone set to light mode too
    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            Configuration.UI_MODE_NIGHT_YES
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null) session.showSessionInProgress()

        // The volume buttons change the volume the alert actually plays at
        volumeControlStream = AudioManager.STREAM_ALARM

        setContent {
            MyApplicationTheme {
                TimerApp(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        session.setUiVisible(true)
    }

    override fun onStop() {
        super.onStop()
        session.setUiVisible(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        session.onUiDestroyed()
    }
}

// Nothing ends until the person says so
private class PendingSwitch(val label: String, val start: () -> Unit)

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
    val stoppedRingColor by viewModel.stoppedRingColor.collectAsState()

    var showCreateSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDevScreen by remember { mutableStateOf(false) }
    var showStatsScreen by remember { mutableStateOf(false) }
    var settingsTimerId by remember { mutableStateOf<Long?>(null) }
    var pendingSwitch by remember { mutableStateOf<PendingSwitch?>(null) }
    // Looked up by id so the sheet always shows what's saved
    val settingsTimer = timers.firstOrNull { it.id == settingsTimerId }

    // Keep the screen awake while the clock runs and while the alert sounds.
    // Once the alert has stopped it can sleep, or a timer left alone would hold it on forever
    val view = LocalView.current
    val keepAwake = (activeState.isRunning && !activeState.isCompleted) || isAlerting
    DisposableEffect(view, keepAwake) {
        view.keepScreenOn = keepAwake
        onDispose { view.keepScreenOn = false }
    }

    // Asked once, as the first session starts. The alert sounds either way
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.refreshNotice()
    }
    LaunchedEffect(activeState.isRunning) {
        if (activeState.isRunning &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(view.context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED &&
            viewModel.shouldAskForNotifications()
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Time ran out while the question was open: there's nothing left to ask about
    val isClockRunning = activeState.isRunning && !activeState.isCompleted
    LaunchedEffect(isClockRunning) {
        if (!isClockRunning) pendingSwitch = null
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
                    onSilenceAlert = { viewModel.silenceSession() },
                    stoppedRingColor = stoppedRingColor
                )
            } else {
                TimerListScreen(
                    timers = timers,
                    runningTimerId = activeState.timerEntity?.id.takeIf { activeState.isRunning },
                    runningTimeText = activeState.formattedRemaining(),
                    oneTimeTimer = activeState.timerEntity?.takeIf { it.isOneTime && activeState.isRunning },
                    onAddTimerClick = { showCreateSheet = true },
                    onTimerSelect = { timer ->
                        if (isClockRunning && timer.id != activeState.timerEntity?.id) {
                            pendingSwitch = PendingSwitch(timer.label) {
                                viewModel.openTimer(timer, autoStart = true)
                            }
                        } else {
                            viewModel.openTimer(timer, autoStart = false)
                        }
                    },
                    onOpenTimerSettings = { timer -> settingsTimerId = timer.id },
                    onSaveOrder = { ordered -> viewModel.saveTimerOrder(ordered) },
                    onOpenSettings = { showSettingsDialog = true },
                    onOpenStats = { showStatsScreen = true },
                    stoppedRingColor = stoppedRingColor
                )
            }
        }

        // Stats. Gives way when time is up, like the dev screen
        if (showStatsScreen && !isFullScreen) {
            val events by remember { viewModel.recentEvents() }.collectAsState(initial = null)
            StatsScreen(
                events = events,
                onBack = { showStatsScreen = false }
            )
        }

        // Dev Screen. Gives way when time is up: the focus screen is where the alert is silenced
        if (showDevScreen && !isFullScreen) {
            DevScreen(
                stoppedRingColor = stoppedRingColor,
                onSelectStoppedRingColor = { viewModel.updateStoppedRingColor(it) },
                onBack = {
                    showDevScreen = false
                    showSettingsDialog = true
                }
            )
        }

        // Create Timer Sheet
        if (showCreateSheet) {
            CreateTimerSheet(
                defaultSound = soundMode,
                onPreviewSound = { viewModel.previewSound(it) },
                onDismiss = { showCreateSheet = false },
                // Answering "Keep going" comes back to this sheet with nothing lost and nothing saved
                onCreateTimer = { label, totalSeconds, autoStart, sound ->
                    val create = {
                        viewModel.createTimer(label, totalSeconds, autoOpen = autoStart, sound = sound)
                        showCreateSheet = false
                    }
                    if (autoStart && isClockRunning) {
                        pendingSwitch = PendingSwitch(label, create)
                    } else {
                        create()
                    }
                },
                onStartOneTime = { label, totalSeconds, sound ->
                    val start = {
                        viewModel.startOneTimeTimer(label, totalSeconds, sound)
                        showCreateSheet = false
                    }
                    if (isClockRunning) {
                        pendingSwitch = PendingSwitch(label, start)
                    } else {
                        start()
                    }
                }
            )
        }

        // Starting something else while the clock runs
        pendingSwitch?.let { switch ->
            SwitchSessionDialog(
                runningLabel = activeState.timerEntity?.label.orEmpty(),
                runningTimeText = activeState.formattedRemaining(),
                nextLabel = switch.label,
                onKeepGoing = { pendingSwitch = null },
                onStartNext = {
                    switch.start()
                    pendingSwitch = null
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
                onOpenDev = {
                    viewModel.silenceAlert()
                    showSettingsDialog = false
                    showDevScreen = true
                },
                onDismiss = {
                    viewModel.silenceAlert()
                    showSettingsDialog = false
                }
            )
        }

        // One timer's own settings
        settingsTimer?.let { timer ->
            TimerSettingsSheet(
                timer = timer,
                defaultSound = soundMode,
                defaultFadeInSeconds = crescendoDuration,
                defaultVibration = vibrationMode.takeIf { hapticsEnabled },
                isTestingAlert = isAlerting,
                testVolume = alertVolume,
                onUpdate = { viewModel.updateTimer(it) },
                onPreviewSound = { viewModel.previewSound(it) },
                onPreviewVibration = { viewModel.previewVibration(it) },
                onToggleTestAlert = { viewModel.testTimerAlert(timer) },
                onDelete = {
                    viewModel.silenceAlert()
                    viewModel.deleteTimer(timer)
                    settingsTimerId = null
                },
                onDismiss = {
                    viewModel.silenceAlert()
                    settingsTimerId = null
                }
            )
        }
    }
}
