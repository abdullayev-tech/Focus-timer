package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.GentleAlertManager
import com.example.audio.GentleSoundMode
import com.example.data.TimerDatabase
import com.example.data.TimerEntity
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
    val remainingSeconds: Long
        get() = (remainingMillis + 999L) / 1000L

    val progress: Float
        get() = if (totalSeconds > 0) {
            (remainingMillis.toFloat() / (totalSeconds * 1000f)).coerceIn(0f, 1f)
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
    val alertManager = GentleAlertManager(application)

    val timers: StateFlow<List<TimerEntity>>

    private val _activeState = MutableStateFlow(ActiveTimerState())
    val activeState: StateFlow<ActiveTimerState> = _activeState.asStateFlow()

    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen: StateFlow<Boolean> = _isFullScreen.asStateFlow()

    private val _selectedSoundMode = MutableStateFlow(GentleSoundMode.ZEN_BOWL)
    val selectedSoundMode: StateFlow<GentleSoundMode> = _selectedSoundMode.asStateFlow()

    private val _crescendoDuration = MutableStateFlow(14)
    val crescendoDuration: StateFlow<Int> = _crescendoDuration.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(true)
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private var countdownJob: Job? = null
    private var lastTickTime: Long = 0L

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
        if (current.remainingMillis <= 0L) {
            // Reset to full if starting from 0
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
            dismissAlert()
        }
        val current = _activeState.value
        _activeState.value = current.copy(
            remainingMillis = current.totalSeconds * 1000L,
            isRunning = false,
            isCompleted = false
        )
    }

    fun addExtraSeconds(seconds: Long) {
        val current = _activeState.value
        val newTotal = current.totalSeconds + seconds
        val newRemaining = current.remainingMillis + (seconds * 1000L)
        _activeState.value = current.copy(
            totalSeconds = newTotal,
            remainingMillis = newRemaining,
            isCompleted = false
        )
        if (alertManager.isAlerting.value) {
            dismissAlert()
        }
        if (!current.isRunning) {
            startTimer()
        }
    }

    fun dismissAlert() {
        alertManager.stopAlert()
        _activeState.value = _activeState.value.copy(isCompleted = false)
    }

    fun createTimer(label: String, totalSeconds: Long, autoOpen: Boolean = true) {
        viewModelScope.launch {
            val newTimer = TimerEntity(
                label = label.ifBlank { "Focus Block" },
                totalSeconds = totalSeconds
            )
            val id = repository.insert(newTimer)
            if (autoOpen) {
                val savedTimer = newTimer.copy(id = id)
                openTimer(savedTimer, autoStart = false)
            }
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
    }

    fun updateCrescendoDuration(seconds: Int) {
        _crescendoDuration.value = seconds
        alertManager.crescendoDurationSeconds = seconds
    }

    fun updateHapticsEnabled(enabled: Boolean) {
        _hapticsEnabled.value = enabled
        alertManager.hapticsEnabled = enabled
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
