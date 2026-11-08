package com.eplaytime.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Modern tactile spring bounce click modifier
 * Gives buttons a juicy, realistic physics-based press scale (0.88x) and spring rebound
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bounceClick(
    scaleDown: Float = 0.88f,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounceScale"
    )

    this
        .scale(animatedScale)
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null, // Custom spring animation replaces generic ripple
            enabled = enabled,
            onLongClick = onLongClick?.let { action ->
                {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    action()
                }
            },
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        )
}

/**
 * Tactile spring bounce icon button
 */
@Composable
fun BounceIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    scaleDown: Float = 0.82f,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .bounceClick(scaleDown = scaleDown, enabled = enabled, onLongClick = onLongClick, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}


