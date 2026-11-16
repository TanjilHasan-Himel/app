package com.eplaytime.app.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/** Isolated PCM biquad foundation. It is not connected to the playback path yet. */
class BiquadFilter(private val channels: Int = 2) {
    init {
        require(channels > 0)
    }

    data class Coefficients(
        val b0: Double,
        val b1: Double,
        val b2: Double,
        val a1: Double,
        val a2: Double
    ) {
        init {
            require(listOf(b0, b1, b2, a1, a2).all(Double::isFinite))
        }

        companion object {
            val FLAT = Coefficients(1.0, 0.0, 0.0, 0.0, 0.0)

            fun peaking(sampleRate: Int, frequencyHz: Double, gainDb: Double, q: Double): Coefficients {
                require(sampleRate > 0)
                require(frequencyHz > 0.0 && frequencyHz < sampleRate / 2.0)
                require(gainDb in -24.0..24.0)
                require(q in 0.1..20.0)

                val a = 10.0.pow(gainDb / 40.0)
                val omega = 2.0 * PI * frequencyHz / sampleRate
                val alpha = sin(omega) / (2.0 * q)
                val a0 = 1.0 + alpha / a
                return Coefficients(
                    b0 = (1.0 + alpha * a) / a0,
                    b1 = (-2.0 * cos(omega)) / a0,
                    b2 = (1.0 - alpha * a) / a0,
                    a1 = (-2.0 * cos(omega)) / a0,
                    a2 = (1.0 - alpha / a) / a0
                )
            }
        }
    }

    private var current = Coefficients.FLAT
    private var target = current
    private var rampFramesLeft = 0
    private val x1 = DoubleArray(channels)
    private val x2 = DoubleArray(channels)
    private val y1 = DoubleArray(channels)
    private val y2 = DoubleArray(channels)

    var enabled: Boolean = false

    fun update(coefficients: Coefficients, rampFrames: Int = 256) {
        require(rampFrames >= 0)
        target = coefficients
        rampFramesLeft = rampFrames
        if (rampFrames == 0) current = coefficients
    }

    fun processInterleaved(samples: FloatArray) {
        require(samples.size % channels == 0) { "PCM buffer must contain complete frames" }
        if (!enabled) return

        for (frameStart in samples.indices step channels) {
            advanceCoefficients()
            for (channel in 0 until channels) {
                val x0 = samples[frameStart + channel].toDouble()
                val y0 = current.b0 * x0 + current.b1 * x1[channel] + current.b2 * x2[channel] -
                    current.a1 * y1[channel] - current.a2 * y2[channel]
                x2[channel] = x1[channel]
                x1[channel] = x0
                y2[channel] = y1[channel]
                y1[channel] = y0
                samples[frameStart + channel] = y0.coerceIn(-1.0, 1.0).toFloat()
            }
        }
    }

    fun reset() {
        x1.fill(0.0)
        x2.fill(0.0)
        y1.fill(0.0)
        y2.fill(0.0)
    }

    private fun advanceCoefficients() {
        if (rampFramesLeft <= 0) return
        val amount = 1.0 / rampFramesLeft
        current = Coefficients(
            b0 = current.b0 + (target.b0 - current.b0) * amount,
            b1 = current.b1 + (target.b1 - current.b1) * amount,
            b2 = current.b2 + (target.b2 - current.b2) * amount,
            a1 = current.a1 + (target.a1 - current.a1) * amount,
            a2 = current.a2 + (target.a2 - current.a2) * amount
        )
        rampFramesLeft--
    }
}
