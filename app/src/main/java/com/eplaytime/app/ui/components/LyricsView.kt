package com.eplaytime.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eplaytime.app.data.model.ParsedLyrics
import com.eplaytime.app.ui.theme.OutfitFontFamily
import com.eplaytime.app.ui.theme.SoftGold
import com.eplaytime.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LyricsView(
    lyrics: ParsedLyrics?,
    isLoading: Boolean,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    onRetry: () -> Unit = {},
    accentColor: Color = SoftGold,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = SoftGold,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "Syncing lyrics...",
                    fontFamily = OutfitFontFamily,
                    fontSize = 15.sp,
                    color = TextSecondary
                )
            }
        }
        return
    }

    if (lyrics == null || lyrics.lines.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(SoftGold.copy(alpha = 0.12f))
                        .border(1.dp, SoftGold.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = SoftGold,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = "No Synced Lyrics Yet",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = Color.White.copy(alpha = 0.95f)
                )

                Text(
                    text = "Audia automatically searches and syncs lyrics from LRCLIB when online.",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(SoftGold.copy(alpha = 0.15f))
                        .border(1.dp, SoftGold.copy(alpha = 0.40f), RoundedCornerShape(22.dp))
                        .clickable { onRetry() }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Search Online",
                            tint = SoftGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Search Online (LRCLIB)",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = SoftGold
                        )
                    }
                }
            }
        }
        return
    }

    val lines = lyrics.lines
    val isSynced = lyrics.isSynced
    val scope = rememberCoroutineScope()

    // Find active lyric index (returns -1 during intro prelude)
    val activeIndex = remember(currentPositionMs, lines, isSynced) {
        if (!isSynced) -1
        else com.eplaytime.app.util.LrcParser.findActiveLyricIndex(lines, currentPositionMs)
    }

    val listState = rememberLazyListState()
    val isUserScrolling by listState.interactionSource.collectIsDraggedAsState()
    var userScrollPauseUntil by remember { mutableLongStateOf(0L) }
    var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            currentTimeMs = System.currentTimeMillis()
        }
    }

    LaunchedEffect(isUserScrolling) {
        if (isUserScrolling) {
            userScrollPauseUntil = System.currentTimeMillis() + 4000L
        }
    }

    val isPausedByUser = isSynced && (currentTimeMs < userScrollPauseUntil)

    // Smooth butter auto-scroll to center active line
    LaunchedEffect(activeIndex, userScrollPauseUntil) {
        if (activeIndex >= 0 && activeIndex < lines.size) {
            val now = System.currentTimeMillis()
            if (now >= userScrollPauseUntil) {
                delay(20)
                listState.animateScrollToItem(
                    index = activeIndex,
                    scrollOffset = 0
                )
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val availableHeight = maxHeight
        // Calculate adaptive vertical padding so active line lands exactly in center
        val centerPadding = if (isSynced) {
            (availableHeight / 2 - 28.dp).coerceAtLeast(16.dp)
        } else {
            16.dp
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.08f to Color.Black,
                            0.92f to Color.Black,
                            1.0f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                },
            contentPadding = PaddingValues(
                top = centerPadding,
                bottom = if (isSynced) centerPadding else 48.dp,
                start = 20.dp,
                end = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isSynced) {
                item(key = "unsynced_header") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "• Plain Lyrics •",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = SoftGold.copy(alpha = 0.6f)
                        )
                    }
                }
            } else if (activeIndex == -1 && lines.isNotEmpty()) {
                item(key = "instrumental_intro") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "♪ • • Instrumental Intro • • ♪",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp,
                            color = SoftGold.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            itemsIndexed(
                items = lines,
                key = { index, line -> "${line.timestampMs}_$index" }
            ) { index, line ->
                val isActive = isSynced && index == activeIndex

                val textColor by animateColorAsState(
                    targetValue = if (isActive) accentColor else Color.White.copy(alpha = if (isSynced) 0.35f else 0.85f),
                    animationSpec = tween(durationMillis = 280),
                    label = "textColor"
                )

                val lineScale by animateFloatAsState(
                    targetValue = if (isActive) 1.04f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "lineScale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(lineScale)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = isSynced) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            userScrollPauseUntil = 0L
                            onSeekTo(line.timestampMs)
                        }
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                ) {
                    Text(
                        text = line.content,
                        fontFamily = OutfitFontFamily,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        fontSize = if (isActive) 22.sp else 18.sp,
                        lineHeight = if (isActive) 30.sp else 26.sp,
                        color = textColor,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }

        // Return to active lyric pill button when user manually scrolls
        if (isPausedByUser && activeIndex >= 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .border(1.dp, SoftGold.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .clickable {
                            userScrollPauseUntil = 0L
                            scope.launch {
                                listState.animateScrollToItem(activeIndex, 0)
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Resume Auto-Scroll ⤓",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = SoftGold
                    )
                }
            }
        }
    }
}

