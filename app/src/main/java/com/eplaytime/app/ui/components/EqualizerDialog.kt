package com.eplaytime.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eplaytime.app.ui.theme.OutfitFontFamily
import com.eplaytime.app.ui.theme.SoftGold
import com.eplaytime.app.ui.theme.TextSecondary
import com.eplaytime.app.util.AudioEffectManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerDialog(
    bassStrength: Int,
    onBassStrengthChanged: (Int) -> Unit,
    onBandLevelChanged: (Short, Short) -> Unit,
    onDismiss: () -> Unit
) {
    val numBands = AudioEffectManager.getNumberOfBands()
    val bandRange = AudioEffectManager.getBandLevelRange()
    val minEQ = bandRange.getOrNull(0) ?: -1500
    val maxEQ = bandRange.getOrNull(1) ?: 1500

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Pro Equalizer",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = SoftGold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Bass Boost
                Text(
                    "Bass Boost",
                    fontFamily = OutfitFontFamily,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Slider(
                    value = bassStrength.toFloat(),
                    onValueChange = { onBassStrengthChanged(it.toInt()) },
                    valueRange = 0f..1000f,
                    colors = SliderDefaults.colors(
                        thumbColor = SoftGold,
                        activeTrackColor = SoftGold,
                        inactiveTrackColor = Color.DarkGray
                    )
                )

                // EQ Bands
                if (numBands > 0) {
                    Text(
                        "Equalizer Bands",
                        fontFamily = OutfitFontFamily,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (i in 0 until numBands) {
                            val freq = AudioEffectManager.getCenterFreq(i.toShort())
                            val currentLevel = AudioEffectManager.getBandLevel(i.toShort())

                            // Vertical Slider
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                // dB text
                                Text(
                                    "${currentLevel / 100} dB",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = OutfitFontFamily
                                )

                                // Actual slider logic for vertical orientation requires rotation or a custom component, 
                                // but we can simulate it by wrapping a normal Slider with a bit less width if needed.
                                // For simplicity and stability in Compose, horizontal sliders inside a Column work well for bands too.
                                // But since we want "pro eq", standard vertical is better, but harder natively.
                                // We'll just do horizontal sliders for bands to keep it clean.
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in 0 until numBands) {
                            val freq = AudioEffectManager.getCenterFreq(i.toShort())
                            var currentLevel by remember { mutableStateOf(AudioEffectManager.getBandLevel(i.toShort()).toFloat()) }
                            val freqLabel = if (freq >= 1000000) "${freq / 1000000} kHz" else "${freq / 1000} Hz"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = freqLabel,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontFamily = OutfitFontFamily,
                                    modifier = Modifier.width(50.dp)
                                )
                                Slider(
                                    value = currentLevel,
                                    onValueChange = {
                                        currentLevel = it
                                        onBandLevelChanged(i.toShort(), it.toInt().toShort())
                                    },
                                    valueRange = minEQ.toFloat()..maxEQ.toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = SoftGold,
                                        activeTrackColor = SoftGold,
                                        inactiveTrackColor = Color.DarkGray
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                } else {
                    Text("Equalizer not supported on this device.", color = TextSecondary, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("DONE", color = SoftGold, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(16.dp)
    )
}
