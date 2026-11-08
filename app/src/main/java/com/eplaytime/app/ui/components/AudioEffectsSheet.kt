package com.eplaytime.app.ui.components

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eplaytime.app.ui.theme.OutfitFontFamily
import com.eplaytime.app.ui.theme.SoftGold
import com.eplaytime.app.ui.theme.TextSecondary
import com.eplaytime.app.ui.viewmodel.MusicViewModel
import com.eplaytime.app.util.AudioEffectManager
import kotlin.math.roundToInt

private val SheetBgColor = Color(0xFF0F0F12)
private val CardSurfaceColor = Color(0xFF17171C)
private val GoldAccent = SoftGold
private val TrackInactiveColor = Color(0xFF2A2A32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEffectsSheet(
    audioSessionId: Int,
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val bassStrength by viewModel.bassBoostStrength.collectAsState()
    val virtualizerStrength by viewModel.virtualizerStrength.collectAsState()
    val eqBandsRaw by viewModel.eqBandsLevels.collectAsState()
    val isInitialized by AudioEffectManager.isInitialized.collectAsState()
    val activeSessionId by AudioEffectManager.activeSessionId.collectAsState()

    val effectiveSessionId = remember(audioSessionId, activeSessionId) {
        if (audioSessionId > 0) audioSessionId else activeSessionId
    }

    val bandCount = remember(isInitialized, effectiveSessionId) {
        AudioEffectManager.getNumberOfBands().toInt()
    }.coerceAtLeast(0)

    val visibleBands = remember(bandCount) { minOf(5, bandCount) }
    val bandRange = AudioEffectManager.getBandLevelRange()
    val minEq = bandRange.getOrNull(0)?.toFloat() ?: -1500f
    val maxEq = bandRange.getOrNull(1)?.toFloat() ?: 1500f

    var effectsEnabled by remember(effectiveSessionId) {
        mutableStateOf(AudioEffectManager.isEnabled())
    }
    var bassValue by remember(effectiveSessionId, bassStrength) {
        mutableStateOf(bassStrength.coerceIn(0, 1000))
    }
    var virtualizerValue by remember(effectiveSessionId, virtualizerStrength) {
        mutableStateOf(virtualizerStrength.coerceIn(0, 1000))
    }

    val bandValues = remember { mutableStateListOf<Float>() }

    LaunchedEffect(Unit) {
        viewModel.requestAudioSessionId()
    }

    LaunchedEffect(effectiveSessionId) {
        if (effectiveSessionId > 0) {
            AudioEffectManager.initialize(effectiveSessionId)
            AudioEffectManager.setEnabled(effectsEnabled)
        }
    }

    LaunchedEffect(eqBandsRaw, visibleBands, bandCount) {
        bandValues.clear()
        val storedLevels = eqBandsRaw
            .split(',')
            .mapNotNull { it.trim().toFloatOrNull() }

        if (visibleBands > 0) {
            repeat(visibleBands) { index ->
                val level = storedLevels.getOrNull(index)
                    ?: AudioEffectManager.getBandLevel(index.toShort()).toFloat()
                bandValues += level
            }
        }
    }

    // Fallback if audio session is still unavailable
    if (effectiveSessionId <= 0 && !isInitialized) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = SheetBgColor,
            scrimColor = Color.Black.copy(alpha = 0.75f),
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = GoldAccent.copy(alpha = 0.4f))
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Audio Engine Initializing",
                    color = Color.White,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Connecting to audio session engine. Please tap Retry or start playing a song.",
                    color = TextSecondary,
                    fontFamily = OutfitFontFamily,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.requestAudioSessionId()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry Connection", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBgColor,
        scrimColor = Color.Black.copy(alpha = 0.75f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = GoldAccent.copy(alpha = 0.45f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar: Title, Master Switch, and System EQ Shortcut
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Equalizer & Effects",
                        color = Color.White,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = if (effectsEnabled) "DSP Processing Active" else "Bypassed (Original Audio)",
                        color = if (effectsEnabled) GoldAccent else TextSecondary,
                        fontFamily = OutfitFontFamily,
                        fontSize = 12.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // System Equalizer Shortcut (Opens Samsung SoundAlive / Dolby Atmos)
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            openSystemEqualizer(context, effectiveSessionId, viewModel)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "System EQ",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Switch(
                        checked = effectsEnabled,
                        onCheckedChange = { enabled ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            effectsEnabled = enabled
                            AudioEffectManager.setEnabled(enabled)
                            if (enabled) {
                                bandValues.forEachIndexed { index, value ->
                                    AudioEffectManager.setBandLevel(index.toShort(), value.roundToInt().toShort())
                                }
                                AudioEffectManager.setBassBoostStrength(bassValue.toShort())
                                AudioEffectManager.setVirtualizerStrength(virtualizerValue.toShort())
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = GoldAccent,
                            uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }
            }

            // Real-Time Frequency Response Curve (Canvas Bézier Spline Graph)
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurfaceColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "FREQUENCY RESPONSE",
                            color = TextSecondary,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (effectsEnabled) "LIVE DSP CURVE" else "FLAT (BYPASS)",
                            color = if (effectsEnabled) GoldAccent else TextSecondary.copy(alpha = 0.6f),
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FrequencyCurveGraph(
                        bandValues = if (effectsEnabled) bandValues.toList() else List(visibleBands) { 0f },
                        minEq = minEq,
                        maxEq = maxEq,
                        accentColor = if (effectsEnabled) GoldAccent else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                    )
                }
            }

            // Presets Horizontal Rail
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PRESETS",
                    color = TextSecondary,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(AudioEffectManager.PRESETS.keys.toList()) { presetName ->
                        val presetLevels = AudioEffectManager.PRESETS[presetName] ?: emptyList()
                        val isSelected = bandValues.size == presetLevels.size &&
                                bandValues.map { it.roundToInt().toShort() } == presetLevels

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) GoldAccent else CardSurfaceColor
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) GoldAccent else Color.White.copy(alpha = 0.12f),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable(enabled = effectsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.applyEqPreset(presetName)
                                    bandValues.clear()
                                    presetLevels.forEach { bandValues.add(it.toFloat()) }
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = presetName,
                                fontFamily = OutfitFontFamily,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else if (effectsEnabled) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }

            // 5-Band Interactive Vertical Faders (Native Touch Gestures)
            if (visibleBands > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurfaceColor),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(visibleBands) { index ->
                            val freqHz = AudioEffectManager.getCenterFreq(index.toShort())
                            val label = formatBandLabel(freqHz)
                            val currentValue = bandValues.getOrNull(index) ?: 0f

                            SmoothVerticalFader(
                                label = label,
                                value = currentValue,
                                valueRange = minEq..maxEq,
                                enabled = effectsEnabled,
                                accentColor = GoldAccent,
                                onValueChange = { newValue ->
                                    if (bandValues.size > index) {
                                        bandValues[index] = newValue
                                        viewModel.setEqBandLevel(index.toShort(), newValue.roundToInt().toShort())
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // Sound Enhancement Controls: Bass Boost & 3D Spatial Virtualizer
            Text(
                text = "SOUND ENHANCEMENTS",
                color = TextSecondary,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bass Boost Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurfaceColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = if (effectsEnabled) GoldAccent else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Bass",
                                    color = Color.White,
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = "${(bassValue / 10f).roundToInt()}%",
                                color = if (effectsEnabled) GoldAccent else TextSecondary,
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Slider(
                            value = bassValue.toFloat(),
                            onValueChange = { value ->
                                bassValue = value.roundToInt().coerceIn(0, 1000)
                                if (effectsEnabled) {
                                    viewModel.setBassBoostStrength(bassValue)
                                }
                            },
                            valueRange = 0f..1000f,
                            enabled = effectsEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = GoldAccent,
                                activeTrackColor = GoldAccent,
                                inactiveTrackColor = TrackInactiveColor,
                                disabledThumbColor = Color.White.copy(alpha = 0.2f),
                                disabledActiveTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }

                // 3D Spatial Virtualizer Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurfaceColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = null,
                                    tint = if (effectsEnabled) GoldAccent else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "3D Audio",
                                    color = Color.White,
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = "${(virtualizerValue / 10f).roundToInt()}%",
                                color = if (effectsEnabled) GoldAccent else TextSecondary,
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Slider(
                            value = virtualizerValue.toFloat(),
                            onValueChange = { value ->
                                virtualizerValue = value.roundToInt().coerceIn(0, 1000)
                                if (effectsEnabled) {
                                    viewModel.setVirtualizerStrength(virtualizerValue)
                                }
                            },
                            valueRange = 0f..1000f,
                            enabled = effectsEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = GoldAccent,
                                activeTrackColor = GoldAccent,
                                inactiveTrackColor = TrackInactiveColor,
                                disabledThumbColor = Color.White.copy(alpha = 0.2f),
                                disabledActiveTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Frequency Response Curve Graph rendered dynamically with cubic Bézier splines
 */
@Composable
private fun FrequencyCurveGraph(
    bandValues: List<Float>,
    minEq: Float,
    maxEq: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val midY = height / 2f

        // Draw horizontal grid lines (-15dB, 0dB, +15dB)
        drawLine(
            color = Color.White.copy(alpha = 0.08f),
            start = Offset(0f, height * 0.15f),
            end = Offset(width, height * 0.15f),
            strokeWidth = 1f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.15f),
            start = Offset(0f, midY),
            end = Offset(width, midY),
            strokeWidth = 1.5f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.08f),
            start = Offset(0f, height * 0.85f),
            end = Offset(width, height * 0.85f),
            strokeWidth = 1f
        )

        if (bandValues.isEmpty()) return@Canvas

        val pointCount = bandValues.size
        val points = mutableListOf<Offset>()

        // Map band values to canvas (x, y) coordinates
        bandValues.forEachIndexed { index, level ->
            val fractionX = if (pointCount > 1) index.toFloat() / (pointCount - 1) else 0.5f
            val x = width * 0.08f + fractionX * (width * 0.84f)
            val normalizedLevel = ((level - minEq) / (maxEq - minEq)).coerceIn(0f, 1f)
            val y = height * (1f - normalizedLevel)
            points.add(Offset(x, y))
        }

        // Build smooth Bézier spline path
        val path = Path()
        val fillPath = Path()

        if (points.isNotEmpty()) {
            path.moveTo(0f, points.first().y)
            path.lineTo(points.first().x, points.first().y)

            fillPath.moveTo(0f, height)
            fillPath.lineTo(0f, points.first().y)
            fillPath.lineTo(points.first().x, points.first().y)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val midX = (p0.x + p1.x) / 2f

                path.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                fillPath.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
            }

            path.lineTo(width, points.last().y)
            fillPath.lineTo(width, points.last().y)
            fillPath.lineTo(width, height)
            fillPath.close()

            // Draw glowing gradient fill under curve
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.28f),
                        accentColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = height
                )
            )

            // Draw primary curve line
            drawPath(
                path = path,
                color = accentColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw glowing control node dots
            points.forEach { point ->
                // Outer glow halo
                drawCircle(
                    color = accentColor.copy(alpha = 0.25f),
                    radius = 7.dp.toPx(),
                    center = point
                )
                // Solid center node
                drawCircle(
                    color = accentColor,
                    radius = 3.5.dp.toPx(),
                    center = point
                )
                // White highlight core
                drawCircle(
                    color = Color.White,
                    radius = 1.5.dp.toPx(),
                    center = point
                )
            }
        }
    }
}

/**
 * Butter-Smooth Vertical Fader with native vertical touch gestures
 */
@Composable
private fun SmoothVerticalFader(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    accentColor: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val normalizedFraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Frequency Label Badge
        Text(
            text = label,
            color = if (enabled) Color.White else TextSecondary,
            fontFamily = OutfitFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )

        // Vertical Fader Track & Knob Container
        BoxWithConstraints(
            modifier = Modifier
                .height(150.dp)
                .width(42.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val fraction = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                        val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onValueChange(newValue)
                    }
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        val fraction = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        val newValue = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val trackHeight = maxHeight
            val trackHeightPx = constraints.maxHeight.toFloat()
            val midYPx = trackHeightPx / 2f
            val knobYPx = (1f - normalizedFraction) * trackHeightPx

            // Background Track Rail
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            )

            // Center 0dB Reference Tick
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.25f))
            )

            // Active Track Fill extending from 0dB center line
            val fillTop = minOf(midYPx, knobYPx)
            val fillHeight = kotlin.math.abs(knobYPx - midYPx).coerceAtLeast(1f)

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp)
            ) {
                if (enabled) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(accentColor, accentColor.copy(alpha = 0.6f))
                        ),
                        topLeft = Offset(0f, fillTop),
                        size = androidx.compose.ui.geometry.Size(size.width, fillHeight)
                    )
                }
            }

            // Luxury Metallic/Gold Knob
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, (knobYPx - 10.dp.toPx()).roundToInt()) }
                    .width(34.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (enabled) {
                            Brush.verticalGradient(
                                listOf(Color(0xFF2C2C34), Color(0xFF1E1E24))
                            )
                        } else {
                            Brush.verticalGradient(
                                listOf(Color(0xFF202025), Color(0xFF151518))
                            )
                        }
                    )
                    .border(
                        1.5.dp,
                        if (enabled) accentColor else Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Center Grip Line
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(2.dp)
                        .background(if (enabled) accentColor else Color.White.copy(alpha = 0.3f))
                )
            }
        }

        // Real-Time dB Gain Label
        Text(
            text = formatDbLabel(value),
            color = if (enabled && kotlin.math.abs(value) > 50) accentColor else TextSecondary,
            fontFamily = OutfitFontFamily,
            fontSize = 10.sp,
            fontWeight = if (kotlin.math.abs(value) > 50) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Launch Samsung's native SoundAlive / Dolby Atmos system EQ panel
 */
private fun openSystemEqualizer(context: Context, audioSessionId: Int, viewModel: MusicViewModel) {
    try {
        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            viewModel.showInAppNotification("System equalizer is not available on this device")
        }
    } catch (e: Exception) {
        viewModel.showInAppNotification("Unable to launch system equalizer")
    }
}

private fun formatBandLabel(centerFreqMilliHz: Int): String {
    val hz = centerFreqMilliHz / 1000
    return if (hz >= 1000) {
        val kHz = hz / 1000f
        if (kHz % 1f == 0f) "${kHz.toInt()}k" else String.format("%.1fk", kHz)
    } else {
        "${hz}Hz"
    }
}

private fun formatDbLabel(level: Float): String {
    val db = (level / 100f).roundToInt()
    return if (db > 0) "+${db}dB" else if (db < 0) "${db}dB" else "0dB"
}
