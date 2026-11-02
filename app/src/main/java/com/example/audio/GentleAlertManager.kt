package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
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
import kotlin.math.min
import kotlin.math.sin

enum class GentleSoundMode(val title: String) {
    AMBIENT_BREATHE("Ambient Breathe"),
    CLOCK_TIMER("Clock Timer"),
    BELL("Bell"),
    MARIMBA("Marimba"),
    SYSTEM_GENTLE("From Phone")
}

/** The sound with this saved name, or null for no name or a sound that no longer exists. */
fun soundModeNamed(name: String?): GentleSoundMode? {
    return GentleSoundMode.values().firstOrNull { it.name == name }
}

enum class GentleVibrationMode(val title: String) {
    SOFT_PULSE("Soft Pulse"),
    HEARTBEAT("Heartbeat"),
    WAVE("Wave"),
    KNOCK("Knock")
}

/** The vibration with this saved name, or null for no name or one that no longer exists. */
fun vibrationModeNamed(name: String?): GentleVibrationMode? {
    return GentleVibrationMode.values().firstOrNull { it.name == name }
}

// Saved as a timer's own vibration when that timer doesn't vibrate at all
const val VIBRATION_OFF_NAME = "OFF"

// The fade-in slider's stops, in seconds
val FADE_IN_STOPS = listOf(3, 5, 7, 10, 15, 20, 25, 30, 40, 50, 60)

/**
 * One repeat of a vibration.
 * - [timings] and [strengths] go together, step by step; a strength of 0 is a rest.
 * - [plainTimings] is off, on, off, on... for phones whose motor has no strength control.
 */
private class VibrationPattern(
    val timings: LongArray,
    val strengths: FloatArray,
    val plainTimings: LongArray,
    val pauseMillis: Long
)

private fun GentleVibrationMode.pattern(): VibrationPattern = when (this) {
    GentleVibrationMode.SOFT_PULSE -> VibrationPattern(
        timings = longArrayOf(120, 240, 120),
        strengths = floatArrayOf(0.45f, 1f, 0.45f),
        plainTimings = longArrayOf(0, 300),
        pauseMillis = 1500
    )
    GentleVibrationMode.HEARTBEAT -> VibrationPattern(
        timings = longArrayOf(90, 130, 120),
        strengths = floatArrayOf(1f, 0f, 0.7f),
        plainTimings = longArrayOf(0, 90, 130, 120),
        pauseMillis = 1400
    )
    GentleVibrationMode.WAVE -> VibrationPattern(
        timings = LongArray(11) { 100L },
        strengths = floatArrayOf(0.15f, 0.3f, 0.5f, 0.7f, 0.9f, 1f, 0.9f, 0.7f, 0.5f, 0.3f, 0.15f),
        plainTimings = longArrayOf(0, 120, 100, 200, 100, 320, 100, 200, 100, 120),
        pauseMillis = 1000
    )
    // The others are soft swells; this one is three firm taps, easy to feel through a pocket
    GentleVibrationMode.KNOCK -> VibrationPattern(
        timings = longArrayOf(90, 120, 90, 120, 90),
        strengths = floatArrayOf(1f, 0f, 1f, 0f, 1f),
        plainTimings = longArrayOf(0, 90, 120, 90, 120, 90),
        pauseMillis = 1100
    )
}

// Where the vibration starts, as a share of full strength, before it rises with the sound
private const val VIBRATION_START_STRENGTH = 0.35f

// Tapping a sound plays one swell of it, below full level so a loud alarm volume doesn't startle
private const val SOUND_PREVIEW_MILLIS = 3400L
private const val SOUND_PREVIEW_LEVEL = 0.6f

// One group of the Clock Timer sound: quick bright pulses, then a rest
private const val CLOCK_TIMER_PULSE_SECONDS = 0.14
private const val CLOCK_TIMER_PULSE_COUNT = 8
private const val CLOCK_TIMER_GROUP_SECONDS = 1.4f

// One round of the Marimba sound
private val MARIMBA_NOTES = listOf(523.25, 659.25, 783.99)
private const val MARIMBA_NOTE_SECONDS = 0.18
private const val MARIMBA_ROUND_SECONDS = 1.6f

class GentleAlertManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var alertJob: Job? = null
    private var hapticJob: Job? = null
    private var volumeJob: Job? = null
    private var autoStopJob: Job? = null

    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null

    private val _isAlerting = MutableStateFlow(false)
    val isAlerting: StateFlow<Boolean> = _isAlerting.asStateFlow()

    private val _currentVolume = MutableStateFlow(0f)

    // How far the fade-in has come, from 0 to 1
    val currentVolume: StateFlow<Float> = _currentVolume.asStateFlow()

    // Sound and vibration configuration
    var soundMode: GentleSoundMode = GentleSoundMode.AMBIENT_BREATHE
    var crescendoDurationSeconds: Int = 25
    var ringtoneUri: Uri? = null // The sound picked from the phone; null is its alarm sound
    var autoStopMinutes: Int = 3 // The alert stops by itself after this long; 0 means never
    var maxVolume: Float = 0.65f // Share of the phone's alarm volume at the end of the fade-in
    var hapticsEnabled: Boolean = true
    var vibrationMode: GentleVibrationMode = GentleVibrationMode.SOFT_PULSE

    /** The defaults are the choices from settings; a timer with its own passes those instead. */
    fun startGentleAlert(
        mode: GentleSoundMode = soundMode,
        fadeInSeconds: Int = crescendoDurationSeconds,
        stopAfterMinutes: Int = autoStopMinutes,
        vibration: GentleVibrationMode? = if (hapticsEnabled) vibrationMode else null
    ) {
        if (_isAlerting.value) return
        stopAlert() // A sound preview may still be playing
        _isAlerting.value = true
        _currentVolume.value = 0f

        startSound(mode, isPreview = false, fadeInSeconds = fadeInSeconds)

        if (vibration != null) {
            startGentleHaptics(vibration, fadeInSeconds)
        }

        if (stopAfterMinutes > 0) {
            autoStopJob = scope.launch {
                delay(stopAfterMinutes * 60_000L)
                stopAlert()
            }
        }
    }

    fun previewSound(mode: GentleSoundMode = soundMode) {
        stopAlert()
        startSound(mode, isPreview = true, fadeInSeconds = crescendoDurationSeconds)
        autoStopJob = scope.launch {
            delay(SOUND_PREVIEW_MILLIS)
            stopAlert()
        }
    }

    private fun startSound(mode: GentleSoundMode, isPreview: Boolean, fadeInSeconds: Int) {
        when (mode) {
            GentleSoundMode.AMBIENT_BREATHE -> startSynthesizedAmbient(toneType = 1, isPreview, fadeInSeconds)
            GentleSoundMode.CLOCK_TIMER -> startSynthesizedAmbient(toneType = 2, isPreview, fadeInSeconds)
            GentleSoundMode.BELL -> startSynthesizedAmbient(toneType = 3, isPreview, fadeInSeconds)
            GentleSoundMode.MARIMBA -> startSynthesizedAmbient(toneType = 4, isPreview, fadeInSeconds)
            GentleSoundMode.SYSTEM_GENTLE -> startSystemTone(isPreview, fadeInSeconds)
        }
    }

    private fun startSystemTone(isPreview: Boolean, fadeInSeconds: Int) {
        try {
            val alertUri = ringtoneUri
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                val startVolume = if (isPreview) SOUND_PREVIEW_LEVEL * maxVolume else 0.001f
                setVolume(startVolume, startVolume)
                prepare()
                start()
            }

            if (isPreview) return

            volumeJob = scope.launch {
                val steps = fadeInSeconds * 2
                for (i in 1..steps) {
                    if (!isActive) break
                    delay(500)
                    // Quadratic ease-in for gentle progressive volume rise
                    val progress = i / steps.toFloat()
                    val vol = (progress * progress * maxVolume).coerceIn(0.005f, maxVolume)
                    _currentVolume.value = progress * progress
                    mediaPlayer?.setVolume(vol, vol)
                }
            }
        } catch (e: Exception) {
            startSynthesizedAmbient(toneType = 1, isPreview, fadeInSeconds)
        }
    }

    /**
     * Synthesizes warm, non-intrusive ambient meditative swells using AudioTrack.
     * Completely avoids harsh transients, clicks, or abrupt attacks.
     */
    private fun startSynthesizedAmbient(toneType: Int, isPreview: Boolean, fadeInSeconds: Int) {
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
                        // The alarm volume: usually high, and on even when media is muted
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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
            val totalRampMillis = fadeInSeconds * 1000L

            // Quadratic curve (progress^2): starts at true whisper quiet and increases very slowly
            fun rampAt(elapsedMillis: Long): Float {
                if (isPreview) return SOUND_PREVIEW_LEVEL
                val progress = (elapsedMillis.toFloat() / totalRampMillis).coerceIn(0f, 1.0f)
                return (progress * progress).coerceIn(0.003f, 1.0f)
            }

            while (isActive && (isPreview || _isAlerting.value)) {
                val elapsed = System.currentTimeMillis() - startTime
                val startRamp = rampAt(elapsed)
                val endRamp = rampAt(elapsed + (swellSeconds(toneType) * 1000f).toLong())
                if (!isPreview) _currentVolume.value = startRamp

                // Generate one tranquil swell. It rises from start to end as it plays, because a
                // short fade-in is over before one swell is
                val pcmBuffer = generateAmbientSamples(
                    sampleRate,
                    toneType,
                    startVolume = startRamp * maxVolume,
                    endVolume = endRamp * maxVolume
                )
                track.write(pcmBuffer, 0, pcmBuffer.size)

                // Brief serene breathing space between swells
                delay(600)
            }
        }
    }

    private fun swellSeconds(toneType: Int): Float = when (toneType) {
        2 -> CLOCK_TIMER_GROUP_SECONDS
        4 -> MARIMBA_ROUND_SECONDS
        else -> 3.2f
    }

    private fun generateAmbientSamples(
        sampleRate: Int,
        toneType: Int,
        startVolume: Float,
        endVolume: Float
    ): ShortArray {
        val numSamples = (swellSeconds(toneType) * sampleRate).toInt()
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
                    // CLOCK_TIMER: quick bright pulses on two alternating notes, like a phone's clock timer
                    val pulse = (t / CLOCK_TIMER_PULSE_SECONDS).toInt()
                    if (pulse >= CLOCK_TIMER_PULSE_COUNT) {
                        0.0
                    } else {
                        val pulseTime = t - pulse * CLOCK_TIMER_PULSE_SECONDS
                        val freq = if (pulse % 2 == 0) 1568.0 else 1976.0
                        // 3ms rise and a fast decay, so each pulse is crisp but never clicks
                        val pulseEnvelope = min(1.0, pulseTime / 0.003) * exp(-pulseTime * 28.0)
                        val wave = sin(2 * PI * freq * pulseTime)
                        val overtone = 0.3 * sin(2 * PI * (freq * 2.0) * pulseTime)
                        (wave + overtone) / 1.3 * pulseEnvelope
                    }
                }
                3 -> {
                    // BELL: one struck note whose higher partials die away first, like a real bell
                    val freq = 523.25
                    val strike = min(1.0, t / 0.002)
                    val partials = exp(-t * 1.1) * sin(2 * PI * freq * t) +
                        0.5 * exp(-t * 1.8) * sin(2 * PI * (freq * 2.0) * t) +
                        0.25 * exp(-t * 2.8) * sin(2 * PI * (freq * 2.76) * t) +
                        0.1 * exp(-t * 5.0) * sin(2 * PI * (freq * 5.4) * t)
                    partials / 1.85 * strike
                }
                else -> {
                    // MARIMBA: three rising wooden notes that ring into each other
                    var notes = 0.0
                    MARIMBA_NOTES.forEachIndexed { index, freq ->
                        val noteTime = t - index * MARIMBA_NOTE_SECONDS
                        if (noteTime >= 0.0) {
                            val strike = min(1.0, noteTime / 0.002)
                            val wave = sin(2 * PI * freq * noteTime)
                            val overtone = 0.25 * sin(2 * PI * (freq * 4.0) * noteTime)
                            notes += (wave + overtone) * strike * exp(-noteTime * 9.0)
                        }
                    }
                    notes / 1.5
                }
            }

            // Scale to 16-bit PCM with master volume
            val volume = startVolume + (endVolume - startVolume) * (i.toFloat() / numSamples)
            val scaled = (sample * volume * 32767.0).coerceIn(-32767.0, 32767.0)
            buffer[i] = scaled.toInt().toShort()
        }

        return buffer
    }

    private fun startGentleHaptics(vibration: GentleVibrationMode, fadeInSeconds: Int) {
        hapticJob = scope.launch {
            val vibrator = getVibrator() ?: return@launch
            // Initial 1.8s delay so the quiet ambient sound begins first
            delay(1800)

            val startTime = System.currentTimeMillis()
            val totalRampMillis = fadeInSeconds * 1000L

            while (isActive && _isAlerting.value) {
                // Rises over the same time as the sound, so the two swell together
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / totalRampMillis).coerceIn(0f, 1f)
                val strength = VIBRATION_START_STRENGTH + (1f - VIBRATION_START_STRENGTH) * progress

                val pattern = vibration.pattern()
                playVibration(vibrator, pattern, strength)
                delay(pattern.timings.sum() + pattern.pauseMillis)
            }
        }
    }

    private fun playVibration(vibrator: Vibrator, pattern: VibrationPattern, strength: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern.plainTimings, -1)
            return
        }

        val effect = if (vibrator.hasAmplitudeControl()) {
            val amplitudes = IntArray(pattern.strengths.size) { step ->
                val stepStrength = pattern.strengths[step]
                if (stepStrength == 0f) 0 else (stepStrength * strength * 255f).toInt().coerceIn(1, 255)
            }
            VibrationEffect.createWaveform(pattern.timings, amplitudes, -1)
        } else {
            VibrationEffect.createWaveform(pattern.plainTimings, -1)
        }

        // Alarm usage, so the phone's touch-vibration setting doesn't mute or weaken it
        vibrator.vibrate(
            effect,
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
    }

    fun triggerVibrationPreview(mode: GentleVibrationMode) {
        scope.launch {
            val vibrator = getVibrator() ?: return@launch
            playVibration(vibrator, mode.pattern(), strength = 1f)
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

        autoStopJob?.cancel()
        autoStopJob = null

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
