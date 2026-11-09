package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.GentleAlertManager
import com.example.audio.GentleSoundMode
import com.example.audio.GentleVibrationMode
import com.example.data.EventLogDatabase
import com.example.data.TimerDatabase
import com.example.data.TimerEntity
import com.example.data.TimerEvent
import com.example.data.TimerEventType
import com.example.data.TimerPreferences
import com.example.data.TimerRepository
import com.example.data.lastSevenDayStarts
import com.example.session.ActiveTimerState
import com.example.session.FocusSession
import com.example.ui.theme.StoppedRingColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TimerRepository
    private val prefs = TimerPreferences(application)
    private val eventDao = EventLogDatabase.getDatabase(application).timerEventDao()
    private val session = FocusSession.get(application)
    val alertManager: GentleAlertManager = session.alertManager

    val timers: StateFlow<List<TimerEntity>>

    val activeState: StateFlow<ActiveTimerState> = session.activeState
    val isFullScreen: StateFlow<Boolean> = session.isFullScreen
    val timeInStatusBar: StateFlow<Boolean> = session.timeInStatusBar

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

    init {
        val dao = TimerDatabase.getDatabase(application).timerDao()
        repository = TimerRepository(dao)
        timers = repository.allTimers.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    }

    fun openTimer(timer: TimerEntity, autoStart: Boolean = false) {
        session.openTimer(timer, autoStart)
    }

    fun closeFullScreen() {
        session.closeFullScreen()
    }

    fun togglePlayPause() {
        session.togglePlayPause()
    }

    fun resetTimer() {
        session.resetTimer()
    }

    /** Asks at most once, and the app works either way. */
    fun shouldAskForNotifications(): Boolean {
        if (prefs.wasAskedForNotifications()) return false
        prefs.setAskedForNotifications()
        return true
    }

    fun refreshNotice() {
        session.refreshNotice()
    }

    /** Stops the sound and vibration only. Past zero, the clock keeps counting. */
    fun silenceAlert() {
        alertManager.stopAlert()
    }

    /** The person silencing a session's alert, as opposed to leaving a settings test. */
    fun silenceSession() {
        session.silenceSession()
    }

    fun recentEvents(): Flow<List<TimerEvent>> {
        return eventDao.eventsSince(lastSevenDayStarts(System.currentTimeMillis()).first())
    }

    /** Runs right away and is never saved. Once its session is over, nothing is left of it. */
    fun startOneTimeTimer(label: String, totalSeconds: Long, sound: GentleSoundMode? = null) {
        val timer = TimerEntity(
            id = -System.currentTimeMillis(),
            label = label.ifBlank { "Focus Block" },
            totalSeconds = totalSeconds,
            soundName = sound?.name
        )
        session.log(TimerEventType.CREATED, timer)
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
            session.log(TimerEventType.CREATED, savedTimer)
            if (autoOpen) {
                openTimer(savedTimer, autoStart = true)
            }
        }
    }

    /** Saves a timer's own alert choices. A session already running with it uses them too. */
    fun updateTimer(timer: TimerEntity) {
        session.onTimerEdited(timer)
        session.log(TimerEventType.EDITED, timer)
        viewModelScope.launch {
            repository.update(timer)
        }
    }

    fun testTimerAlert(timer: TimerEntity) {
        if (alertManager.isAlerting.value) {
            alertManager.stopAlert()
        } else {
            session.startAlertFor(timer)
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
        session.log(TimerEventType.REORDERED)
        viewModelScope.launch {
            repository.updateAll(reordered)
        }
    }

    fun deleteTimer(timer: TimerEntity) {
        viewModelScope.launch {
            session.onTimerDeleted(timer)
            session.log(TimerEventType.DELETED, timer)
            repository.delete(timer)
        }
    }

    fun updateSoundMode(mode: GentleSoundMode) {
        _selectedSoundMode.value = mode
        alertManager.soundMode = mode
        prefs.setSoundMode(mode)
        session.log(TimerEventType.ALERT_CHANGED)
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
        session.log(TimerEventType.ALERT_CHANGED)
    }

    fun updateStoppedRingColor(color: StoppedRingColor) {
        _stoppedRingColor.value = color
        prefs.setStoppedRingColor(color)
    }

    fun updateCrescendoDuration(seconds: Int) {
        _crescendoDuration.value = seconds
        alertManager.crescendoDurationSeconds = seconds
        prefs.setCrescendoDuration(seconds)
        session.log(TimerEventType.ALERT_CHANGED)
    }

    fun updateTimeInStatusBar(show: Boolean) {
        session.setTimeInStatusBar(show)
    }

    fun updateHapticsEnabled(enabled: Boolean) {
        _hapticsEnabled.value = enabled
        alertManager.hapticsEnabled = enabled
        prefs.setHapticsEnabled(enabled)
        // Turning it on always comes with picking a vibration, which logs itself
        if (!enabled) session.log(TimerEventType.ALERT_CHANGED)
    }

    fun updateVibrationMode(mode: GentleVibrationMode) {
        _vibrationMode.value = mode
        alertManager.vibrationMode = mode
        prefs.setVibrationMode(mode)
        session.log(TimerEventType.ALERT_CHANGED)
        alertManager.triggerVibrationPreview(mode)
    }

    fun testAlertSound() {
        if (alertManager.isAlerting.value) {
            alertManager.stopAlert()
        } else {
            alertManager.startGentleAlert()
        }
    }
}
