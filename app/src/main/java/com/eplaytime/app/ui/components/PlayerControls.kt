package com.eplaytime.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.eplaytime.app.R

/**
 * Modern Clean Primary Player Controls (The Sacred 1 Row)
 * [Shuffle]  [Previous]  [Play/Pause (68dp circle)]  [Next]  [Repeat]
 *
 * Spotify & SimpMusic architecture: 5 clean buttons, zero squishing, dynamic accent glow.
 */
@Composable
fun PlayerControls(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    accentColor: Color,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Shuffle
        val shuffleInteraction = remember { MutableInteractionSource() }
        val shufflePressed by shuffleInteraction.collectIsPressedAsState()
        val shuffleScale by animateFloatAsState(
            targetValue = if (shufflePressed) 0.8f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "shuffle_scale"
        )
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggleShuffle()
            },
            interactionSource = shuffleInteraction,
            modifier = Modifier.size(44.dp).scale(shuffleScale)
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (shuffleEnabled) accentColor else Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(24.dp)
            )
        }

        // 2. Previous Track
        val prevInteraction = remember { MutableInteractionSource() }
        val prevPressed by prevInteraction.collectIsPressedAsState()
        val prevScale by animateFloatAsState(
            targetValue = if (prevPressed) 0.8f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "prev_scale"
        )
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPrevious()
            },
            interactionSource = prevInteraction,
            modifier = Modifier.size(52.dp).scale(prevScale)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        // 3. Play / Pause (The Hero Button: 68dp Perfect Circle, Elevated, High-Contrast)
        val playInteraction = remember { MutableInteractionSource() }
        val playPressed by playInteraction.collectIsPressedAsState()
        val playScale by animateFloatAsState(
            targetValue = if (playPressed) 0.88f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "play_scale"
        )
        Box(
            modifier = Modifier
                .size(68.dp)
                .scale(playScale)
                .shadow(16.dp, CircleShape, spotColor = accentColor.copy(alpha = 0.55f))
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor,
                            accentColor.copy(alpha = 0.90f)
                        )
                    )
                )
                .clickable(
                    interactionSource = playInteraction,
                    indication = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPlayPause()
                },
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause",
                    tint = Color(0xFF0F0F10), // Maximum contrast on bright accent!
                    modifier = Modifier.size(34.dp)
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.icon),
                    contentDescription = "Play",
                    tint = Color(0xFF0F0F10),
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        // 4. Next Track
        val nextInteraction = remember { MutableInteractionSource() }
        val nextPressed by nextInteraction.collectIsPressedAsState()
        val nextScale by animateFloatAsState(
            targetValue = if (nextPressed) 0.8f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "next_scale"
        )
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onNext()
            },
            interactionSource = nextInteraction,
            modifier = Modifier.size(52.dp).scale(nextScale)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        // 5. Repeat Mode
        val repeatInteraction = remember { MutableInteractionSource() }
        val repeatPressed by repeatInteraction.collectIsPressedAsState()
        val repeatScale by animateFloatAsState(
            targetValue = if (repeatPressed) 0.8f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "repeat_scale"
        )
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCycleRepeatMode()
            },
            interactionSource = repeatInteraction,
            modifier = Modifier.size(44.dp).scale(repeatScale)
        ) {
            val repeatIcon = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
            val isRepeatActive = repeatMode != Player.REPEAT_MODE_OFF
            Icon(
                imageVector = repeatIcon,
                contentDescription = "Repeat",
                tint = if (isRepeatActive) accentColor else Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Spotify-Style Heart / Favorite Button
 */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isFavorite) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 600f),
        label = "heart_bounce"
    )

    val color by animateColorAsState(
        targetValue = if (isFavorite) Color(0xFFFF2D55) else Color.White.copy(alpha = 0.60f),
        label = "heart_color"
    )

    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggle()
        },
        modifier = modifier.scale(if (isFavorite) scale else 1.0f)
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Favorite",
            tint = color,
            modifier = Modifier.size(26.dp)
        )
    }
}
