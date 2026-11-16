package com.eplaytime.app

import com.eplaytime.app.dsp.BiquadFilter
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class BiquadFilterTest {
    @Test
    fun bypass_isBitIdentical() {
        val input = floatArrayOf(-0.8f, 0.2f, 0.5f, -0.1f)
        val output = input.copyOf()
        BiquadFilter(channels = 2).processInterleaved(output)
        assertArrayEquals(input, output, 0f)
    }

    @Test
    fun flatFilter_isIdentity() {
        val input = FloatArray(2_048) { sin(it * 0.031).toFloat() * 0.5f }
        val output = input.copyOf()
        BiquadFilter(channels = 1).apply {
            enabled = true
            update(BiquadFilter.Coefficients.FLAT, rampFrames = 0)
            processInterleaved(output)
        }
        assertArrayEquals(input, output, 0.000001f)
    }

    @Test
    fun peakingFilter_reachesRequestedGainAtCenterFrequency() {
        val sampleRate = 48_000
        val frequency = 1_000.0
        val input = FloatArray(sampleRate) { i -> (0.1 * sin(2.0 * PI * frequency * i / sampleRate)).toFloat() }
        val output = input.copyOf()
        BiquadFilter(channels = 1).apply {
            enabled = true
            update(BiquadFilter.Coefficients.peaking(sampleRate, frequency, 6.0, 1.0), rampFrames = 0)
            processInterleaved(output)
        }
        val inputRms = rms(input, sampleRate / 2)
        val outputRms = rms(output, sampleRate / 2)
        val ratio = outputRms / inputRms
        assertTrue("Expected approximately +6 dB, ratio=$ratio", abs(ratio - 1.995) < 0.04)
        assertTrue(output.all { it.isFinite() })
    }

    private fun rms(values: FloatArray, start: Int): Double {
        val meanSquare = values.drop(start).sumOf { it.toDouble() * it } / (values.size - start)
        return sqrt(meanSquare)
    }
}
