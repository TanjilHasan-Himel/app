package com.eplaytime.app.ui.components

import android.media.audiofx.BassBoost
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eplaytime.app.ui.theme.NeonCyan
import com.eplaytime.app.ui.theme.OceanDark
import com.eplaytime.app.ui.theme.OceanDarkGlass
import com.eplaytime.app.ui.theme.OutfitFontFamily
import com.eplaytime.app.ui.theme.SoftGold
import com.eplaytime.app.ui.theme.TextSecondary
import com.eplaytime.app.ui.viewmodel.MusicViewModel
import com.eplaytime.app.util.AudioEffectManager
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEffectsSheet(
    audioSessionId: Int,
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bassStrength by viewModel.bassBoostStrength.collectAsState()
    val eqBandsRaw by viewModel.eqBandsLevels.collectAsState()
    val bandCount = remember(audioSessionId) {
        AudioEffectManager.getNumberOfBands().toInt().coerceAtLeast(0)
    }
    val visibleBands = remember(bandCount) { minOf(5, bandCount) }
    val bandRange = AudioEffectManager.getBandLevelRange()
    val minEq = bandRange.getOrNull(0)?.toFloat() ?: -1500f
    val maxEq = bandRange.getOrNull(1)?.toFloat() ?: 1500f

    var effectsEnabled by remember(audioSessionId) {
        mutableStateOf(AudioEffectManager.isEnabled())
    }
    var bassValue by remember(audioSessionId, bassStrength) { mutableStateOf(bassStrength.coerceIn(0, 1000)) }
    val bandValues = remember { mutableStateListOf<Float>() }

    LaunchedEffect(audioSessionId) {
        if (audioSessionId != 0) {
            AudioEffectManager.initialize(audioSessionId)
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

    if (audioSessionId == 0) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = OceanDarkGlass,
            scrimColor = Color.Black.copy(alpha = 0.72f),
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = Color.White.copy(alpha = 0.35f)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Audio effects are unavailable yet",
                    color = Color.White,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Start playback first so the equalizer can attach to the audio session.",
                    color = TextSecondary,
                    fontFamily = OutfitFontFamily,
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OceanDark.copy(alpha = 0.95f),
        scrimColor = Color.Black.copy(alpha = 0.72f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = NeonCyan.copy(alpha = 0.55f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Audio Effects",
                color = Color.White,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Audio Effects",
                            color = Color.White,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Turn the equalizer and bass boost on or off for this session.",
                            color = TextSecondary,
                            fontFamily = OutfitFontFamily,
                            fontSize = 12.sp
                        )
                    }

                    Switch(
                        checked = effectsEnabled,
                        onCheckedChange = { enabled ->
                            effectsEnabled = enabled
                            AudioEffectManager.setEnabled(enabled)
                            if (enabled) {
                                bandValues.forEachIndexed { index, value ->
                                    AudioEffectManager.setBandLevel(index.toShort(), value.roundToInt().toShort())
                                }
                                AudioEffectManager.setBassBoostStrength(bassValue.toShort())
                            }
                        }
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Text(
                text = "Equalizer",
                color = Color.White,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )

            if (visibleBands == 0) {
                Text(
                    text = "Equalizer not supported on this device.",
                    color = TextSecondary,
                    fontFamily = OutfitFontFamily,
                    fontSize = 13.sp
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    repeat(visibleBands) { index ->
                        val label = formatBandLabel(AudioEffectManager.getCenterFreq(index.toShort()))
                        val currentValue = bandValues.getOrNull(index) ?: 0f
                        VerticalEqualizerSlider(
                            label = label,
                            value = currentValue,
                            enabled = effectsEnabled,
                            valueRange = minEq..maxEq,
                            onValueChange = { newValue ->
                                if (bandValues.size <= index) return@VerticalEqualizerSlider
                                bandValues[index] = newValue
                                viewModel.setEqBandLevel(index.toShort(), newValue.roundToInt().toShort())
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (index != visibleBands - 1) {
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Text(
                text = "Bass Boost",
                color = Color.White,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = Color.White.copy(alpha = 0.18f),
                        disabledThumbColor = Color.White.copy(alpha = 0.3f),
                        disabledActiveTrackColor = Color.White.copy(alpha = 0.16f),
                        disabledInactiveTrackColor = Color.White.copy(alpha = 0.08f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "0",
                        color = TextSecondary,
                        fontFamily = OutfitFontFamily,
                        fontSize = 12.sp
                    )
                    Text(
                        text = bassValue.toString(),
                        color = NeonCyan,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "1000",
                        color = TextSecondary,
                        fontFamily = OutfitFontFamily,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalEqualizerSlider(
    label: String,
    value: Float,
    enabled: Boolean,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontFamily = OutfitFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )

        Box(
            modifier = Modifier
                .height(170.dp)
                .width(42.dp),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                enabled = enabled,
                modifier = Modifier
                    .graphicsLayer(rotationZ = -90f)
                    .width(170.dp),
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = Color.White.copy(alpha = 0.12f),
                    disabledThumbColor = Color.White.copy(alpha = 0.3f),
                    disabledActiveTrackColor = Color.White.copy(alpha = 0.12f),
                    disabledInactiveTrackColor = Color.White.copy(alpha = 0.08f)
                )
            )
        }

        Text(
            text = formatDbLabel(value),
            color = TextSecondary,
            fontFamily = OutfitFontFamily,
            fontSize = 10.sp
        )
    }
}

private fun formatBandLabel(centerFreqMilliHz: Int): String {
    val hz = centerFreqMilliHz / 1000
    return if (hz >= 1000) {
        String.format("%.1fkHz", hz / 1000f)
    } else {
        "${hz}Hz"
    }
}

private fun formatDbLabel(level: Float): String {
    val db = (level / 100f).roundToInt()
    return if (db > 0) "+${db}dB" else "${db}dB"
}