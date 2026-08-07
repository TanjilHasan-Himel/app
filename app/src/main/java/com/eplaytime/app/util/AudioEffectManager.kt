package com.eplaytime.app.util

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer

object AudioEffectManager {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var effectsEnabled: Boolean = true

    fun initialize(audioSessionId: Int) {
        if (audioSessionId == 0) return
        release()
        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = true
            }
            effectsEnabled = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setEnabled(enabled: Boolean) {
        effectsEnabled = enabled
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isEnabled(): Boolean = effectsEnabled

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        equalizer = null
        bassBoost = null
    }

    fun setBassBoostStrength(strength: Short) {
        if (bassBoost?.strengthSupported == true) {
            bassBoost?.setStrength(strength)
        }
    }

    fun setBandLevel(band: Short, level: Short) {
        equalizer?.setBandLevel(band, level)
    }

    fun getNumberOfBands(): Short = equalizer?.numberOfBands ?: 0
    fun getBandLevel(band: Short): Short = equalizer?.getBandLevel(band) ?: 0
    fun getCenterFreq(band: Short): Int = equalizer?.getCenterFreq(band) ?: 0
    fun getBandLevelRange(): ShortArray = equalizer?.bandLevelRange ?: shortArrayOf(0, 0)
}
