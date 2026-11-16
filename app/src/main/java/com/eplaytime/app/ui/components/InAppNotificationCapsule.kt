package com.eplaytime.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eplaytime.app.ui.theme.OutfitFontFamily
import com.eplaytime.app.ui.theme.SoftGold

/**
 * Lightweight transient feedback for actions that have no persistent on-screen state.
 */
@Composable
fun InAppNotificationCapsule(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = SoftGold
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = slideInVertically(
            initialOffsetY = { -it - 40 },
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = 380f
            )
        ) + fadeIn(tween(180)),
        exit = slideOutVertically(
            targetOffsetY = { -it - 40 },
            animationSpec = tween(280, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(220)),
        modifier = modifier
    ) {
        val nonNullMessage = message ?: ""
        val (icon, feedbackColor) = getContextualNotificationIcon(nonNullMessage, accentColor)

        Box(
            modifier = Modifier
                .clickable { onDismiss() }
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = feedbackColor,
                    modifier = Modifier.size(17.dp)
                )

                // Message Text
                Text(
                    text = nonNullMessage,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.White,
                    letterSpacing = 0.1.sp
                )
            }
        }
    }
}

private fun getContextualNotificationIcon(message: String, defaultColor: Color): Pair<ImageVector, Color> {
    val lower = message.lowercase()
    return when {
        lower.contains("shuffle") -> Pair(Icons.Default.Shuffle, defaultColor)
        lower.contains("repeat") -> Pair(Icons.Default.Repeat, defaultColor)
        lower.contains("favorite") -> Pair(Icons.Default.Favorite, Color(0xFFFF5252))
        lower.contains("loop") -> Pair(Icons.Default.AllInclusive, Color(0xFF00E5FF))
        lower.contains("speed") -> Pair(Icons.Default.Speed, Color(0xFFFFD54F))
        lower.contains("lyrics") -> Pair(Icons.Default.MusicNote, Color(0xFF64B5F6))
        lower.contains("cover") || lower.contains("artwork") -> Pair(Icons.Default.Image, Color(0xFFBA68C8))
        lower.contains("tag") -> Pair(Icons.AutoMirrored.Filled.Label, defaultColor)
        lower.contains("ringtone") || lower.contains("alarm") -> Pair(Icons.Default.NotificationsActive, Color(0xFFFFB74D))
        lower.contains("copy") || lower.contains("copied") -> Pair(Icons.Default.ContentCopy, Color(0xFF81C784))
        lower.contains("error") || lower.contains("fail") || lower.contains("unable") -> Pair(Icons.Default.Warning, Color(0xFFFF7043))
        else -> Pair(Icons.Default.CheckCircle, Color(0xFF81C784))
    }
}

