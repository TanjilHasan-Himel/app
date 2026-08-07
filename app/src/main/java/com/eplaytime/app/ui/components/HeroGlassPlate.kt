package com.eplaytime.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eplaytime.app.R
import com.eplaytime.app.ui.theme.GaladaFontFamily
import com.eplaytime.app.ui.theme.OutfitFontFamily
import kotlin.math.roundToInt

private val BrandGlowColor = Color(0xFFFF5A5F)
private val DarkBackground = Color(0xFF0A0A12)

/**
 * The "Hero Glass Plate" - A cinematic cold start overlay.
 * Covers top 60% of screen. Content is Neumorphic Logo + Dynamic Greeting.
 * Swipe UP to dismiss.
 */
@Composable
fun HeroGlassPlate(
    userName: String?,
    greeting: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val threshold = with(density) { 100.dp.toPx() } // Swipe threshold

    // Infinite pulse animation for the Chevron
    val infiniteTransition = rememberInfiniteTransition(label = "chevron_pulse")
    val chevronAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val chevronOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f) // Covers top 60%
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    // Only allow dragging UP (negative delta)
                    if (delta < 0) {
                        offsetY += delta
                    }
                },
                onDragStopped = {
                    if (offsetY < -threshold) {
                        onDismiss() // Trigger full exit animation in parent
                    } else {
                        offsetY = 0f // Snap back if swipe wasn't enough
                    }
                }
            )
            .background(DarkBackground)
            // Frosted Glass Glow Effect
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        BrandGlowColor.copy(alpha = 0.4f), // Center glow
                        BrandGlowColor.copy(alpha = 0.1f),
                        Color.Transparent // Fade to dark
                    ),
                    center = Offset.Unspecified,
                    radius = 500f
                )
            )
    ) {
        // Content Row (Centered)
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Neumorphic Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = BrandGlowColor,
                        ambientColor = BrandGlowColor
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.05f)) // Glassy container
            ) {
                Image(
                    painter = painterResource(id = R.drawable.audia_player),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Text Column
            Column {
                Text(
                    text = greeting.split(",")[0], // e.g. "Up late?" or "Good Morning"
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = userName ?: "Audia User",
                    fontFamily = GaladaFontFamily, // The Rich Bengali Font
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = Color.White,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = Shadow(
                            color = BrandGlowColor.copy(alpha = 0.6f),
                            blurRadius = 12f
                        )
                    )
                )
            }
        }

        // Bottom Cue (Chevron)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .offset(y = chevronOffset.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SWIPE UP",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                color = BrandGlowColor.copy(alpha = chevronAlpha)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = BrandGlowColor.copy(alpha = chevronAlpha),
                modifier = Modifier.size(24.dp)
            )
        }

        // Refinement 1: Blended Bottom Edge
        // Fades seamlessly merging into darkness
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.25f) // Bottom 25%
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            DarkBackground
                        )
                    )
                )
        )
    }
}
