package com.eplaytime.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eplaytime.app.R
import com.eplaytime.app.ui.theme.OutfitFontFamily
import com.eplaytime.app.ui.theme.SoftGold
import kotlinx.coroutines.delay

/**
 * SplashScreen - Logo Opening with Soft Spring Animation
 * Premium entrance experience for Audia Player
 */
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(true) }

    // Scale animation with Soft Spring effect
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    // Alpha animation for fade in
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "alpha"
    )

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Fade out animation
    val fadeOut by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = EaseOutCubic),
        label = "fadeOut"
    )

    // Trigger animations
    LaunchedEffect(Unit) {
        startAnimation = true
        delay(800) // Fast Launch: 0.8 seconds
        showContent = false
        delay(200) // Quick fade out
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .alpha(fadeOut),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo with scale and pulse
            // Logo with scale and pulse
            Image(
                painter = painterResource(id = R.drawable.audia_player),
                contentDescription = "Audia Player Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(180.dp) // Slightly larger
                    .scale(scale * pulse)
                    .alpha(alpha)
                    .clip(CircleShape) // Circular Clip
                    .border(2.dp, SoftGold.copy(alpha = 0.5f), CircleShape) // Optional: Gold border for premium feel
            )

            Spacer(modifier = Modifier.height(24.dp))

            // App Name
            Text(
                text = "Audia Player",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 42.sp, // Larger text
                color = Color.White,
                modifier = Modifier.alpha(alpha)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Your Music, Your Schedule",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = 14.sp,
                color = SoftGold.copy(alpha = 0.8f),
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}
