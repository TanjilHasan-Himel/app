package com.eplaytime.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.eplaytime.app.ui.theme.SoftGold
import com.eplaytime.app.ui.theme.SoftGoldDark
import com.eplaytime.app.ui.theme.TextTertiary

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.Spring

/**
 * Rich Player Controls with Haptics and Animations
 */
@Composable
fun PlayerControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous
        val prevInteractionSource = remember { MutableInteractionSource() }
        val prevIsPressed by prevInteractionSource.collectIsPressedAsState()
        val prevScale by animateFloatAsState(
            targetValue = if (prevIsPressed) 0.75f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
        IconButton(
            onClick = { 
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPrevious() 
            },
            interactionSource = prevInteractionSource,
            modifier = Modifier.size(56.dp).scale(prevScale)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        // Rewind 5s
        val rwInteractionSource = remember { MutableInteractionSource() }
        val rwIsPressed by rwInteractionSource.collectIsPressedAsState()
        val rwScale by animateFloatAsState(
            targetValue = if (rwIsPressed) 0.75f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSeekBackward()
            },
            interactionSource = rwInteractionSource,
            modifier = Modifier.size(40.dp).scale(rwScale)
        ) {
            Icon(
                imageVector = Icons.Default.Replay5,
                contentDescription = "Rewind 5s",
                tint = TextTertiary,
                modifier = Modifier.size(28.dp)
            )
        }

        // Play/Pause (Large, Round, Soft edges)
        val playInteractionSource = remember { MutableInteractionSource() }
        val playIsPressed by playInteractionSource.collectIsPressedAsState()
        val playScale by animateFloatAsState(
            targetValue = if (playIsPressed) 0.85f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(playScale)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(SoftGold, SoftGoldDark)
                    )
                )
                .clickable(
                    interactionSource = playInteractionSource,
                    indication = androidx.compose.foundation.LocalIndication.current
                ) { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPlayPause() 
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause",
                tint = Color.Black,
                modifier = Modifier.size(40.dp)
            )
        }

        // Forward 5s
        val ffInteractionSource = remember { MutableInteractionSource() }
        val ffIsPressed by ffInteractionSource.collectIsPressedAsState()
        val ffScale by animateFloatAsState(
            targetValue = if (ffIsPressed) 0.75f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSeekForward()
            },
            interactionSource = ffInteractionSource,
            modifier = Modifier.size(40.dp).scale(ffScale)
        ) {
            Icon(
                imageVector = Icons.Default.Forward5,
                contentDescription = "Forward 5s",
                tint = TextTertiary,
                modifier = Modifier.size(28.dp)
            )
        }

        // Next
        val nextInteractionSource = remember { MutableInteractionSource() }
        val nextIsPressed by nextInteractionSource.collectIsPressedAsState()
        val nextScale by animateFloatAsState(
            targetValue = if (nextIsPressed) 0.75f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
        IconButton(
            onClick = { 
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onNext() 
            },
            interactionSource = nextInteractionSource,
            modifier = Modifier.size(56.dp).scale(nextScale)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    val scale by animateFloatAsState(
        targetValue = if (isFavorite) 1.3f else 1.0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 600f), // Bouncy spring
        label = "heart_bounce"
    )

    val color by animateColorAsState(
        targetValue = if (isFavorite) Color.Red else TextTertiary,
        label = "heart_color"
    )

    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggle()
        },
        modifier = modifier.scale(if (isFavorite) scale else 1.0f) // Only scale up on favorite
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Favorite",
            tint = color
        )
    }
}
