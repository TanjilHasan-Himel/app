package com.eplaytime.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.eplaytime.app.R
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.ui.theme.OutfitFontFamily
import com.eplaytime.app.ui.theme.SoftGold
import com.eplaytime.app.ui.theme.TextPrimary
import com.eplaytime.app.ui.theme.TextSecondary
import com.eplaytime.app.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * 4-Piece Liquid Glass MiniPlayer:
 * Piece 1: Left Arc `(|` (Previous track - pure glass surface)
 * Piece 2: Center Deck (Play/Pause + Rotating Vinyl + Song info + Liquid Progress Wave)
 * Piece 3: Right Arc `|)` (Next track - pure glass surface)
 * Piece 4: Queue Arc `(|` (Queue toggle - same arc direction with 3 gold hamburger bars)
 */
@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onSongClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    accentColor: Color = SoftGold,
    modifier: Modifier = Modifier,
    viewModel: MusicViewModel = hiltViewModel()
) {
    val haptic = LocalHapticFeedback.current
    val progress by viewModel.progress.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val progressFraction = if (duration > 0L) {
        (progress.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val upcomingSongs by viewModel.upcomingQueue.collectAsState()
    val songList by viewModel.songList.collectAsState()
    val resolvedQueue = remember(upcomingSongs, songList, song.uri) {
        if (upcomingSongs.isNotEmpty()) upcomingSongs
        else {
            val idx = songList.indexOfFirst { it.uri == song.uri }
            if (idx != -1 && idx + 1 < songList.size) {
                songList.subList(idx + 1, (idx + 7).coerceAtMost(songList.size))
            } else {
                songList.filter { it.uri != song.uri }.take(6)
            }
        }
    }

    var showQueue by remember { mutableStateOf(false) }

    BackHandler(enabled = showQueue) {
        showQueue = false
    }

    val goldAccent = remember(accentColor) {
        if (accentColor == Color.Unspecified || accentColor.alpha < 0.2f) SoftGold
        else {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(accentColor.toArgb(), hsv)
            if (hsv[2] < 0.65f) SoftGold else accentColor
        }
    }

    // Shapes (for height 74.dp, semicircle radius is 37.dp)
    val leftArcShape = remember {
        RoundedCornerShape(topStart = 37.dp, bottomStart = 37.dp, topEnd = 8.dp, bottomEnd = 8.dp)
    }
    val rightArcShape = remember {
        RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 37.dp, bottomEnd = 37.dp)
    }
    val centerDeckShape = remember { RoundedCornerShape(16.dp) }

    // Water Physics scales for all 4 pieces
    var p1Pressed by remember { mutableStateOf(false) }
    val p1Scale by animateFloatAsState(
        targetValue = if (p1Pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "miniPlayer_p1Scale"
    )

    var p2Pressed by remember { mutableStateOf(false) }
    val p2Scale by animateFloatAsState(
        targetValue = if (p2Pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "miniPlayer_p2Scale"
    )

    var p3Pressed by remember { mutableStateOf(false) }
    val p3Scale by animateFloatAsState(
        targetValue = if (p3Pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "miniPlayer_p3Scale"
    )

    var p4Pressed by remember { mutableStateOf(false) }
    val p4Scale by animateFloatAsState(
        targetValue = if (p4Pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "miniPlayer_p4Scale"
    )

    // Continuous Vinyl Rotation
    val vinylRotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                vinylRotation.animateTo(
                    targetValue = vinylRotation.value + 360f,
                    animationSpec = tween(durationMillis = 10000, easing = LinearEasing)
                )
            }
        }
    }

    // Single unified light beam travelling continuously across all 4 pieces
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_glass_fx")
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_phase"
    )

    val density = LocalDensity.current
    val beamWidthPx = remember(density) { with(density) { 260.dp.toPx() } }

    var rowOriginX by remember { mutableFloatStateOf(0f) }
    var rowWidthPx by remember { mutableFloatStateOf(1000f) }
    var piece1OffsetX by remember { mutableFloatStateOf(0f) }
    var piece2OffsetX by remember { mutableFloatStateOf(140f) }
    var piece3OffsetX by remember { mutableFloatStateOf(800f) }
    var piece4OffsetX by remember { mutableFloatStateOf(940f) }

    // Single unified beam center traveling smoothly across the entire width of all 4 pieces:
    val totalTravelDistance = rowWidthPx + 2 * beamWidthPx
    val globalBeamCenter = -beamWidthPx + shimmerPhase * totalTravelDistance

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Floating Queue Overlay above mini player
            AnimatedVisibility(
                visible = showQueue,
                enter = slideInVertically(tween(280, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200)),
                exit = slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(150))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .padding(bottom = 6.dp)
                        .pointerInput(Unit) {
                            var dragY = 0f
                            detectVerticalDragGestures(
                                onDragStart = { dragY = 0f },
                                onVerticalDrag = { change, amount ->
                                    change.consume()
                                    dragY += amount
                                },
                                onDragEnd = {
                                    if (dragY > 10f) showQueue = false
                                    dragY = 0f
                                },
                                onDragCancel = { dragY = 0f }
                            )
                        }
                        .shadow(16.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(0.6f))
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xD9141622), Color(0xB310121B))
                            )
                        )
                        .border(
                            1.2.dp,
                            Brush.verticalGradient(
                                listOf(Color.White.copy(0.60f), Color.White.copy(0.18f), Color.White.copy(0.06f))
                            ),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        // Drag handle
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                                .align(Alignment.CenterHorizontally)
                        )
                        Spacer(Modifier.height(8.dp))

                        // Header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "UPCOMING QUEUE",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = goldAccent,
                                    letterSpacing = 1.sp
                                )
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.1f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${resolvedQueue.size}",
                                        fontFamily = OutfitFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showQueue = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss queue",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        if (resolvedQueue.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No upcoming tracks in queue",
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(resolvedQueue.take(6), key = { it.uri }) { queueSong ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White.copy(alpha = 0.04f))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.playSong(queueSong.uri)
                                                showQueue = false
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = queueSong.albumArtUri ?: R.drawable.audia_player_logo,
                                            contentDescription = "${queueSong.title} artwork",
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF22242E)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = queueSong.title,
                                                fontFamily = OutfitFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = queueSong.artist,
                                                fontFamily = OutfitFontFamily,
                                                fontSize = 11.sp,
                                                color = TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4-Piece Liquid Glass Row (Unified Continuous Shimmer Sweep)
            val p1WidthPx = remember(density) { with(density) { 50.dp.toPx() } }
            var p2WidthPx by remember { mutableFloatStateOf(700f) }
            val p3WidthPx = remember(density) { with(density) { 50.dp.toPx() } }
            val p4WidthPx = remember(density) { with(density) { 50.dp.toPx() } }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .onGloballyPositioned { coords ->
                        rowOriginX = coords.positionInRoot().x
                        rowWidthPx = coords.size.width.toFloat()
                    },
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Piece 1 — Left Arc `(|` (Previous track)
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(74.dp)
                        .onGloballyPositioned { coords ->
                            piece1OffsetX = coords.positionInRoot().x - rowOriginX
                        }
                        .graphicsLayer {
                            scaleX = p1Scale
                            scaleY = p1Scale
                        }
                        .liquidGlassPiece(
                            shape = leftArcShape,
                            localBeamCenter = globalBeamCenter - piece1OffsetX,
                            beamWidthPx = beamWidthPx,
                            pieceWidthPx = p1WidthPx,
                            goldAccent = goldAccent
                        )
                        .semantics {
                            contentDescription = "Previous track"
                            role = Role.Button
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    p1Pressed = true
                                    tryAwaitRelease()
                                    p1Pressed = false
                                },
                                onTap = {
                                    if (showQueue) showQueue = false
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPreviousClick()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous track",
                        tint = goldAccent.copy(alpha = 0.95f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Piece 2 — Center Deck (Play/Pause + Dynamic Progress Fill + Vinyl + Info)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(74.dp)
                        .onGloballyPositioned { coords ->
                            piece2OffsetX = coords.positionInRoot().x - rowOriginX
                            p2WidthPx = coords.size.width.toFloat()
                        }
                        .graphicsLayer {
                            scaleX = p2Scale
                            scaleY = p2Scale
                        }
                        .liquidGlassPiece(
                            shape = centerDeckShape,
                            localBeamCenter = globalBeamCenter - piece2OffsetX,
                            beamWidthPx = beamWidthPx,
                            pieceWidthPx = p2WidthPx,
                            goldAccent = goldAccent
                        )
                        .semantics {
                            contentDescription = if (isPlaying) "Pause playback or swipe up to open player" else "Resume playback or swipe up to open player"
                            role = Role.Button
                        }
                        .pointerInput(Unit) {
                            var dragY = 0f
                            detectVerticalDragGestures(
                                onDragStart = { dragY = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragY += dragAmount
                                },
                                onDragEnd = {
                                    if (dragY < 0f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSongClick()
                                    }
                                    dragY = 0f
                                },
                                onDragCancel = { dragY = 0f }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    p2Pressed = true
                                    tryAwaitRelease()
                                    p2Pressed = false
                                },
                                onTap = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPlayPauseClick()
                                }
                            )
                        }
                ) {
                    // Dynamic Theme Progress Fill: Fills the entire Center Deck from left to right as the song plays!
                    if (progressFraction > 0f) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val fillWidth = size.width * progressFraction
                            // 1. Translucent liquid theme fill across the button body
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        goldAccent.copy(alpha = 0.22f),
                                        goldAccent.copy(alpha = 0.42f)
                                    ),
                                    startX = 0f,
                                    endX = fillWidth
                                ),
                                topLeft = Offset.Zero,
                                size = Size(fillWidth, size.height)
                            )
                            // 2. Glowing vertical crest beam at current progress position
                            drawLine(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.85f),
                                        goldAccent.copy(alpha = 0.95f),
                                        Color.White.copy(alpha = 0.65f)
                                    )
                                ),
                                start = Offset(fillWidth, 0f),
                                end = Offset(fillWidth, size.height),
                                strokeWidth = 2.dp.toPx()
                            )
                            // 3. Soft ambient glow halo at progress head
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(goldAccent.copy(alpha = 0.35f), Color.Transparent),
                                    center = Offset(fillWidth, size.height * 0.5f),
                                    radius = 24.dp.toPx()
                                ),
                                radius = 24.dp.toPx(),
                                center = Offset(fillWidth, size.height * 0.5f)
                            )
                        }
                    }

                    // Center content: rotating vinyl + title + artist
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 10.dp, end = 10.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rotating Vinyl Record
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .graphicsLayer { rotationZ = vinylRotation.value }
                                .clip(CircleShape)
                                .background(Color(0xFF141620))
                                .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = song.albumArtUri ?: R.drawable.audia_player_logo,
                                contentDescription = "${song.title} artwork",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            // Vinyl center spindle hole
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xE5141620))
                                    .border(1.dp, goldAccent.copy(alpha = 0.75f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(3.5.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.85f))
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        // Song Title and Artist (Clean text, no play/pause icon)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = song.title,
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = song.artist,
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.5.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Piece 3 — Right Arc `|)` (Next track)
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(74.dp)
                        .onGloballyPositioned { coords ->
                            piece3OffsetX = coords.positionInRoot().x - rowOriginX
                        }
                        .graphicsLayer {
                            scaleX = p3Scale
                            scaleY = p3Scale
                        }
                        .liquidGlassPiece(
                            shape = rightArcShape,
                            localBeamCenter = globalBeamCenter - piece3OffsetX,
                            beamWidthPx = beamWidthPx,
                            pieceWidthPx = p3WidthPx,
                            goldAccent = goldAccent
                        )
                        .semantics {
                            contentDescription = "Next track"
                            role = Role.Button
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    p3Pressed = true
                                    tryAwaitRelease()
                                    p3Pressed = false
                                },
                                onTap = {
                                    if (showQueue) showQueue = false
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onNextClick()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next track",
                        tint = goldAccent.copy(alpha = 0.95f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Piece 4 — Queue Arc `(|` (Queue button - same curve direction as Left Arc)
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(74.dp)
                        .onGloballyPositioned { coords ->
                            piece4OffsetX = coords.positionInRoot().x - rowOriginX
                        }
                        .graphicsLayer {
                            scaleX = p4Scale
                            scaleY = p4Scale
                        }
                        .liquidGlassPiece(
                            shape = leftArcShape,
                            localBeamCenter = globalBeamCenter - piece4OffsetX,
                            beamWidthPx = beamWidthPx,
                            pieceWidthPx = p4WidthPx,
                            goldAccent = goldAccent
                        )
                        .semantics {
                            contentDescription = "Queue"
                            role = Role.Button
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    p4Pressed = true
                                    tryAwaitRelease()
                                    p4Pressed = false
                                },
                                onTap = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showQueue = !showQueue
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // 3 horizontal gold lines (= hamburger) centered, drawn via Canvas
                    Canvas(modifier = Modifier.size(18.dp, 13.dp)) {
                        val lineWidth = size.width
                        val strokePx = 2.dp.toPx()
                        val lineSpacing = (size.height - 3 * strokePx) / 2f
                        for (i in 0..2) {
                            val lineY = i * (strokePx + lineSpacing) + strokePx / 2f
                            drawLine(
                                color = goldAccent,
                                start = Offset(0f, lineY),
                                end = Offset(lineWidth, lineY),
                                strokeWidth = strokePx,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Common frosted liquid glass material applied to all 4 mini player pieces:
 * - Translucent glass base (40-60% opacity) so underlying song items shine through
 * - Unified Continuous Shimmer: Single light beam traveling smoothly across all pieces
 * - Dynamic Local Flare: Shimmer border and glow flare up when the unified beam passes over this piece
 * - Fluid Specular Liquid Sheen Sweep across the glass
 */
private fun Modifier.liquidGlassPiece(
    shape: Shape,
    localBeamCenter: Float,
    beamWidthPx: Float,
    pieceWidthPx: Float,
    goldAccent: Color
): Modifier {
    val distanceToCenter = abs(localBeamCenter - (pieceWidthPx * 0.5f))
    val flare = (1f - (distanceToCenter / (beamWidthPx * 0.75f))).coerceIn(0f, 1f)
    val glowAlpha = 0.18f + 0.52f * flare

    return this
        .shadow(
            elevation = 12.dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.55f),
            spotColor = goldAccent.copy(alpha = glowAlpha)
        )
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x99181B28), // 60% translucent dark sapphire-glass at top
                    Color(0x6610121C)  // 40% translucent deep glass at bottom
                )
            ),
            shape = shape
        )
        .border(
            width = 1.2.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.18f),
                    goldAccent.copy(alpha = 0.35f),
                    goldAccent.copy(alpha = 0.95f),
                    Color.White.copy(alpha = 0.95f),
                    goldAccent.copy(alpha = 0.95f),
                    goldAccent.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.18f)
                ),
                start = Offset(localBeamCenter - beamWidthPx * 0.5f, 0f),
                end = Offset(localBeamCenter + beamWidthPx * 0.5f, 180f)
            ),
            shape = shape
        )
        .drawWithCache {
            // Fluid Specular Liquid Sheen Sweep across the glass body
            val glossSheen = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.05f),
                    goldAccent.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.18f),
                    goldAccent.copy(alpha = 0.16f),
                    Color.Transparent
                ),
                start = Offset(localBeamCenter - beamWidthPx * 0.4f, 0f),
                end = Offset(localBeamCenter + beamWidthPx * 0.4f, size.height)
            )
            onDrawWithContent {
                drawContent()
                drawRect(glossSheen)
            }
        }
}
