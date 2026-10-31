package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class GentleSoundMode(val title: String, val description: String) {
    AMBIENT_BREATHE("Ambient Breathe", "Low soothing sine wave pulse with gradual swell"),
    OCEAN_TIDE("Ocean Tide", "Ultra-deep warm swell rolling in like calm water"),
    FOREST_MIST("Forest Mist", "Serene dual-tone harmonic pulse, soft and organic"),
    STELLAR_CALM("Stellar Calm", "Gentle floating pad with warm meditative breath"),
    SYSTEM_GENTLE("System Notification", "Default system tone with gradual volume fade-in")
}

enum class GentleVibrationMode(val title: String, val description: String) {
    SUBTLE_WHISPER("Subtle Whisper", "Delicate micro-pulse, extremely soft and gentle"),
    CALM_HEARTBEAT("Resting Heartbeat", "Slow, quiet double pulse with spacious pause"),
    BREATHING_WAVE("Slow Breath Wave", "Tiny rising and relaxing vibration"),
    FEATHER_TICK("Feather Touch", "Barely perceptible gentle haptic tick")
}

class GentleAlertManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var alertJob: Job? = null
    private var hapticJob: Job? = null
    private var volumeJob: Job? = null

    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null

    private val _isAlerting = MutableStateFlow(false)
    val isAlerting: StateFlow<Boolean> = _isAlerting.asStateFlow()

    private val _currentVolume = MutableStateFlow(0f)
    val currentVolume: StateFlow<Float> = _currentVolume.asStateFlow()

    // Sound and vibration configuration
    var soundMode: GentleSoundMode = GentleSoundMode.AMBIENT_BREATHE
    var crescendoDurationSeconds: Int = 25
    var maxVolume: Float = 0.50f // Max 50% comfortable volume
    var hapticsEnabled: Boolean = true
    var vibrationMode: GentleVibrationMode = GentleVibrationMode.SUBTLE_WHISPER

    fun startGentleAlert() {
        if (_isAlerting.value) return
        _isAlerting.value = true
        _currentVolume.value = 0f

        when (soundMode) {
            GentleSoundMode.AMBIENT_BREATHE -> startSynthesizedAmbient(toneType = 1)
            GentleSoundMode.OCEAN_TIDE -> startSynthesizedAmbient(toneType = 2)
            GentleSoundMode.FOREST_MIST -> startSynthesizedAmbient(toneType = 3)
            GentleSoundMode.STELLAR_CALM -> startSynthesizedAmbient(toneType = 4)
            GentleSoundMode.SYSTEM_GENTLE -> startSystemTone()
        }

        if (hapticsEnabled) {
            startGentleHaptics()
        }
    }

    private fun startSystemTone() {
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                setVolume(0.001f, 0.001f)
                prepare()
                start()
            }

            volumeJob = scope.launch {
                val steps = crescendoDurationSeconds * 2
                for (i in 1..steps) {
                    if (!isActive) break
                    delay(500)
                    // Quadratic ease-in for gentle progressive volume rise
                    val progress = i / steps.toFloat()
                    val vol = (progress * progress * maxVolume).coerceIn(0.005f, maxVolume)
                    _currentVolume.value = vol
                    mediaPlayer?.setVolume(vol, vol)
                }
            }
        } catch (e: Exception) {
            startSynthesizedAmbient(toneType = 1)
        }
    }

    /**
     * Synthesizes warm, non-intrusive ambient meditative swells using AudioTrack.
     * Completely avoids harsh transients, clicks, or abrupt attacks.
     */
    private fun startSynthesizedAmbient(toneType: Int) {
        alertJob = scope.launch {
            val sampleRate = 44100
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            track.play()

            val startTime = System.currentTimeMillis()
            val totalRampMillis = (crescendoDurationSeconds * 1000L).coerceAtLeast(1000L)

            while (isActive && _isAlerting.value) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / totalRampMillis).coerceIn(0f, 1.0f)
                // Quadratic curve (progress^2): starts at true whisper quiet and increases very slowly
                val rampFactor = (progress * progress).coerceIn(0.003f, 1.0f)
                val currentVol = rampFactor * maxVolume
                _currentVolume.value = currentVol

                // Generate a 3.2-second tranquil swell buffer
                val pcmBuffer = generateAmbientSamples(sampleRate, toneType, currentVol)
                track.write(pcmBuffer, 0, pcmBuffer.size)

                // Brief serene breathing space between swells
                delay(600)
            }
        }
    }

    private fun generateAmbientSamples(
        sampleRate: Int,
        toneType: Int,
        volume: Float
    ): ShortArray {
        val durationSec = 3.2f
        val numSamples = (durationSec * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate

            // Very smooth, tender attack (800ms) - completely eliminates harsh clicks or strikes
            val attackTime = 0.8
            val envelope = if (t < attackTime) {
                sin((t / attackTime) * (PI / 2.0))
            } else {
                exp(-(t - attackTime) * 0.85)
            }

            val sample = when (toneType) {
                1 -> {
                    // AMBIENT_BREATHE: Warm 260 Hz sine wave with slow 0.45 Hz breath undulation
                    val baseFreq = 260.0
                    val wave = sin(2 * PI * baseFreq * t)
                    val sub = 0.25 * sin(2 * PI * (baseFreq / 2.0) * t)
                    val harmonic = 0.12 * sin(2 * PI * (baseFreq * 2.0) * t)
                    val breath = (sin(2 * PI * 0.45 * t) + 1.0) / 2.0
                    (wave + sub + harmonic) * envelope * breath
                }
                2 -> {
                    // OCEAN_TIDE: Deep warm 174 Hz Solfeggio frequency + 261 Hz rolling tide swell
                    val baseFreq = 174.0
                    val tide = (sin(2 * PI * 0.32 * t) + 1.0) / 2.0
                    val deepWave = sin(2 * PI * baseFreq * t)
                    val overtoneWave = 0.35 * sin(2 * PI * (baseFreq * 1.5) * t)
                    (deepWave + overtoneWave) * envelope * tide
                }
                3 -> {
                    // FOREST_MIST: Serene organic dual harmonic pulse (216 Hz & 324 Hz) with slow phase drift
                    val freq1 = 216.0
                    val freq2 = 324.0
                    val wave1 = sin(2 * PI * freq1 * t)
                    val wave2 = sin(2 * PI * (freq2 + 0.5) * t) * 0.55
                    val drift = (sin(2 * PI * 0.38 * t) + 1.0) / 2.0
                    (wave1 + wave2) * envelope * drift
                }
                else -> {
                    // STELLAR_CALM: Soft ambient chord pad (240 Hz, 300 Hz, 360 Hz) warm floating swell
                    val w1 = sin(2 * PI * 240.0 * t)
                    val w2 = sin(2 * PI * 300.0 * t) * 0.5
                    val w3 = sin(2 * PI * 360.0 * t) * 0.4
                    val padBreath = (sin(2 * PI * 0.35 * t) + 1.0) / 2.0
                    (w1 + w2 + w3) / 1.7 * envelope * padBreath
                }
            }

            // Scale to 16-bit PCM with master volume
            val scaled = (sample * volume * 32767.0).coerceIn(-32767.0, 32767.0)
            buffer[i] = scaled.toInt().toShort()
        }

        return buffer
    }

    private fun startGentleHaptics() {
        hapticJob = scope.launch {
            val vibrator = getVibrator() ?: return@launch
            // Initial 1.8s delay so the quiet ambient sound begins first
            delay(1800)

            while (isActive && _isAlerting.value) {
                performVibrationPattern(vibrator, vibrationMode)
            }
        }
    }

    private suspend fun performVibrationPattern(vibrator: Vibrator, mode: GentleVibrationMode) {
        val hasAmp = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasAmplitudeControl()

        when (mode) {
            GentleVibrationMode.SUBTLE_WHISPER -> {
                // Tiny soft single tap
                if (hasAmp) {
                    vibrator.vibrate(VibrationEffect.createOneShot(22, 35))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(18)
                }
                delay(1900)
            }
            GentleVibrationMode.CALM_HEARTBEAT -> {
                // Slow resting heartbeat (two delicate micro-pulses)
                if (hasAmp) {
                    vibrator.vibrate(VibrationEffect.createOneShot(20, 35))
                    delay(140)
                    vibrator.vibrate(VibrationEffect.createOneShot(22, 28))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(16, VibrationEffect.DEFAULT_AMPLITUDE))
                    delay(140)
                    vibrator.vibrate(VibrationEffect.createOneShot(16, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(16)
                    delay(140)
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(16)
                }
                delay(2200)
            }
            GentleVibrationMode.BREATHING_WAVE -> {
                // Gradual gentle wave rise & release
                if (hasAmp) {
                    val timings = longArrayOf(0, 25, 20, 25, 20, 30, 20, 25, 20, 25)
                    val amplitudes = intArrayOf(0, 15, 0, 30, 0, 48, 0, 30, 0, 15)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(24, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(24)
                }
                delay(2400)
            }
            GentleVibrationMode.FEATHER_TICK -> {
                // Barely perceptible delicate haptic tick
                if (hasAmp) {
                    vibrator.vibrate(VibrationEffect.createOneShot(12, 25))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(12)
                }
                delay(2000)
            }
        }
    }

    fun triggerVibrationPreview(mode: GentleVibrationMode) {
        scope.launch {
            val vibrator = getVibrator() ?: return@launch
            performVibrationPattern(vibrator, mode)
        }
    }

    private fun getVibrator(): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            null
        }
    }

    fun stopAlert() {
        _isAlerting.value = false
        _currentVolume.value = 0f

        alertJob?.cancel()
        alertJob = null

        volumeJob?.cancel()
        volumeJob = null

        hapticJob?.cancel()
        hapticJob = null

        try {
            audioTrack?.apply {
                stop()
                release()
            }
            audioTrack = null
        } catch (e: Exception) {
            // Ignore
        }

        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            // Ignore
        }

        try {
            getVibrator()?.cancel()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
