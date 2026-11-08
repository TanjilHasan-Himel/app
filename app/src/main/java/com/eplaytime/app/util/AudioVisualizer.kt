package com.eplaytime.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

/**
 * VisualizerHelper - Connects to Android Visualizer API
 * Captures FFT frequency data, computes real-time bass magnitude & energy for ambient lighting,
 * and exposes normalized frequency data for spectrum visualizers.
 * Falls back to an authentic simulated audio-envelope reactive engine when RECORD_AUDIO
 * permission is missing or when hardware audio capture is unsupported by the OEM.
 */
class VisualizerHelper {

    private var visualizer: Visualizer? = null
    private var smoothedData = FloatArray(0)
    private var smoothedBass = 0f
    private var smoothedEnergy = 0f

    // Normalized frequency bands (0.0 to 1.0)
    private val _waveform = MutableStateFlow<List<Float>>(emptyList())
    val waveform: StateFlow<List<Float>> = _waveform.asStateFlow()

    // Real-time bass magnitude (0.0 to 1.0) with fast-attack, smooth-decay physics
    private val _bassMagnitude = MutableStateFlow(0f)
    val bassMagnitude: StateFlow<Float> = _bassMagnitude.asStateFlow()

    // Overall energy (0.0 to 1.0) for ambient breathing
    private val _energy = MutableStateFlow(0f)
    val energy: StateFlow<Float> = _energy.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var fallbackJob: Job? = null
    private val helperScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun start(context: Context, audioSessionId: Int) {
        stop() // Release any existing session or fallback

        // Check RECORD_AUDIO permission safely
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission && audioSessionId > 0) {
            try {
                visualizer = Visualizer(audioSessionId).apply {
                    val captureRanges = Visualizer.getCaptureSizeRange()
                    captureSize = captureRanges[1].coerceAtMost(1024)

                    setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {}

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            fft?.let { bytes ->
                                val n = bytes.size / 2
                                if (smoothedData.size != n) {
                                    smoothedData = FloatArray(n)
                                }

                                val captureSize = min(n, 64)
                                val output = ArrayList<Float>(captureSize)

                                var bassSum = 0f
                                var bassCount = 0
                                var totalEnergySum = 0f

                                for (i in 0 until captureSize) {
                                    val rfk = bytes[2 * i].toFloat()
                                    val ifk = bytes[2 * i + 1].toFloat()
                                    val magnitude = (hypot(rfk, ifk) / 128f).coerceIn(0f, 1.5f)

                                    smoothedData[i] = smoothedData[i] * 0.80f + magnitude * 0.20f
                                    output.add(smoothedData[i])

                                    if (i in 1..7) {
                                        bassSum += magnitude
                                        bassCount++
                                    }
                                    totalEnergySum += magnitude
                                }

                                val rawBass = if (bassCount > 0) (bassSum / bassCount).coerceIn(0f, 1f) else 0f
                                if (rawBass > smoothedBass) {
                                    smoothedBass = smoothedBass * 0.35f + rawBass * 0.65f
                                } else {
                                    smoothedBass = smoothedBass * 0.86f + rawBass * 0.14f
                                }

                                val rawEnergy = (totalEnergySum / captureSize).coerceIn(0f, 1f)
                                smoothedEnergy = smoothedEnergy * 0.85f + rawEnergy * 0.15f

                                _waveform.value = output
                                _bassMagnitude.value = smoothedBass
                                _energy.value = smoothedEnergy
                            }
                        }
                    }, Visualizer.getMaxCaptureRate() / 2, false, true)

                    enabled = true
                }
                _isRunning.value = true
                Log.d("VisualizerHelper", "Hardware Visualizer attached to session: $audioSessionId")
                return
            } catch (e: Exception) {
                Log.w("VisualizerHelper", "Hardware visualizer initialization failed, switching to simulated envelope", e)
                try {
                    visualizer?.release()
                } catch (_: Exception) {}
                visualizer = null
            }
        } else {
            Log.d("VisualizerHelper", "RECORD_AUDIO not granted or sessionId invalid; using simulated audio envelope")
        }

        // Fallback: Start smooth, rhythmic simulated audio-envelope reactive engine
        startSimulatedFallback()
    }

    private fun startSimulatedFallback() {
        fallbackJob?.cancel()
        _isRunning.value = true
        fallbackJob = helperScope.launch {
            var step = 0f
            while (isActive) {
                step += 0.08f
                // Rhythmic beat pulse (~120 BPM) with sub-bass harmonics
                val beatPhase = (step % (Math.PI.toFloat() * 2f))
                val rawBassPulse = sin(beatPhase).coerceAtLeast(0f).pow(2.2f)
                val rawSnarePulse = sin(beatPhase * 2f + 1f).coerceAtLeast(0f).pow(3f) * 0.5f

                val targetBass = (rawBassPulse * 0.85f + 0.15f).coerceIn(0.1f, 1.0f)
                smoothedBass = if (targetBass > smoothedBass) {
                    smoothedBass * 0.40f + targetBass * 0.60f // Fast attack
                } else {
                    smoothedBass * 0.88f + targetBass * 0.12f // Gentle decay
                }

                val targetEnergy = (targetBass * 0.65f + rawSnarePulse * 0.35f).coerceIn(0.1f, 1.0f)
                smoothedEnergy = smoothedEnergy * 0.85f + targetEnergy * 0.15f

                // Generate 48 simulated spectrum bands
                val bands = ArrayList<Float>(48)
                for (b in 0 until 48) {
                    val bandFreq = (b.toFloat() / 48f)
                    val freqFactor = exp(-bandFreq * 1.8f) // Natural audio roll-off
                    val wobble = sin(step * (1.2f + b * 0.05f) + b * 0.4f) * 0.25f
                    val amp = ((smoothedBass * freqFactor + wobble + 0.1f) * (1f - bandFreq * 0.4f)).coerceIn(0.05f, 1.0f)
                    bands.add(amp)
                }

                _waveform.value = bands
                _bassMagnitude.value = smoothedBass
                _energy.value = smoothedEnergy

                delay(33) // ~30 fps update
            }
        }
    }

    fun stop() {
        fallbackJob?.cancel()
        fallbackJob = null
        try {
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
        } catch (e: Exception) {
            Log.w("VisualizerHelper", "Error releasing visualizer", e)
        } finally {
            _isRunning.value = false
            _bassMagnitude.value = 0f
            _energy.value = 0f
            _waveform.value = emptyList()
            smoothedBass = 0f
            smoothedEnergy = 0f
        }
    }
}


