package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.example.audio.FADE_IN_STOPS
import com.example.audio.GentleSoundMode
import com.example.audio.GentleVibrationMode
import com.example.ui.theme.StoppedRingColor

class TimerPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("focus_timer_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SOUND_MODE = "pref_sound_mode"
        private const val KEY_CRESCENDO_DURATION = "pref_crescendo_duration"
        private const val KEY_HAPTICS_ENABLED = "pref_haptics_enabled"
        private const val KEY_VIBRATION_MODE = "pref_vibration_mode"
        private const val KEY_RINGTONE_URI = "pref_ringtone_uri"
        private const val KEY_AUTO_STOP_MINUTES = "pref_auto_stop_minutes"
        private const val KEY_STOPPED_RING_COLOR = "pref_stopped_ring_color"
        private const val KEY_ASKED_NOTIFICATIONS = "pref_asked_notifications"
        private const val KEY_TIME_IN_STATUS_BAR = "pref_time_in_status_bar"
    }

    fun getSoundMode(): GentleSoundMode {
        val savedName = prefs.getString(KEY_SOUND_MODE, GentleSoundMode.AMBIENT_BREATHE.name)
        return try {
            GentleSoundMode.valueOf(savedName ?: GentleSoundMode.AMBIENT_BREATHE.name)
        } catch (e: Exception) {
            GentleSoundMode.AMBIENT_BREATHE
        }
    }

    fun setSoundMode(mode: GentleSoundMode) {
        prefs.edit().putString(KEY_SOUND_MODE, mode.name).apply()
    }

    fun getRingtoneUri(): Uri? {
        return prefs.getString(KEY_RINGTONE_URI, null)?.let { Uri.parse(it) }
    }

    fun setRingtoneUri(uri: Uri) {
        prefs.edit().putString(KEY_RINGTONE_URI, uri.toString()).apply()
    }

    fun getAutoStopMinutes(): Int {
        return prefs.getInt(KEY_AUTO_STOP_MINUTES, 3)
    }

    fun setAutoStopMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_AUTO_STOP_MINUTES, minutes).apply()
    }

    fun getCrescendoDuration(): Int {
        // Coerced because "off" (0) used to be a choice
        return prefs.getInt(KEY_CRESCENDO_DURATION, 25).coerceAtLeast(FADE_IN_STOPS.first())
    }

    fun setCrescendoDuration(seconds: Int) {
        prefs.edit().putInt(KEY_CRESCENDO_DURATION, seconds).apply()
    }

    fun getHapticsEnabled(): Boolean {
        return prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, enabled).apply()
    }

    fun getVibrationMode(): GentleVibrationMode {
        val savedName = prefs.getString(KEY_VIBRATION_MODE, GentleVibrationMode.SOFT_PULSE.name)
        return try {
            GentleVibrationMode.valueOf(savedName ?: GentleVibrationMode.SOFT_PULSE.name)
        } catch (e: Exception) {
            GentleVibrationMode.SOFT_PULSE
        }
    }

    fun setVibrationMode(mode: GentleVibrationMode) {
        prefs.edit().putString(KEY_VIBRATION_MODE, mode.name).apply()
    }

    fun getStoppedRingColor(): StoppedRingColor {
        val savedName = prefs.getString(KEY_STOPPED_RING_COLOR, null)
        return StoppedRingColor.values().firstOrNull { it.name == savedName } ?: StoppedRingColor.STEEL
    }

    fun setStoppedRingColor(color: StoppedRingColor) {
        prefs.edit().putString(KEY_STOPPED_RING_COLOR, color.name).apply()
    }

    fun wasAskedForNotifications(): Boolean {
        return prefs.getBoolean(KEY_ASKED_NOTIFICATIONS, false)
    }

    fun setAskedForNotifications() {
        prefs.edit().putBoolean(KEY_ASKED_NOTIFICATIONS, true).apply()
    }

    fun getTimeInStatusBar(): Boolean {
        return prefs.getBoolean(KEY_TIME_IN_STATUS_BAR, true)
    }

    fun setTimeInStatusBar(show: Boolean) {
        prefs.edit().putBoolean(KEY_TIME_IN_STATUS_BAR, show).apply()
    }
}
