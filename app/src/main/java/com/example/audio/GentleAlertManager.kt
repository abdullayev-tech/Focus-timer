package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
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
    ZEN_BOWL("Zen Singing Bowl", "Harmonic warm acoustic bell with soft overtones"),
    DAWN_CHIME("Soft Dawn Chime", "Serene ascending harmonic chord"),
    WARM_PULSE("Ambient Breath", "Low soothing sine wave pulse"),
    SYSTEM_GENTLE("System Notification", "System gentle tone with volume fade-in")
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

    // Sound configuration
    var soundMode: GentleSoundMode = GentleSoundMode.ZEN_BOWL
    var crescendoDurationSeconds: Int = 14
    var maxVolume: Float = 0.50f // Max 50% comfortable volume
    var hapticsEnabled: Boolean = true

    fun startGentleAlert() {
        if (_isAlerting.value) return
        _isAlerting.value = true
        _currentVolume.value = 0f

        when (soundMode) {
            GentleSoundMode.ZEN_BOWL -> startSynthesizedChime(toneType = 1)
            GentleSoundMode.DAWN_CHIME -> startSynthesizedChime(toneType = 2)
            GentleSoundMode.WARM_PULSE -> startSynthesizedChime(toneType = 3)
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
                    val vol = (i / steps.toFloat()) * maxVolume
                    _currentVolume.value = vol
                    mediaPlayer?.setVolume(vol, vol)
                }
            }
        } catch (e: Exception) {
            // Fallback to synthesized chime if system tone fails
            startSynthesizedChime(toneType = 1)
        }
    }

    /**
     * Synthesizes warm, meditative harmonic chime tones using AudioTrack.
     * This avoids abrasive alerts and guarantees a soft, non-distressing acoustic experience.
     */
    private fun startSynthesizedChime(toneType: Int) {
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
            val totalRampMillis = crescendoDurationSeconds * 1000L

            while (isActive && _isAlerting.value) {
                val elapsed = System.currentTimeMillis() - startTime
                val rampFactor = (elapsed.toFloat() / totalRampMillis).coerceIn(0.01f, 1.0f)
                val currentVol = rampFactor * maxVolume
                _currentVolume.value = currentVol

                // Generate a 2.4-second calm harmonic chime buffer
                val pcmBuffer = generateHarmonicChimeSamples(sampleRate, toneType, currentVol)
                track.write(pcmBuffer, 0, pcmBuffer.size)

                // Brief tranquil space between chimes
                delay(800)
            }
        }
    }

    private fun generateHarmonicChimeSamples(
        sampleRate: Int,
        toneType: Int,
        volume: Float
    ): ShortArray {
        val durationSec = 2.4f
        val numSamples = (durationSec * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        val baseFreq = when (toneType) {
            1 -> 432.0 // Zen Tibetan singing bowl tuning
            2 -> 528.0 // Serene Dawn chime
            else -> 280.0 // Warm Ambient pulse
        }

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate

            // Gentle exponential decay envelope: fast 40ms attack, long serene tail
            val attackTime = 0.04
            val envelope = if (t < attackTime) {
                (t / attackTime)
            } else {
                exp(-(t - attackTime) * 1.6)
            }

            // Rich harmonic composition (fundamental + warm 2nd & 3rd harmonic)
            val sample = when (toneType) {
                1 -> {
                    // Singing bowl with gentle shimmering overtone
                    val fundamental = sin(2 * PI * baseFreq * t)
                    val overtone = 0.35 * sin(2 * PI * baseFreq * 2.76 * t)
                    val sub = 0.20 * sin(2 * PI * (baseFreq / 2.0) * t)
                    (fundamental + overtone + sub) * envelope
                }
                2 -> {
                    // Chime arpeggio triad: Root, Major 3rd, 5th staggered
                    val note1 = sin(2 * PI * baseFreq * t) * envelope
                    val note2 = if (t > 0.15) {
                        sin(2 * PI * baseFreq * 1.2599 * (t - 0.15)) * exp(-(t - 0.15) * 1.8)
                    } else 0.0
                    val note3 = if (t > 0.30) {
                        sin(2 * PI * baseFreq * 1.4983 * (t - 0.30)) * exp(-(t - 0.30) * 1.8)
                    } else 0.0
                    (note1 + note2 * 0.7 + note3 * 0.6) / 2.0
                }
                else -> {
                    // Warm low pulse
                    val wave = sin(2 * PI * baseFreq * t)
                    val breath = (sin(2 * PI * 0.6 * t) + 1.0) / 2.0
                    wave * envelope * breath
                }
            }

            // Scale to 16-bit PCM with volume applied
            val scaled = (sample * volume * 32767.0).coerceIn(-32767.0, 32767.0)
            buffer[i] = scaled.toInt().toShort()
        }

        return buffer
    }

    private fun startGentleHaptics() {
        hapticJob = scope.launch {
            val vibrator = getVibrator() ?: return@launch
            // Initial 1.5s silent delay so it doesn't jolt immediately
            delay(1500)

            while (isActive && _isAlerting.value) {
                // Nothing-style gentle heartbeat pulse: 60ms soft vibration
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(60)
                }
                // Relaxed interval (1.4 seconds between pulses)
                delay(1400)
            }
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
            // Ignore cleanup exceptions
        }

        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            // Ignore cleanup exceptions
        }

        try {
            getVibrator()?.cancel()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
