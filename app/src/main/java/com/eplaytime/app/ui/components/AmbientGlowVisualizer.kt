package com.eplaytime.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * AmbientGlowVisualizer - YouTube-Style Music-Reactive Ambient Mode
 *
 * Casts a rich, dynamic, multi-orb atmospheric light wash behind the player stage.
 * Reacts in real time to the audio frequency spectrum:
 *  - Bass Magnitude pulses and expands the central album bloom.
 *  - Audio Energy / Mid-Treble shifts the secondary ambient diffusion orbs.
 *  - Organic phase oscillation ensures the light is alive even during quiet passages.
 *  - Seamlessly covers the entire viewport from top edge to bottom edge (zero cut-off).
 */
@Composable
fun AmbientGlowVisualizer(
    primaryColor: Color,
    secondaryColor: Color,
    bassMagnitude: Float,
    audioEnergy: Float,
    isPlaying: Boolean,
    isZenMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow_phase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient_phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val effectiveBass = if (isPlaying) bassMagnitude else 0f
        val effectiveEnergy = if (isPlaying) audioEnergy else 0f

        // Zen Mode uses a calmer, deeper alpha for restful night viewing
        val alphaMultiplier = if (isZenMode) 0.55f else 1.0f

        // 1. TOP CINEMATIC AMBIENT DIFFUSION (YouTube Video Ambient Style)
        // Radiates from the top status/header area down towards the stage
        val topDriftX = w * 0.5f + sin(phase) * (w * 0.10f)
        val topDriftY = h * 0.08f + cos(phase * 0.7f) * (h * 0.04f)
        val topRadius = w * (0.85f + effectiveEnergy * 0.28f)
        val topAlpha = ((0.35f + effectiveEnergy * 0.30f) * alphaMultiplier).coerceIn(0.18f, 0.80f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = topAlpha),
                    secondaryColor.copy(alpha = topAlpha * 0.50f),
                    Color.Transparent
                ),
                center = Offset(topDriftX, topDriftY),
                radius = topRadius
            ),
            center = Offset(topDriftX, topDriftY),
            radius = topRadius
        )

        // 2. CENTER STAGE REACTIVE BLOOM (Directly behind Album Art / Lyrics)
        val centerStageY = if (isZenMode) h * 0.42f else h * 0.35f
        val centerDriftX = w * 0.5f + cos(phase * 1.2f) * (w * 0.05f)
        val centerBloomRadius = w * (0.75f + effectiveBass * 0.50f + effectiveEnergy * 0.18f)
        val centerAlpha = ((0.42f + effectiveBass * 0.48f) * alphaMultiplier).coerceIn(0.24f, 0.95f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = centerAlpha),
                    secondaryColor.copy(alpha = centerAlpha * 0.45f),
                    Color.Transparent
                ),
                center = Offset(centerDriftX, centerStageY),
                radius = centerBloomRadius
            ),
            center = Offset(centerDriftX, centerStageY),
            radius = centerBloomRadius
        )

        // 3. MID-RIGHT ACCENT LIGHT ORB (Slightly offset for multi-chromatic fluid depth)
        val orbX = w * 0.80f + sin(phase * 0.9f) * (w * 0.07f)
        val orbY = centerStageY + h * 0.15f + cos(phase * 1.1f) * (h * 0.05f)
        val orbRadius = w * (0.60f + effectiveEnergy * 0.30f)
        val orbAlpha = ((0.30f + effectiveEnergy * 0.32f) * alphaMultiplier).coerceIn(0.14f, 0.72f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = orbAlpha),
                    primaryColor.copy(alpha = orbAlpha * 0.35f),
                    Color.Transparent
                ),
                center = Offset(orbX, orbY),
                radius = orbRadius
            ),
            center = Offset(orbX, orbY),
            radius = orbRadius
        )

        // 4. BOTTOM AMBIENT ATMOSPHERIC DIFFUSION (Breathes warm ambient glow behind bottom controls)
        val bottomDriftX = w * 0.45f + sin(phase * 0.75f) * (w * 0.08f)
        val bottomDriftY = h * 0.85f + cos(phase * 0.65f) * (h * 0.04f)
        val bottomRadius = w * (0.85f + effectiveBass * 0.35f)
        val bottomAlpha = ((0.32f + effectiveBass * 0.30f) * alphaMultiplier).coerceIn(0.16f, 0.70f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = bottomAlpha),
                    secondaryColor.copy(alpha = bottomAlpha * 0.45f),
                    Color.Transparent
                ),
                center = Offset(bottomDriftX, bottomDriftY),
                radius = bottomRadius
            ),
            center = Offset(bottomDriftX, bottomDriftY),
            radius = bottomRadius
        )
    }
}
