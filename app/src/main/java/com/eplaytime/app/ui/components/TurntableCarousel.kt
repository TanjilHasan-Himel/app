package com.eplaytime.app.ui.components

import android.animation.ValueAnimator
import android.os.Build
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

/**
 * Endless native cover ring adapted from Bencho's MIT-licensed Carousel geometry.
 * Position remains unbounded so cards never jump at the 0/360 seam.
 */
@Composable
fun TurntableCarousel(
    songs: List<Song>,
    currentSongId: Long?,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val displaySongs = remember(songs) { songs.distinctBy { it.id }.take(10) }
    if (displaySongs.isEmpty()) return

    val initialTurn = remember(displaySongs, currentSongId) {
        displaySongs.indexOfFirst { it.id == currentSongId }.coerceAtLeast(0).toFloat()
    }
    val turn = remember(displaySongs) { mutableFloatStateOf(initialTurn) }
    val scope = rememberCoroutineScope()
    var settleJob: Job? by remember { mutableStateOf(null) }
    val density = LocalDensity.current
    val motionEnabled = remember {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()
    }
    val pullPx = with(density) { 112.dp.toPx() }
    var lastDragAt = remember { 0L }
    var dragVelocity = remember { 0f }

    fun settleTo(target: Float) {
        settleJob?.cancel()
        settleJob = scope.launch {
            if (!motionEnabled) {
                turn.floatValue = target
                return@launch
            }
            Animatable(turn.floatValue).animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = 0.78f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) { turn.floatValue = value }
        }
    }

    fun moveByCards(delta: Int) = settleTo(round(turn.floatValue) + delta)

    fun nearestTurnFor(index: Int): Float {
        val count = displaySongs.size
        val current = round(turn.floatValue).toInt()
        val front = current.floorMod(count)
        var delta = index - front
        if (delta > count / 2) delta -= count
        if (delta < -count / 2) delta += count
        return (current + delta).toFloat()
    }

    LaunchedEffect(currentSongId, displaySongs) {
        val index = displaySongs.indexOfFirst { it.id == currentSongId }
        if (index >= 0 && index != round(turn.floatValue).toInt().floorMod(displaySongs.size)) {
            settleTo(nearestTurnFor(index))
        }
    }

    val front = round(turn.floatValue).toInt().floorMod(displaySongs.size)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(316.dp)
            .pointerInput(displaySongs, motionEnabled) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        settleJob?.cancel()
                        lastDragAt = SystemClock.uptimeMillis()
                        dragVelocity = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val now = SystemClock.uptimeMillis()
                        val elapsed = (now - lastDragAt).coerceAtLeast(1L)
                        val measured = dragAmount / elapsed
                        dragVelocity = dragVelocity * 0.65f + measured * 0.35f
                        lastDragAt = now
                        turn.floatValue -= dragAmount / pullPx
                    },
                    onDragEnd = {
                        val projectedCards = ((-dragVelocity * 1000f / pullPx) * 0.20f)
                            .coerceIn(-3f, 3f)
                        settleTo(round(turn.floatValue + projectedCards))
                    },
                    onDragCancel = { settleTo(round(turn.floatValue)) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient Stage Lighting behind the active center card (Illuminates the dark background void)
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SoftGold.copy(alpha = 0.22f),
                            Color(0xFF38BDF8).copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 2. Subtle Ground Stage Floor Reflection
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
                .width(280.dp)
                .height(32.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SoftGold.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )

        CoverRing(
            songs = displaySongs,
            turn = { turn.floatValue },
            currentSongId = currentSongId,
            motionEnabled = motionEnabled,
            onCardClick = { index, song ->
                if (index == front) onSongClick(song) else settleTo(nearestTurnFor(index))
            },
            modifier = Modifier.fillMaxSize()
        )

        // 3. Frosted Glass Left Arrow
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 6.dp)
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xB312151D))
                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                .clickable { moveByCards(-1) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous cover",
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(20.dp)
            )
        }

        // 4. Frosted Glass Right Arrow
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp)
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xB312151D))
                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                .clickable { moveByCards(1) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next cover",
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(20.dp)
            )
        }

        // 5. Sleek Pagination Pill Dots at the bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            displaySongs.indices.forEach { idx ->
                val isSelected = idx == front
                Box(
                    modifier = Modifier
                        .height(3.5.dp)
                        .width(if (isSelected) 18.dp else 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isSelected) SoftGold else Color.White.copy(alpha = 0.22f)
                        )
                )
            }
        }
    }
}

@Composable
private fun CoverRing(
    songs: List<Song>,
    turn: () -> Float,
    currentSongId: Long?,
    motionEnabled: Boolean,
    onCardClick: (Int, Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val cardWidth = with(density) { 205.dp.roundToPx() }
    val cardHeight = with(density) { 272.dp.roundToPx() }
    val verticalDepth = with(density) { 20.dp.toPx() }
    val corner = RoundedCornerShape(20.dp)

    Layout(
        modifier = modifier,
        content = {
            songs.forEachIndexed { index, song ->
                val isCurrent = song.id == currentSongId
                Box(
                    Modifier
                        .clip(corner)
                        .background(BlackSurface)
                        .then(
                            if (isCurrent) Modifier.border(
                                2.dp,
                                Brush.verticalGradient(
                                    listOf(SoftGold, SoftGold.copy(alpha = 0.28f))
                                ),
                                corner
                            ) else Modifier.border(
                                1.dp,
                                Color.White.copy(alpha = 0.10f),
                                corner
                            )
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "${song.title}, ${song.artist}"
                        }
                        .clickable { onCardClick(index, song) }
                ) {
                    AsyncImage(
                        model = song.albumArtUri?.takeIf(String::isNotBlank) ?: song.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.11f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.88f)
                                )
                            )
                        )
                    )
                    Column(
                        Modifier.fillMaxWidth().align(Alignment.BottomStart)
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = song.title,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isCurrent) SoftGold else Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            fontFamily = OutfitFontFamily,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val safeCardWidth = cardWidth.coerceAtMost((width * 0.62f).toInt())
        val safeCardHeight = cardHeight.coerceAtMost(height - 12.dp.roundToPx())
        val placeables = measurables.map {
            it.measure(Constraints.fixed(safeCardWidth, safeCardHeight))
        }
        val orbit = (width * 0.36f).coerceAtLeast(safeCardWidth * 0.65f)
        val step = (2.0 * PI / songs.size).toFloat()

        layout(width, height) {
            val liveTurn = turn()
            val spots = songs.indices.map { index ->
                val angle = (index - liveTurn) * step
                val depth = ((cos(angle) + 1f) / 2f).coerceIn(0f, 1f)
                RingSpot(index, angle, depth)
            }.sortedBy { it.depth }

            spots.forEach { spot ->
                val placeable = placeables[spot.index]
                val depth = spot.depth
                val side = sin(spot.angle)
                val scale = 0.66f + depth * 0.34f
                placeable.placeWithLayer(
                    x = (width - placeable.width) / 2,
                    y = (height - placeable.height) / 2,
                    zIndex = depth
                ) {
                    translationX = side * orbit
                    translationY = (1f - depth) * verticalDepth
                    scaleX = scale
                    scaleY = scale
                    alpha = 0.46f + depth * 0.54f
                    if (motionEnabled) {
                        rotationY = -side * 50f
                        rotationZ = side * 2.5f
                        transformOrigin = TransformOrigin.Center
                        cameraDistance = 18f * density.density
                    }
                }
            }
        }
    }
}

private data class RingSpot(val index: Int, val angle: Float, val depth: Float)

private fun Int.floorMod(divisor: Int): Int = ((this % divisor) + divisor) % divisor
