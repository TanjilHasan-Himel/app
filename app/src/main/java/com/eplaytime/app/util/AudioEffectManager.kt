package com.eplaytime.app.util

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AudioEffectManager {
    private const val TAG = "AudioEffectManager"

    val PRESETS = mapOf(
        "Flat" to listOf<Short>(0, 0, 0, 0, 0),
        "Bass Boost" to listOf<Short>(600, 400, 0, 0, 0),
        "Vocal" to listOf<Short>(-200, 200, 500, 300, 0),
        "Rock" to listOf<Short>(500, 300, -100, 200, 400),
        "Pop" to listOf<Short>(-100, 200, 400, 200, -100),
        "Jazz" to listOf<Short>(400, 200, -100, 200, 300),
        "Electronic" to listOf<Short>(500, 400, 0, 200, 400),
        "Classical" to listOf<Short>(400, 300, -100, 200, 300),
        "Hip Hop" to listOf<Short>(600, 400, 0, 100, 300),
        "Acoustic" to listOf<Short>(300, 300, 100, 200, 300)
    )

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _activeSessionId = MutableStateFlow(0)
    val activeSessionId: StateFlow<Int> = _activeSessionId.asStateFlow()

    @Volatile
    private var equalizer: Equalizer? = null

    @Volatile
    private var bassBoost: BassBoost? = null

    @Volatile
    private var virtualizer: Virtualizer? = null

    private var effectsEnabled: Boolean = true

    private var desiredBassStrength: Short = 0
    private var desiredVirtualizerStrength: Short = 0
    private val desiredBandLevels = mutableMapOf<Short, Short>()

    @Synchronized
    fun initialize(audioSessionId: Int) {
        if (audioSessionId <= 0) {
            Log.w(TAG, "initialize called with invalid audioSessionId: $audioSessionId")
            return
        }
        if (_activeSessionId.value == audioSessionId && _isInitialized.value) {
            Log.d(TAG, "AudioEffectManager already initialized for session $audioSessionId")
            return
        }

        release()

        var eqSuccess = false
        var bbSuccess = false

        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                try {
                    enabled = effectsEnabled
                } catch (e: Exception) {
                    Log.w(TAG, "Failed setting equalizer.enabled to $effectsEnabled", e)
                }
            }
            eqSuccess = true
            Log.d(TAG, "Equalizer successfully attached to session $audioSessionId (bands: ${equalizer?.numberOfBands})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Equalizer for session $audioSessionId", e)
            equalizer = null
        }

        try {
            bassBoost = BassBoost(0, audioSessionId).apply {
                try {
                    enabled = effectsEnabled
                } catch (e: Exception) {
                    Log.w(TAG, "Failed setting bassBoost.enabled to $effectsEnabled", e)
                }
            }
            bbSuccess = true
            Log.d(TAG, "BassBoost successfully attached to session $audioSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize BassBoost for session $audioSessionId", e)
            bassBoost = null
        }

        try {
            virtualizer = Virtualizer(0, audioSessionId).apply {
                try {
                    enabled = effectsEnabled
                } catch (e: Exception) {
                    Log.w(TAG, "Failed setting virtualizer.enabled to $effectsEnabled", e)
                }
            }
            Log.d(TAG, "Virtualizer successfully attached to session $audioSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Virtualizer for session $audioSessionId", e)
            virtualizer = null
        }

        if (eqSuccess || bbSuccess || virtualizer != null) {
            _activeSessionId.value = audioSessionId
            _isInitialized.value = true
            applyDesiredState()
            Log.d(TAG, "AudioEffectManager initialized successfully on session $audioSessionId")
        } else {
            _activeSessionId.value = 0
            _isInitialized.value = false
            Log.e(TAG, "All audio effects failed to initialize on session $audioSessionId")
        }
    }

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        effectsEnabled = enabled
        try {
            equalizer?.enabled = enabled
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set equalizer enabled", e)
        }
        try {
            bassBoost?.enabled = enabled
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set bassBoost enabled", e)
        }
        try {
            virtualizer?.enabled = enabled
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set virtualizer enabled", e)
        }
    }

    fun isEnabled(): Boolean = effectsEnabled

    @Synchronized
    fun release() {
        try {
            equalizer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing Equalizer", e)
        }
        try {
            bassBoost?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing BassBoost", e)
        }
        try {
            virtualizer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing Virtualizer", e)
        }
        equalizer = null
        bassBoost = null
        virtualizer = null
        _activeSessionId.value = 0
        _isInitialized.value = false
    }

    @Synchronized
    fun setBassBoostStrength(strength: Short) {
        desiredBassStrength = strength.coerceIn(0, 1000).toShort()
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(desiredBassStrength)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set bass boost strength", e)
        }
    }

    @Synchronized
    fun setVirtualizerStrength(strength: Short) {
        desiredVirtualizerStrength = strength.coerceIn(0, 1000).toShort()
        try {
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(desiredVirtualizerStrength)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set virtualizer strength", e)
        }
    }

    fun isVirtualizerSupported(): Boolean {
        return try {
            virtualizer?.strengthSupported == true
        } catch (e: Exception) {
            false
        }
    }

    @Synchronized
    fun setBandLevel(band: Short, level: Short) {
        if (band < 0) return
        val range = getBandLevelRange()
        val safeLevel = level.coerceIn(range[0], range[1])
        desiredBandLevels[band] = safeLevel
        try {
            val eq = equalizer ?: return
            if (band < eq.numberOfBands) eq.setBandLevel(band, safeLevel)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set band level for band $band to $safeLevel", e)
        }
    }

    fun applyPreset(presetName: String) {
        val preset = PRESETS[presetName] ?: return
        val bands = getNumberOfBands()
        for (i in 0 until bands) {
            val level = if (i < preset.size) preset[i] else 0.toShort()
            setBandLevel(i.toShort(), level)
        }
    }

    fun getNumberOfBands(): Short {
        return try {
            equalizer?.numberOfBands ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get numberOfBands", e)
            0
        }
    }

    @Synchronized
    fun getBandLevel(band: Short): Short {
        return try {
            equalizer?.getBandLevel(band) ?: desiredBandLevels[band] ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to getBandLevel for band $band", e)
            0
        }
    }

    fun getCenterFreq(band: Short): Int {
        return try {
            equalizer?.getCenterFreq(band) ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to getCenterFreq for band $band", e)
            0
        }
    }

    fun getBandLevelRange(): ShortArray {
        return try {
            equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to getBandLevelRange", e)
            shortArrayOf(-1500, 1500)
        }
    }

    @Synchronized
    private fun applyDesiredState() {
        setBassBoostStrength(desiredBassStrength)
        setVirtualizerStrength(desiredVirtualizerStrength)
        desiredBandLevels.toMap().forEach { (band, level) -> setBandLevel(band, level) }
    }
}
