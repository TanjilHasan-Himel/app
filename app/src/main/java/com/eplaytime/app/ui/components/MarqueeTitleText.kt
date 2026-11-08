package com.eplaytime.app.ui.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import com.eplaytime.app.ui.theme.OutfitFontFamily

/**
 * Intelligent adaptive marquee text.
 * When alignStart is true, always aligns to Start.
 * When alignStart is false:
 * - If text fits, it centers cleanly.
 * - If text overflows, it shifts to TextAlign.Start and scrolls with basicMarquee,
 *   guaranteeing the beginning of the title is NEVER clipped or cut off.
 */
@Composable
fun MarqueeTitleText(
    text: String,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    color: Color,
    modifier: Modifier = Modifier,
    alignStart: Boolean = false,
    fontFamily: FontFamily = OutfitFontFamily,
    initialDelayMillis: Int = 2000
) {
    var isOverflowing by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        fontSize = fontSize,
        color = color,
        maxLines = 1,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow) {
                isOverflowing = true
            }
        },
        textAlign = if (alignStart || isOverflowing) TextAlign.Start else TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isOverflowing) {
                    Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = initialDelayMillis
                    )
                } else Modifier
            )
    )
}
