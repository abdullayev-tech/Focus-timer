package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.GentleAlertManager
import com.example.audio.GentleSoundMode
import com.example.audio.GentleVibrationMode
import com.example.audio.VIBRATION_OFF_NAME
import com.example.audio.soundModeNamed
import com.example.audio.vibrationModeNamed
import com.example.data.EventLogDatabase
import com.example.data.TimerDatabase
import com.example.data.TimerEntity
import com.example.data.TimerEvent
import com.example.data.TimerEventType
import com.example.data.TimerPreferences
import com.example.data.TimerRepository
import com.example.data.lastSevenDayStarts
import com.example.ui.theme.StoppedRingColor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ActiveTimerState(
    val timerEntity: TimerEntity? = null,
    val totalSeconds: Long = 0L,
    val remainingMillis: Long = 0L,
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false
) {
    val progress: Float
        get() = if (totalSeconds > 0) {
            if (isCompleted) {
                1f
            } else {
                (remainingMillis.toFloat() / (totalSeconds * 1000f)).coerceIn(0f, 1f)
            }
        } else 0f

    fun formattedRemaining(): String {
        return formatClock((remainingMillis.coerceAtLeast(0L) + 999L) / 1000L)
    }

    // How long ago time was up; null until then
    fun formattedOvertime(): String? {
        return if (isCompleted) formatClock(-remainingMillis / 1000L) else null
    }

    private fun formatClock(s: Long): String {
        val hours = s / 3600
        val minutes = (s % 3600) / 60
        val seconds = s % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TimerRepository
    private val prefs = TimerPreferences(application)
    private val eventDao = EventLogDatabase.getDatabase(application).timerEventDao()
    val alertManager = GentleAlertManager(application)

    val timers: StateFlow<List<TimerEntity>>

    private val _activeState = MutableStateFlow(ActiveTimerState())
    val activeState: StateFlow<ActiveTimerState> = _activeState.asStateFlow()

    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen: StateFlow<Boolean> = _isFullScreen.asStateFlow()

    private val _selectedSoundMode = MutableStateFlow(prefs.getSoundMode())
    val selectedSoundMode: StateFlow<GentleSoundMode> = _selectedSoundMode.asStateFlow()

    private val _crescendoDuration = MutableStateFlow(prefs.getCrescendoDuration())
    val crescendoDuration: StateFlow<Int> = _crescendoDuration.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(prefs.getHapticsEnabled())
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _vibrationMode = MutableStateFlow(prefs.getVibrationMode())
    val vibrationMode: StateFlow<GentleVibrationMode> = _vibrationMode.asStateFlow()

    private val _ringtoneUri = MutableStateFlow(prefs.getRingtoneUri())
    val ringtoneUri: StateFlow<Uri?> = _ringtoneUri.asStateFlow()

    private val _autoStopMinutes = MutableStateFlow(prefs.getAutoStopMinutes())
    val autoStopMinutes: StateFlow<Int> = _autoStopMinutes.asStateFlow()

    private val _stoppedRingColor = MutableStateFlow(prefs.getStoppedRingColor())
    val stoppedRingColor: StateFlow<StoppedRingColor> = _stoppedRingColor.asStateFlow()

    private var countdownJob: Job? = null
    private var lastTickTime: Long = 0L

    // What the clock said when it last began running; null while it isn't
    private var stretchStartMillis: Long? = null

    init {
        // Sync alert manager with saved preferences
        alertManager.soundMode = _selectedSoundMode.value
        alertManager.crescendoDurationSeconds = _crescendoDuration.value
        alertManager.hapticsEnabled = _hapticsEnabled.value
        alertManager.vibrationMode = _vibrationMode.value
        alertManager.ringtoneUri = _ringtoneUri.value
        alertManager.autoStopMinutes = _autoStopMinutes.value

        val dao = TimerDatabase.getDatabase(application).timerDao()
        repository = TimerRepository(dao)
        timers = repository.allTimers.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    }

    fun openTimer(timer: TimerEntity, autoStart: Boolean = false) {
        // Its session is still running: go back to it, don't restart it
        val current = _activeState.value
        if (current.isRunning && current.timerEntity?.id == timer.id) {
            logSession(TimerEventType.RETURNED)
            _isFullScreen.value = true
            return
        }

        // This takes the place of whatever was still running
        if (current.isRunning) {
            logSession(TimerEventType.CLOSED, takeRanMillis())
        }

        countdownJob?.cancel()
        _activeState.value = ActiveTimerState(
            timerEntity = timer,
            totalSeconds = timer.totalSeconds,
            remainingMillis = timer.totalSeconds * 1000L,
            isRunning = false,
            isCompleted = false
        )
        _isFullScreen.value = true
        if (autoStart) {
            startTimer()
        } else {
            logSession(TimerEventType.OPENED)
        }
    }

    /** Leaving a running session keeps it going; leaving a paused or finished one ends it. */
    fun closeFullScreen() {
        val current = _activeState.value
        if (!current.isRunning || current.isCompleted) {
            logSession(TimerEventType.CLOSED, takeRanMillis())
            clearClock()
        } else {
            logSession(TimerEventType.LEFT)
        }
        _isFullScreen.value = false
    }

    fun startTimer() {
        val current = _activeState.value
        val isFromTheTop = current.remainingMillis <= 0L || current.isCompleted ||
            current.remainingMillis >= current.totalSeconds * 1000L
        if (current.remainingMillis <= 0L || current.isCompleted) {
            // Reset to full if starting from completed state
            _activeState.value = current.copy(
                remainingMillis = current.totalSeconds * 1000L,
                isCompleted = false
            )
        }

        silenceAlert()

        _activeState.value = _activeState.value.copy(isRunning = true, isCompleted = false)
        lastTickTime = System.currentTimeMillis()
        stretchStartMillis = _activeState.value.remainingMillis
        logSession(if (isFromTheTop) TimerEventType.STARTED else TimerEventType.RESUMED)

        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive && _activeState.value.isRunning) {
                delay(50L) // 50ms tick for fluid circular animation
                val now = System.currentTimeMillis()
                val delta = now - lastTickTime
                lastTickTime = now

                val updatedMillis = _activeState.value.remainingMillis - delta
                val justFinished = updatedMillis <= 0L && !_activeState.value.isCompleted
                _activeState.value = _activeState.value.copy(
                    remainingMillis = updatedMillis,
                    isCompleted = updatedMillis <= 0L
                )
                if (justFinished) {
                    logSession(TimerEventType.FINISHED, takeRanMillis())
                    startAlertFor(_activeState.value.timerEntity)
                    // The focus screen is where the alert can be silenced
                    _isFullScreen.value = true
                }
            }
        }
    }

    fun pauseTimer() {
        countdownJob?.cancel()
        _activeState.value = _activeState.value.copy(isRunning = false)
        logSession(TimerEventType.PAUSED, takeRanMillis())
    }

    fun togglePlayPause() {
        if (_activeState.value.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    fun resetTimer() {
        logSession(TimerEventType.RESET, takeRanMillis())
        clearClock()
    }

    private fun clearClock() {
        countdownJob?.cancel()
        if (alertManager.isAlerting.value) {
            alertManager.stopAlert()
        }
        val current = _activeState.value
        _activeState.value = current.copy(
            remainingMillis = current.totalSeconds * 1000L,
            isRunning = false,
            isCompleted = false
        )
    }

    /** Stops the sound and vibration only. Past zero, the clock keeps counting. */
    fun silenceAlert() {
        alertManager.stopAlert()
    }

    /** The person silencing a session's alert, as opposed to leaving a settings test. */
    fun silenceSession() {
        if (alertManager.isAlerting.value) {
            logSession(TimerEventType.SILENCED)
        }
        silenceAlert()
    }

    fun recentEvents(): Flow<List<TimerEvent>> {
        return eventDao.eventsSince(lastSevenDayStarts(System.currentTimeMillis()).first())
    }

    private fun logSession(type: TimerEventType, ranMillis: Long? = null) {
        val state = _activeState.value
        log(type, state.timerEntity, state.remainingMillis, ranMillis)
    }

    private fun log(
        type: TimerEventType,
        timer: TimerEntity? = null,
        remainingMillis: Long? = null,
        ranMillis: Long? = null
    ) {
        val event = TimerEvent(
            type = type,
            timerId = timer?.id,
            label = timer?.label,
            totalSeconds = timer?.totalSeconds,
            remainingMillis = remainingMillis,
            ranMillis = ranMillis
        )
        viewModelScope.launch {
            eventDao.insert(event)
        }
    }

    // How much of the timer this stretch of running used. Only the first caller gets it
    private fun takeRanMillis(): Long? {
        val start = stretchStartMillis ?: return null
        stretchStartMillis = null
        return start - _activeState.value.remainingMillis.coerceAtLeast(0L)
    }

    /** Runs right away and is never saved. Once its session is over, nothing is left of it. */
    fun startOneTimeTimer(label: String, totalSeconds: Long, sound: GentleSoundMode? = null) {
        val timer = TimerEntity(
            id = -System.currentTimeMillis(),
            label = label.ifBlank { "Focus Block" },
            totalSeconds = totalSeconds,
            soundName = sound?.name
        )
        log(TimerEventType.CREATED, timer)
        openTimer(timer, autoStart = true)
    }

    fun createTimer(
        label: String,
        totalSeconds: Long,
        autoOpen: Boolean = true,
        sound: GentleSoundMode? = null
    ) {
        viewModelScope.launch {
            val maxOrder = timers.value.maxOfOrNull { it.displayOrder } ?: -1
            val newTimer = TimerEntity(
                label = label.ifBlank { "Focus Block" },
                totalSeconds = totalSeconds,
                displayOrder = maxOrder + 1,
                soundName = sound?.name
            )
            val id = repository.insert(newTimer)
            val savedTimer = newTimer.copy(id = id)
            log(TimerEventType.CREATED, savedTimer)
            if (autoOpen) {
                openTimer(savedTimer, autoStart = true)
            }
        }
    }

    /** The timer's own choices where it has them, otherwise the defaults from settings. */
    private fun startAlertFor(timer: TimerEntity?) {
        alertManager.startGentleAlert(
            mode = soundModeNamed(timer?.soundName) ?: _selectedSoundMode.value,
            fadeInSeconds = timer?.fadeInSeconds ?: _crescendoDuration.value,
            stopAfterMinutes = timer?.autoStopMinutes ?: _autoStopMinutes.value,
            vibration = when (timer?.vibrationName) {
                VIBRATION_OFF_NAME -> null
                else -> vibrationModeNamed(timer?.vibrationName)
                    ?: _vibrationMode.value.takeIf { _hapticsEnabled.value }
            }
        )
    }

    /** Saves a timer's own alert choices. A session already running with it uses them too. */
    fun updateTimer(timer: TimerEntity) {
        if (_activeState.value.timerEntity?.id == timer.id) {
            _activeState.value = _activeState.value.copy(timerEntity = timer)
        }
        log(TimerEventType.EDITED, timer)
        viewModelScope.launch {
            repository.update(timer)
        }
    }

    fun testTimerAlert(timer: TimerEntity) {
        if (alertManager.isAlerting.value) {
            alertManager.stopAlert()
        } else {
            startAlertFor(timer)
        }
    }

    fun previewVibration(mode: GentleVibrationMode) {
        alertManager.triggerVibrationPreview(mode)
    }

    fun previewSound(sound: GentleSoundMode?) {
        alertManager.previewSound(sound ?: _selectedSoundMode.value)
    }

    fun saveTimerOrder(ordered: List<TimerEntity>) {
        val reordered = ordered.mapIndexed { index, timer ->
            timer.copy(displayOrder = index)
        }
        log(TimerEventType.REORDERED)
        viewModelScope.launch {
            repository.updateAll(reordered)
        }
    }

    fun deleteTimer(timer: TimerEntity) {
        viewModelScope.launch {
            if (_activeState.value.timerEntity?.id == timer.id) {
                if (_activeState.value.isRunning) {
                    logSession(TimerEventType.CLOSED, takeRanMillis())
                }
                clearClock()
                _isFullScreen.value = false
            }
            log(TimerEventType.DELETED, timer)
            repository.delete(timer)
        }
    }

    fun updateSoundMode(mode: GentleSoundMode) {
        _selectedSoundMode.value = mode
        alertManager.soundMode = mode
        prefs.setSoundMode(mode)
        log(TimerEventType.ALERT_CHANGED)
        alertManager.previewSound()
    }

    fun updateRingtone(uri: Uri) {
        _ringtoneUri.value = uri
        alertManager.ringtoneUri = uri
        prefs.setRingtoneUri(uri)
        updateSoundMode(GentleSoundMode.SYSTEM_GENTLE)
    }

    fun updateAutoStopMinutes(minutes: Int) {
        _autoStopMinutes.value = minutes
        alertManager.autoStopMinutes = minutes
        prefs.setAutoStopMinutes(minutes)
        log(TimerEventType.ALERT_CHANGED)
    }

    fun updateStoppedRingColor(color: StoppedRingColor) {
        _stoppedRingColor.value = color
        prefs.setStoppedRingColor(color)
    }

    fun updateCrescendoDuration(seconds: Int) {
        _crescendoDuration.value = seconds
        alertManager.crescendoDurationSeconds = seconds
        prefs.setCrescendoDuration(seconds)
        log(TimerEventType.ALERT_CHANGED)
    }

    fun updateHapticsEnabled(enabled: Boolean) {
        _hapticsEnabled.value = enabled
        alertManager.hapticsEnabled = enabled
        prefs.setHapticsEnabled(enabled)
        // Turning it on always comes with picking a vibration, which logs itself
        if (!enabled) log(TimerEventType.ALERT_CHANGED)
    }

    fun updateVibrationMode(mode: GentleVibrationMode) {
        _vibrationMode.value = mode
        alertManager.vibrationMode = mode
        prefs.setVibrationMode(mode)
        log(TimerEventType.ALERT_CHANGED)
        alertManager.triggerVibrationPreview(mode)
    }

    fun testAlertSound() {
        if (alertManager.isAlerting.value) {
            alertManager.stopAlert()
        } else {
            alertManager.startGentleAlert()
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        alertManager.stopAlert()
    }
}
