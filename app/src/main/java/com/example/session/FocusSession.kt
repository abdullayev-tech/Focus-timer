package com.example.session

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.example.audio.GentleAlertManager
import com.example.audio.VIBRATION_OFF_NAME
import com.example.audio.soundModeNamed
import com.example.audio.vibrationModeNamed
import com.example.data.EventLogDatabase
import com.example.data.TimerEntity
import com.example.data.TimerEvent
import com.example.data.TimerEventType
import com.example.data.TimerPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ActiveTimerState(
    val timerEntity: TimerEntity? = null,
    val totalSeconds: Long = 0L,
    val remainingMillis: Long = 0L,
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    // When time is up, by the wall clock; null unless the clock is running
    val endAtMillis: Long? = null
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

/** What the notification should say. */
sealed interface SessionNotice {
    data object None : SessionNotice
    data class Running(val label: String, val endAtMillis: Long) : SessionNotice
    data class Done(val label: String) : SessionNotice
}

internal fun noticeFor(state: ActiveTimerState, isAlerting: Boolean): SessionNotice {
    val label = state.timerEntity?.label ?: return SessionNotice.None
    val endAt = state.endAtMillis
    return when {
        state.isCompleted && isAlerting -> SessionNotice.Done(label)
        state.isRunning && !state.isCompleted && endAt != null -> SessionNotice.Running(label, endAt)
        else -> SessionNotice.None
    }
}

// Opening the app this long after time was up shows that it's over, without sounding the alert.
// The alarm always sounds it: that is the first anyone hears of it
private const val LATE_ALERT_LIMIT_MILLIS = 60_000L

internal fun isTooLateToAlert(remainingMillis: Long): Boolean {
    return -remainingMillis >= LATE_ALERT_LIMIT_MILLIS
}

private const val TICK_MILLIS = 50L // For a fluid ring

/**
 * The one session. It lives as long as the process, not as long as a screen, so leaving the app
 * doesn't end it. The alarm ends it even if the process died on the way; [FocusSessionService]
 * keeps the process alive and says so in the notification.
 *
 * Everything here runs on the main thread.
 */
class FocusSession internal constructor(
    context: Context,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val app = context.applicationContext
    private val store = SessionStore(app)
    private val eventDao = EventLogDatabase.getDatabase(app).timerEventDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val alarms = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val alertWakeLock =
        (app.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FocusTimer:alert")
            .apply { setReferenceCounted(false) }

    val alertManager = GentleAlertManager(app)

    private val _activeState = MutableStateFlow(ActiveTimerState())
    val activeState: StateFlow<ActiveTimerState> = _activeState.asStateFlow()

    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen: StateFlow<Boolean> = _isFullScreen.asStateFlow()

    val notices: Flow<SessionNotice> =
        combine(_activeState, alertManager.isAlerting) { state, isAlerting ->
            noticeFor(state, isAlerting)
        }.distinctUntilChanged()

    private var countdownJob: Job? = null
    private var isUiVisible = false

    // What the clock said when it last began running; null while it isn't
    private var stretchStartMillis: Long? = null

    init {
        // Set here, not on a screen: the alarm can bring the process back with no screen at all
        val prefs = TimerPreferences(app)
        alertManager.soundMode = prefs.getSoundMode()
        alertManager.crescendoDurationSeconds = prefs.getCrescendoDuration()
        alertManager.hapticsEnabled = prefs.getHapticsEnabled()
        alertManager.vibrationMode = prefs.getVibrationMode()
        alertManager.ringtoneUri = prefs.getRingtoneUri()
        alertManager.autoStopMinutes = prefs.getAutoStopMinutes()

        scope.launch {
            alertManager.isAlerting.collect { isAlerting ->
                if (!isAlerting && alertWakeLock.isHeld) alertWakeLock.release()
            }
        }

        restore()
    }

    /** Picks up a session from before the process died: the clock is true to the second. */
    private fun restore() {
        val saved = store.load() ?: return
        stretchStartMillis = saved.stretchStartMillis
        val state = saved.state
        val endAt = state.endAtMillis
        _activeState.value =
            if (endAt != null) state.copy(remainingMillis = endAt - now()) else state
        _isFullScreen.value = true
        if (endAt != null && !state.isCompleted && endAt > now()) scheduleEnd(endAt)
    }

    fun currentNotice(): SessionNotice {
        return noticeFor(_activeState.value, alertManager.isAlerting.value)
    }

    /** Whether something on the clock would be lost by starting something else. */
    fun isClockRunning(): Boolean {
        val state = _activeState.value
        return state.isRunning && !state.isCompleted
    }

    /** The ticking is only for looking at. In the background the alarm ends the session. */
    fun setUiVisible(visible: Boolean) {
        isUiVisible = visible
        if (visible) {
            tick()
            if (isClockRunning()) startService()
        }
        syncTicker()
    }

    /** The app was opened anew: a session in progress is the first thing to see. */
    fun showSessionInProgress() {
        val state = _activeState.value
        if (state.isRunning || state.isCompleted) _isFullScreen.value = true
    }

    /** A test or preview ends with the app. A session's alert doesn't. */
    fun onUiDestroyed() {
        if (!_activeState.value.isCompleted) alertManager.stopAlert()
    }

    fun refreshNotice() {
        if (currentNotice() != SessionNotice.None) startService()
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
        cancelEnd()
        store.clear()
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
        // Reset to full if starting from completed state
        val remaining = if (current.remainingMillis <= 0L || current.isCompleted) {
            current.totalSeconds * 1000L
        } else {
            current.remainingMillis
        }

        alertManager.stopAlert()

        val endAt = now() + remaining
        _activeState.value = current.copy(
            remainingMillis = remaining,
            isRunning = true,
            isCompleted = false,
            endAtMillis = endAt
        )
        stretchStartMillis = remaining
        logSession(if (isFromTheTop) TimerEventType.STARTED else TimerEventType.RESUMED)
        save()
        scheduleEnd(endAt)
        startService()
        syncTicker()
    }

    fun pauseTimer() {
        val current = _activeState.value
        val remaining = current.endAtMillis?.let { it - now() } ?: current.remainingMillis
        _activeState.value = current.copy(
            remainingMillis = remaining,
            isRunning = false,
            endAtMillis = null
        )
        logSession(TimerEventType.PAUSED, takeRanMillis())
        cancelEnd()
        save()
        syncTicker()
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
        cancelEnd()
        store.clear()
        if (alertManager.isAlerting.value) {
            alertManager.stopAlert()
        }
        val current = _activeState.value
        _activeState.value = current.copy(
            remainingMillis = current.totalSeconds * 1000L,
            isRunning = false,
            isCompleted = false,
            endAtMillis = null
        )
    }

    /** The person silencing a session's alert, as opposed to leaving a settings test. */
    fun silenceSession() {
        if (alertManager.isAlerting.value) {
            syncRemaining()
            logSession(TimerEventType.SILENCED)
        }
        alertManager.stopAlert()
    }

    /** The alarm went off. */
    fun onTimeUp() {
        val state = _activeState.value
        val endAt = state.endAtMillis ?: return
        if (!state.isRunning || state.isCompleted) return
        if (endAt > now()) {
            // The phone's clock was moved back since the session started
            scheduleEnd(endAt)
            return
        }
        finish(endAt, playAlert = true)
    }

    private fun tick() {
        val state = _activeState.value
        val endAt = state.endAtMillis ?: return
        val remaining = endAt - now()
        if (remaining <= 0L && !state.isCompleted) {
            finish(endAt, playAlert = !isTooLateToAlert(remaining))
        } else {
            _activeState.value = state.copy(remainingMillis = remaining)
        }
    }

    private fun syncRemaining() {
        val state = _activeState.value
        val endAt = state.endAtMillis ?: return
        _activeState.value = state.copy(remainingMillis = endAt - now())
    }

    private fun finish(endAt: Long, playAlert: Boolean) {
        cancelEnd()
        syncRemaining()
        logSession(TimerEventType.FINISHED, takeRanMillis())

        // The alert first, so the notification goes straight from running to done
        val state = _activeState.value
        if (playAlert) startAlertFor(state.timerEntity)
        _activeState.value = state.copy(remainingMillis = endAt - now(), isCompleted = true)
        // The focus screen is where the alert can be silenced
        _isFullScreen.value = true
        save()
        if (playAlert) startService()
        syncTicker()
    }

    private fun syncTicker() {
        countdownJob?.cancel()
        if (!isUiVisible || !_activeState.value.isRunning) return
        countdownJob = scope.launch {
            while (isActive) {
                tick()
                delay(TICK_MILLIS)
            }
        }
    }

    /** The timer's own choices where it has them, otherwise the defaults from settings. */
    fun startAlertFor(timer: TimerEntity?) {
        val stopAfterMinutes = timer?.autoStopMinutes ?: alertManager.autoStopMinutes
        // Sound, vibration and the fade-in all run on timers the phone would otherwise sleep through
        alertWakeLock.acquire(
            if (stopAfterMinutes > 0) (stopAfterMinutes + 1) * 60_000L else 60 * 60_000L
        )
        alertManager.startGentleAlert(
            mode = soundModeNamed(timer?.soundName) ?: alertManager.soundMode,
            fadeInSeconds = timer?.fadeInSeconds ?: alertManager.crescendoDurationSeconds,
            stopAfterMinutes = stopAfterMinutes,
            vibration = when (timer?.vibrationName) {
                VIBRATION_OFF_NAME -> null
                else -> vibrationModeNamed(timer?.vibrationName)
                    ?: alertManager.vibrationMode.takeIf { alertManager.hapticsEnabled }
            }
        )
    }

    /** A session already running with this timer uses its new choices too. */
    fun onTimerEdited(timer: TimerEntity) {
        if (_activeState.value.timerEntity?.id == timer.id) {
            _activeState.value = _activeState.value.copy(timerEntity = timer)
            save()
        }
    }

    fun onTimerDeleted(timer: TimerEntity) {
        if (_activeState.value.timerEntity?.id == timer.id) {
            if (_activeState.value.isRunning) {
                logSession(TimerEventType.CLOSED, takeRanMillis())
            }
            clearClock()
            _isFullScreen.value = false
        }
    }

    private fun save() {
        store.save(_activeState.value, stretchStartMillis)
    }

    private fun endPendingIntent(): PendingIntent {
        return PendingIntent.getBroadcast(
            app,
            0,
            Intent(app, SessionReceiver::class.java).setAction(SessionReceiver.ACTION_TIME_UP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun scheduleEnd(endAt: Long) {
        try {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, endPendingIntent())
        } catch (e: SecurityException) {
            // Exact alarms were taken away from this app: a little late beats not at all
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, endPendingIntent())
        }
    }

    private fun cancelEnd() {
        alarms.cancel(endPendingIntent())
    }

    private fun startService() {
        try {
            ContextCompat.startForegroundService(app, Intent(app, FocusSessionService::class.java))
        } catch (e: IllegalStateException) {
            // Android refused it from the background. The alarm and the alert still work
        }
    }

    private fun logSession(type: TimerEventType, ranMillis: Long? = null) {
        val state = _activeState.value
        log(type, state.timerEntity, state.remainingMillis, ranMillis)
    }

    fun log(
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
        scope.launch {
            eventDao.insert(event)
        }
    }

    // How much of the timer this stretch of running used. Only the first caller gets it
    private fun takeRanMillis(): Long? {
        val start = stretchStartMillis ?: return null
        stretchStartMillis = null
        return start - _activeState.value.remainingMillis.coerceAtLeast(0L)
    }

    companion object {
        @Volatile
        private var instance: FocusSession? = null

        fun get(context: Context): FocusSession {
            return instance ?: synchronized(this) {
                instance ?: FocusSession(context).also { instance = it }
            }
        }
    }
}
