package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.GentleAlertManager
import com.example.audio.GentleSoundMode
import com.example.audio.GentleVibrationMode
import com.example.data.TimerDatabase
import com.example.data.TimerEntity
import com.example.data.TimerPreferences
import com.example.data.TimerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    // When completed or at zero, show the configured actual timer duration instead of 00:00
    val remainingSeconds: Long
        get() = if (isCompleted || (remainingMillis <= 0L && totalSeconds > 0L)) {
            totalSeconds
        } else {
            (remainingMillis + 999L) / 1000L
        }

    val progress: Float
        get() = if (totalSeconds > 0) {
            if (isCompleted) {
                1f
            } else {
                (remainingMillis.toFloat() / (totalSeconds * 1000f)).coerceIn(0f, 1f)
            }
        } else 0f

    fun formattedRemaining(): String {
        val s = remainingSeconds
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

    private var countdownJob: Job? = null
    private var lastTickTime: Long = 0L

    init {
        // Sync alert manager with saved preferences
        alertManager.soundMode = _selectedSoundMode.value
        alertManager.crescendoDurationSeconds = _crescendoDuration.value
        alertManager.hapticsEnabled = _hapticsEnabled.value
        alertManager.vibrationMode = _vibrationMode.value

        val dao = TimerDatabase.getDatabase(application).timerDao()
        repository = TimerRepository(dao)
        timers = repository.allTimers.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    }

    fun openTimer(timer: TimerEntity, autoStart: Boolean = false) {
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
        }
    }

    fun closeFullScreen() {
        if (alertManager.isAlerting.value) {
            dismissAlert()
        }
        _isFullScreen.value = false
    }

    fun startTimer() {
        val current = _activeState.value
        if (current.remainingMillis <= 0L || current.isCompleted) {
            // Reset to full if starting from completed state
            _activeState.value = current.copy(
                remainingMillis = current.totalSeconds * 1000L,
                isCompleted = false
            )
        }

        if (alertManager.isAlerting.value) {
            dismissAlert()
        }

        _activeState.value = _activeState.value.copy(isRunning = true, isCompleted = false)
        lastTickTime = System.currentTimeMillis()

        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive && _activeState.value.isRunning) {
                delay(50L) // 50ms tick for fluid circular animation
                val now = System.currentTimeMillis()
                val delta = now - lastTickTime
                lastTickTime = now

                val updatedMillis = _activeState.value.remainingMillis - delta
                if (updatedMillis <= 0L) {
                    _activeState.value = _activeState.value.copy(
                        remainingMillis = 0L,
                        isRunning = false,
                        isCompleted = true
                    )
                    alertManager.startGentleAlert()
                    break
                } else {
                    _activeState.value = _activeState.value.copy(
                        remainingMillis = updatedMillis
                    )
                }
            }
        }
    }

    fun pauseTimer() {
        countdownJob?.cancel()
        _activeState.value = _activeState.value.copy(isRunning = false)
    }

    fun togglePlayPause() {
        if (_activeState.value.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    fun resetTimer() {
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

    /**
     * Dismisses the alert and directly resets the timer to the full duration,
     * showing the actual time and allowing the very next tap to immediately start.
     */
    fun dismissAlert() {
        alertManager.stopAlert()
        val current = _activeState.value
        _activeState.value = current.copy(
            remainingMillis = current.totalSeconds * 1000L,
            isRunning = false,
            isCompleted = false
        )
    }

    fun createTimer(label: String, totalSeconds: Long, autoOpen: Boolean = true) {
        viewModelScope.launch {
            val maxOrder = timers.value.maxOfOrNull { it.displayOrder } ?: -1
            val newTimer = TimerEntity(
                label = label.ifBlank { "Focus Block" },
                totalSeconds = totalSeconds,
                displayOrder = maxOrder + 1
            )
            val id = repository.insert(newTimer)
            if (autoOpen) {
                val savedTimer = newTimer.copy(id = id)
                openTimer(savedTimer, autoStart = false)
            }
        }
    }

    fun reorderTimers(fromIndex: Int, toIndex: Int) {
        val current = timers.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        val reordered = current.mapIndexed { index, timer ->
            timer.copy(displayOrder = index)
        }
        viewModelScope.launch {
            repository.updateAll(reordered)
        }
    }

    fun deleteTimer(timer: TimerEntity) {
        viewModelScope.launch {
            if (_activeState.value.timerEntity?.id == timer.id) {
                closeFullScreen()
            }
            repository.delete(timer)
        }
    }

    fun updateSoundMode(mode: GentleSoundMode) {
        _selectedSoundMode.value = mode
        alertManager.soundMode = mode
        prefs.setSoundMode(mode)
    }

    fun updateCrescendoDuration(seconds: Int) {
        _crescendoDuration.value = seconds
        alertManager.crescendoDurationSeconds = seconds
        prefs.setCrescendoDuration(seconds)
    }

    fun updateHapticsEnabled(enabled: Boolean) {
        _hapticsEnabled.value = enabled
        alertManager.hapticsEnabled = enabled
        prefs.setHapticsEnabled(enabled)
    }

    fun updateVibrationMode(mode: GentleVibrationMode) {
        _vibrationMode.value = mode
        alertManager.vibrationMode = mode
        prefs.setVibrationMode(mode)
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
