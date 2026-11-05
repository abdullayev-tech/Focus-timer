package com.example.session

import android.content.Context
import com.example.data.TimerEntity

/** A session on disk, so it outlives the process. A one-time timer exists nowhere else. */
internal class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("focus_session", Context.MODE_PRIVATE)

    class Saved(val state: ActiveTimerState, val stretchStartMillis: Long?)

    fun save(state: ActiveTimerState, stretchStartMillis: Long?) {
        val timer = state.timerEntity ?: return
        prefs.edit()
            .putLong(KEY_ID, timer.id)
            .putString(KEY_LABEL, timer.label)
            .putLong(KEY_TOTAL_SECONDS, timer.totalSeconds)
            .putString(KEY_SOUND, timer.soundName)
            .putInt(KEY_FADE_IN, timer.fadeInSeconds ?: NONE.toInt())
            .putInt(KEY_AUTO_STOP, timer.autoStopMinutes ?: NONE.toInt())
            .putString(KEY_VIBRATION, timer.vibrationName)
            .putLong(KEY_REMAINING, state.remainingMillis)
            .putBoolean(KEY_RUNNING, state.isRunning)
            .putBoolean(KEY_COMPLETED, state.isCompleted)
            .putLong(KEY_END_AT, state.endAtMillis ?: NONE)
            .putLong(KEY_STRETCH_START, stretchStartMillis ?: NONE)
            .apply()
    }

    fun load(): Saved? {
        val label = prefs.getString(KEY_LABEL, null) ?: return null
        val totalSeconds = prefs.getLong(KEY_TOTAL_SECONDS, 0L)
        val timer = TimerEntity(
            id = prefs.getLong(KEY_ID, 0L),
            label = label,
            totalSeconds = totalSeconds,
            soundName = prefs.getString(KEY_SOUND, null),
            fadeInSeconds = prefs.getInt(KEY_FADE_IN, NONE.toInt()).takeIf { it != NONE.toInt() },
            autoStopMinutes = prefs.getInt(KEY_AUTO_STOP, NONE.toInt()).takeIf { it != NONE.toInt() },
            vibrationName = prefs.getString(KEY_VIBRATION, null)
        )
        return Saved(
            state = ActiveTimerState(
                timerEntity = timer,
                totalSeconds = totalSeconds,
                remainingMillis = prefs.getLong(KEY_REMAINING, 0L),
                isRunning = prefs.getBoolean(KEY_RUNNING, false),
                isCompleted = prefs.getBoolean(KEY_COMPLETED, false),
                endAtMillis = prefs.getLong(KEY_END_AT, NONE).takeIf { it != NONE }
            ),
            stretchStartMillis = prefs.getLong(KEY_STRETCH_START, NONE).takeIf { it != NONE }
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val NONE = -1L
        const val KEY_ID = "id"
        const val KEY_LABEL = "label"
        const val KEY_TOTAL_SECONDS = "total_seconds"
        const val KEY_SOUND = "sound"
        const val KEY_FADE_IN = "fade_in"
        const val KEY_AUTO_STOP = "auto_stop"
        const val KEY_VIBRATION = "vibration"
        const val KEY_REMAINING = "remaining"
        const val KEY_RUNNING = "running"
        const val KEY_COMPLETED = "completed"
        const val KEY_END_AT = "end_at"
        const val KEY_STRETCH_START = "stretch_start"
    }
}
