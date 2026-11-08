package com.eplaytime.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.*

/**
 * 3D Turntable Ring Carousel
 * Inspired by Bencho's elastic turntable architecture.
 *
 * Cards are arranged along a 3D elliptical orbit:
 * - Back cards recede, scale down, and lean upward
 * - Front card advances forward with high-res album art and soft specular sheen
 * - Continuous infinite ring rotation with smooth inertial drag and spring snap
 * - Independent idle floating drift per card
 */
@Composable
fun TurntableCarousel(
    songs: List<Song>,
    currentSongId: Long?,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return

    val displaySongs = remember(songs) {
        if (songs.size in 1..2) {
            // Repeat to make at least 4-5 cards for a continuous 3D ring
            (songs + songs + songs).take(5)
        } else {
            songs.take(15)
        }
    }

    val count = displaySongs.size
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Ring Geometry parameters: wide panoramic orbit that fills the screen gracefully
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val stageHeight = 265.dp
    val cardWidth = 165.dp
    val cardHeight = 220.dp

    val orbitPx = with(density) { (screenWidth * 0.40f).toPx() }
    val leanPx = with(density) { 30.dp.toPx() }
    val pullPx = with(density) { 140.dp.toPx() }

    // Continuous turn position (0 = first card at front)
    val turnAnim = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Idle floating drift clock
    val infiniteTransition = rememberInfiniteTransition(label = "carousel_drift")
    val idlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift_phase"
    )

    // Precomputed resting angles to avoid artificial symmetry
    val restingAngles = remember { listOf(-3.5f, 2.8f, -1.8f, 3.2f, -2.4f, 2.1f, -1.2f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(stageHeight)
            .pointerInput(count) {
                val velocityTracker = VelocityTracker()
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        scope.launch { turnAnim.stop() }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        val deltaTurn = -dragAmount.x / pullPx
                        scope.launch {
                            turnAnim.snapTo(turnAnim.value + deltaTurn)
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        val velocityX = velocityTracker.calculateVelocity().x
                        val flingCarry = (-velocityX * 0.15f / pullPx).coerceIn(-2f, 2f)
                        val targetTurn = round(turnAnim.value + flingCarry)
                        scope.launch {
                            turnAnim.animateTo(
                                targetValue = targetTurn,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        val targetTurn = round(turnAnim.value)
                        scope.launch {
                            turnAnim.animateTo(
                                targetValue = targetTurn,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val currentTurn = turnAnim.value

        displaySongs.forEachIndexed { index, song ->
            // Angular position on the turntable
            val theta = (index - currentTurn) * ((2.0 * Math.PI) / count)
            val f = ((cos(theta) + 1.0) / 2.0).toFloat().coerceIn(0f, 1f)

            // Orbit X & Lean Y
            val xOffset = (sin(theta) * orbitPx).toFloat()
            val yOffset = -((1f - f) * leanPx)

            // Scale: back cards shrink to 0.62x, front card is 1.0x
            val scale = 0.62f + (f * 0.38f)

            // Idle floating drift
            val cardPeriodOffset = index * 1.1f
            val driftY = if (!isDragging) sin(idlePhase + cardPeriodOffset) * with(density) { 5.dp.toPx() } else 0f
            val driftRot = if (!isDragging) cos(idlePhase + cardPeriodOffset) * 1.2f else 0f

            val baseAngle = restingAngles[index % restingAngles.size]
            val totalRotation = baseAngle + driftRot

            val isCurrent = song.id == currentSongId
            val isFront = f > 0.85f

            Box(
                modifier = Modifier
                    .zIndex(f)
                    .graphicsLayer {
                        translationX = xOffset
                        translationY = yOffset + driftY
                        scaleX = scale
                        scaleY = scale
                        rotationZ = totalRotation
                        cameraDistance = 12f * density.density
                    }
                    .size(cardWidth, cardHeight)
                    .shadow(
                        elevation = (f * 14f).dp,
                        shape = RoundedCornerShape(18.dp),
                        spotColor = Color.Black.copy(alpha = 0.8f),
                        ambientColor = Color.Black.copy(alpha = 0.4f)
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(BlackSurface)
                    .then(
                        if (isCurrent) {
                            Modifier.border(
                                width = 2.dp,
                                brush = Brush.verticalGradient(listOf(SoftGold, SoftGold.copy(alpha = 0.3f))),
                                shape = RoundedCornerShape(18.dp)
                            )
                        } else if (isFront) {
                            Modifier.border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f))),
                                shape = RoundedCornerShape(18.dp)
                            )
                        } else {
                            Modifier // No top-edge border on background cards to eliminate the rear shelf/gray line artifact
                        }
                    )
                    .clickable {
                        if (isFront) {
                            onSongClick(song)
                        } else {
                            // Rotate this card to the front
                            val stepDiff = ((index - (currentTurn % count)) + count) % count
                            val shortestStep = if (stepDiff > count / 2) stepDiff - count else stepDiff
                            val targetTurn = currentTurn + shortestStep
                            scope.launch {
                                turnAnim.animateTo(
                                    targetValue = targetTurn,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                    }
            ) {
                // Album Art
                AsyncImage(
                    model = if (!song.albumArtUri.isNullOrBlank()) song.albumArtUri else song.uri,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Specular Glass Sheen Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.12f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                // Song Info on Card Bottom
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = song.title,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isCurrent) SoftGold else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

