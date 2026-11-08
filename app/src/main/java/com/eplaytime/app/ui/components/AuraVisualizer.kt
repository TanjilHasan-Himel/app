package com.eplaytime.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.eplaytime.app.util.VisualizerHelper
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.min

/**
 * AuraVisualizer - A dynamic ring of light that shoots frequency
 * spikes outwards from behind the album cover, creating a synced aura.
 */
@Composable
fun AuraVisualizer(
    visualizerHelper: VisualizerHelper,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val waveform by visualizerHelper.waveform.collectAsState()

    // Smooth rotation so the aura feels alive even when sitting idle
    val infiniteTransition = rememberInfiniteTransition(label = "aura_rotation")
    val idlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idle_phase_aura"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2
        val coreRadius = min(cx, cy) * 0.65f // Starts right along the perimeter of the squircle artwork card
        val maxSpike = min(cx, cy) * 0.32f

        val points = waveform
        
        if (points.isEmpty() || points.all { it < 0.05f }) {
            // Idle State: Gentle glowing dots breathing in a circle
            for (i in 0 until 48) {
                val angle = (i.toFloat() / 48) * 2 * Math.PI + idlePhase
                val offset = sin(angle * 4 + idlePhase).toFloat() * 6f
                
                val r = coreRadius + offset
                val x = cx + cos(angle).toFloat() * r
                val y = cy + sin(angle).toFloat() * r
                
                drawCircle(
                    color = primaryColor.copy(alpha = 0.25f),
                    radius = 5f,
                    center = Offset(x, y)
                )
            }
            return@Canvas
        }

        // Active State: Neon Aura Ambient Spikes radiating symmetrically around artwork
        val bandCount = 48.coerceAtMost(points.size) // 48 points around the circle
        val sampleStep = points.size / bandCount.toFloat()

        for (i in 0 until bandCount) {
            val index = (i * sampleStep).toInt().coerceIn(0, points.lastIndex)
            val baseAmplitude = points[index]
            
            // Smooth 360 circle
            val angle = (i.toFloat() / bandCount) * 2 * Math.PI + idlePhase
            val value = baseAmplitude.coerceIn(0f, 1f)
            
            val spikeHeight = (value * maxSpike).coerceIn(6f, maxSpike)

            val startR = coreRadius - 4f
            val endR = coreRadius + spikeHeight
            
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()
            
            val start = Offset(cx + cosA * startR, cy + sinA * startR)
            val end = Offset(cx + cosA * endR, cy + sinA * endR)
            
            // 1. Soft Outer Neon Glow
            drawLine(
                color = primaryColor.copy(alpha = (0.25f + value * 0.55f).coerceIn(0.15f, 0.80f)),
                start = start,
                end = end,
                strokeWidth = 16f,
                cap = StrokeCap.Round
            )
            // 2. Crisp Core Light
            drawLine(
                color = Color.White.copy(alpha = (0.35f + value * 0.60f).coerceIn(0.20f, 0.95f)),
                start = start,
                end = end,
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
        }
    }
}
