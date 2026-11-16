package com.eplaytime.app.ui.screens

import com.eplaytime.app.data.model.Song
import com.eplaytime.app.data.model.ParsedLyrics
import com.eplaytime.app.ui.components.MarqueeTitleText

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import com.eplaytime.app.ui.components.AmbientGlowVisualizer
import com.eplaytime.app.ui.components.LoopPresetsBottomSheet
import androidx.compose.ui.graphics.toArgb
import com.eplaytime.app.ui.components.LyricsView
import com.eplaytime.app.ui.components.SongGarageBottomSheet
import com.eplaytime.app.ui.components.SongWikiBottomSheet
import com.eplaytime.app.ui.components.BounceIconButton
import com.eplaytime.app.ui.components.bounceClick
import com.eplaytime.app.util.LrcParser
import com.eplaytime.app.util.FirebaseDiagnostics
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.eplaytime.app.ui.components.AudioEffectsSheet
import com.eplaytime.app.ui.components.PlayerControls
import com.eplaytime.app.ui.components.FavoriteButton
import com.eplaytime.app.util.VisualizerHelper
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.eplaytime.app.R
import com.eplaytime.app.ui.theme.*
import com.eplaytime.app.ui.viewmodel.MusicViewModel
import java.util.*
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQueue: () -> Unit = {},
    viewModel: MusicViewModel
) {
    DisposableEffect(viewModel) {
        viewModel.setVisualizerUiVisible(true)
        onDispose { viewModel.setVisualizerUiVisible(false) }
    }
    val haptic = LocalHapticFeedback.current
    val currentSong by viewModel.currentSong.collectAsState()
    val songList by viewModel.songList.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val loopStart by viewModel.loopStart.collectAsState()
    val loopEnd by viewModel.loopEnd.collectAsState()
    val loopEnabled by viewModel.loopEnabled.collectAsState()
    val allFavorites by viewModel.allFavorites.collectAsState(initial = emptyList())
    val audioSessionId by viewModel.audioSessionId.collectAsState()

    val visualizerStyle by viewModel.visualizerStyle.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val stopAfterCurrent by viewModel.stopAfterCurrent.collectAsState()
    val fadeOnPlayPause by viewModel.fadeOnPlayPause.collectAsState()
    val sleepTimerRemainingMs by viewModel.sleepTimerRemainingMs.collectAsState()
    val loopPresets by viewModel.loopPresetsForCurrentSong.collectAsState()
    // Legacy branch placeholders. Live visualizer state is collected only by its leaf composables.
    val bassMagnitude = 0f
    val audioEnergy = 0f
    val isVisualizerRunning = false
    val audioEnergyFlow = viewModel.visualizerHelper.energy
    var showLoopPresetsSheet by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showStopDialog by remember { mutableStateOf(false) }
    var sleepMinutesInput by remember { mutableStateOf("30") }
    var showAudioEffectsSheet by remember { mutableStateOf(false) }

    var showSettingsDialog by remember { mutableStateOf(false) } 
    var isZenModeEnabled by remember { mutableStateOf(false) }
    var isLyricsMode by remember { mutableStateOf(false) }
    val currentLyrics by viewModel.currentLyrics.collectAsState()
    val lyrics = currentLyrics
    val isLyricsLoading by viewModel.isLyricsLoading.collectAsState()
    val lyricsCandidates by viewModel.lyricsCandidates.collectAsState()

    if (lyricsCandidates.isNotEmpty() && currentSong != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissLyricsCandidates,
            title = { Text("Choose the matching lyrics", fontFamily = OutfitFontFamily) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    lyricsCandidates.take(8).forEach { candidate ->
                        TextButton(
                            onClick = { viewModel.selectLyricsCandidate(currentSong!!, candidate) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(candidate.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${candidate.artist} • ${candidate.durationSeconds / 60}:${(candidate.durationSeconds % 60).toString().padStart(2, '0')}",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = viewModel::dismissLyricsCandidates) { Text("Cancel") }
            }
        )
    }

    var showSongGarageSheet by remember { mutableStateOf(false) }
    var showSongWikiSheet by remember { mutableStateOf(false) }
    val currentSongWiki by viewModel.currentSongWiki.collectAsState()
    val isSongWikiLoading by viewModel.isSongWikiLoading.collectAsState()

    // Sexy Collapsible Header Tool Capsule (< when collapsed, > + tools when expanded)
    var isHeaderToolsExpanded by rememberSaveable { mutableStateOf(false) }

    // Album Art Stage Side Tap to Seek 5s (-5s on Left, +5s on Right)
    var seekRippleText by remember { mutableStateOf<String?>(null) }
    var seekRippleIsForward by remember { mutableStateOf(true) }
    var accumulatedSeekSeconds by remember { mutableIntStateOf(0) }
    var lastSeekTapTime by remember { mutableLongStateOf(0L) }
    var seekTapCount by remember { mutableIntStateOf(0) }

    val triggerSideSeek: (Boolean) -> Unit = { isForward ->
        val now = System.currentTimeMillis()
        val isContinuation = now - lastSeekTapTime < 700L && seekRippleIsForward == isForward
        if (isContinuation) {
            seekTapCount++
        } else {
            seekTapCount = 1
            accumulatedSeekSeconds = 0
        }
        lastSeekTapTime = now
        seekRippleIsForward = isForward

        // Single tap (1 tap) does NOTHING to prevent accidental touches while viewing or swiping!
        // Double-tap (2 taps) triggers 5s seek; every subsequent rapid tap adds another 5s (+10s, +15s, etc.)
        if (seekTapCount >= 2) {
            accumulatedSeekSeconds += 5
            val newPos = if (isForward) {
                (progress + 5000L).coerceAtMost(duration)
            } else {
                (progress - 5000L).coerceAtLeast(0L)
            }
            viewModel.seekTo(newPos)
            seekRippleText = if (isForward) "+${accumulatedSeekSeconds}s" else "-${accumulatedSeekSeconds}s"
        }
    }

    LaunchedEffect(seekRippleText) {
        if (seekRippleText != null) {
            kotlinx.coroutines.delay(800)
            if (System.currentTimeMillis() - lastSeekTapTime >= 700L) {
                seekRippleText = null
                accumulatedSeekSeconds = 0
                seekTapCount = 0
            }
        }
    }

    // Ensure lyrics are loaded when Zen Mode or Lyrics Mode is active
    LaunchedEffect(isZenModeEnabled, isLyricsMode, currentSong?.id) {
        if ((isZenModeEnabled || isLyricsMode) && currentSong != null && currentLyrics == null) {
            viewModel.loadLyricsForSong(currentSong)
        }
    }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        FirebaseDiagnostics.recordScreen(context, "PlayerScreen")
    }
    
    // Gallery picker launcher for long-press cover artwork customization
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { pickedUri: Uri? ->
        if (pickedUri != null) {
            viewModel.updateCurrentSongAlbumArt(pickedUri.toString())
            viewModel.showInAppNotification("Cover artwork updated!")
        }
    }
    
    var dynamicAccentColor by remember { mutableStateOf(SoftGold) }
    var dynamicSecondaryColor by remember { mutableStateOf(Color(0xFFE91E63)) }
    var dynamicBgColor by remember { mutableStateOf(Color.Black) }

    LaunchedEffect(currentSong?.albumArtUri) {
        dynamicAccentColor = SoftGold
        dynamicSecondaryColor = Color(0xFFE91E63)
        dynamicBgColor = Color.Black

        val albumArt = currentSong?.albumArtUri
        if (!albumArt.isNullOrBlank()) {
            val request = ImageRequest.Builder(context)
                .data(albumArt)
                .allowHardware(false)
                .build()

            val result = withContext(Dispatchers.IO) {
                context.imageLoader.execute(request)
            }

            if (result is SuccessResult) {
                val bitmap = result.drawable.toBitmap()
                val palette = Palette.from(bitmap).generate()

                // Primary vibrant accent for YouTube-style ambient bloom
                val rawAccent = palette.lightVibrantSwatch?.rgb
                    ?: palette.vibrantSwatch?.rgb
                    ?: palette.dominantSwatch?.rgb
                if (rawAccent != null) {
                    val hsv = FloatArray(3)
                    android.graphics.Color.colorToHSV(rawAccent, hsv)

                    // On dark backgrounds: prevent dark muddy brown/olive buttons by transforming into luminous gold
                    if (hsv[0] in 15f..55f && hsv[2] < 0.85f) {
                        hsv[0] = 42f   // Luminous gold hue
                        hsv[1] = 0.62f // Rich golden saturation
                        hsv[2] = 0.96f // Brilliant luminosity
                    } else {
                        // High-contrast rule for dark theme: ALWAYS guarantee 85%-100% brightness (never dark on dark!)
                        hsv[1] = hsv[1].coerceIn(0.40f, 0.85f)
                        hsv[2] = hsv[2].coerceIn(0.85f, 1.0f)
                    }

                    dynamicAccentColor = Color(android.graphics.Color.HSVToColor(hsv))
                } else {
                    dynamicAccentColor = SoftGold
                }

                // Secondary complementary swatch for multi-chromatic fluid light
                val rawSec = palette.lightMutedSwatch?.rgb
                    ?: palette.mutedSwatch?.rgb
                    ?: palette.dominantSwatch?.rgb
                if (rawSec != null) {
                    val hsv = FloatArray(3)
                    android.graphics.Color.colorToHSV(rawSec, hsv)
                    hsv[1] = hsv[1].coerceIn(0.45f, 0.80f)
                    hsv[2] = hsv[2].coerceIn(0.80f, 0.95f)
                    dynamicSecondaryColor = Color(android.graphics.Color.HSVToColor(hsv))
                }

                palette.darkVibrantSwatch?.rgb?.let { dynamicBgColor = Color(it) }
                    ?: palette.darkMutedSwatch?.rgb?.let { dynamicBgColor = Color(it) }
            }
        }
    }

    val animatedAccentColor by animateColorAsState(targetValue = dynamicAccentColor, tween(1000))
    val animatedSecondaryColor by animateColorAsState(targetValue = dynamicSecondaryColor, tween(1000))
    val animatedBgColor by animateColorAsState(targetValue = dynamicBgColor, tween(1000))

    if (currentSong == null) {
        SideEffect { onNavigateBack() }
        return
    }

    val vinylRotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            vinylRotation.animateTo(
                targetValue = vinylRotation.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 20000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            vinylRotation.stop()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        animatedBgColor.copy(alpha = 0.65f),
                        Color(0xFF141416),
                        Color(0xFF0A0A0C)
                    )
                )
            )
    ) {
        val albumArtModel = remember(currentSong?.albumArtUri, currentSong?.uri) {
            val artUri = currentSong?.albumArtUri
            val songUri = currentSong?.uri
            val primaryData = if (!artUri.isNullOrBlank()) artUri else songUri
            if (primaryData != null) {
                ImageRequest.Builder(context)
                    .data(primaryData)
                    .allowHardware(true)
                    .size(coil.size.Size.ORIGINAL) // Native crystal-clear resolution
                    .crossfade(true)
                    .placeholder(R.drawable.audia_player_logo)
                    .error(R.drawable.audia_player_logo)
                    .fallback(R.drawable.audia_player_logo)
                    .build()
            } else {
                R.drawable.audia_player_logo
            }
        }

        // --- YouTube-Style Music-Reactive Ambient Mode & Lighting ---
        AmbientGlowVisualizer(
            primaryColor = animatedAccentColor,
            secondaryColor = animatedSecondaryColor,
            visualizerHelper = viewModel.visualizerHelper,
            isPlaying = isPlaying,
            isZenMode = isZenModeEnabled,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle gradient scrim for crisp foreground text/controls without hard cut-offs
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = if (isZenModeEnabled) 0.28f else 0.15f),
                            Color.Black.copy(alpha = if (isZenModeEnabled) 0.50f else 0.35f)
                        )
                    )
                )
        )

        BackHandler(enabled = isZenModeEnabled) {
            isZenModeEnabled = false
        }

        if (isZenModeEnabled) {
            ZenModeView(
                currentSong = currentSong,
                isPlaying = isPlaying,
                progress = progress,
                duration = duration,
                currentLyrics = currentLyrics,
                isLyricsLoading = isLyricsLoading,
                animatedAccentColor = animatedAccentColor,
                onPlayPause = { viewModel.togglePlayPause() },
                onPrevious = { viewModel.playPrevious() },
                onNext = { viewModel.playNext() },
                onExitZen = { isZenModeEnabled = false }
            )
        } else {
            AdaptiveImmersivePlayer(
                song = currentSong!!,
                albumArtModel = albumArtModel,
                previousArtwork = songList.indexOfFirst { it.uri == currentSong?.uri }
                    .takeIf { it >= 0 }?.let { songList.getOrNull(it - 1)?.albumArtUri },
                nextArtwork = songList.indexOfFirst { it.uri == currentSong?.uri }
                    .takeIf { it >= 0 }?.let { songList.getOrNull(it + 1)?.albumArtUri },
                isPlaying = isPlaying,
                audioEnergyFlow = audioEnergyFlow,
                progress = progress,
                duration = duration,
                accentColor = animatedAccentColor,
                lyrics = lyrics,
                isLyricsLoading = isLyricsLoading,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                playbackSpeed = playbackSpeed,
                loopStart = loopStart,
                loopEnd = loopEnd,
                loopEnabled = loopEnabled,
                isFavorite = allFavorites.any { it.uri == currentSong?.uri },
                sleepTimerActive = sleepTimerRemainingMs != null,
                seekFeedback = seekRippleText,
                seekFeedbackForward = seekRippleIsForward,
                onDismiss = onNavigateBack,
                onSeek = viewModel::seekTo,
                onSideSeek = triggerSideSeek,
                onPrevious = {
                    FirebaseDiagnostics.recordAction(context, "skip_previous")
                    viewModel.playPrevious()
                },
                onPlayPause = {
                    FirebaseDiagnostics.recordAction(context, "play_pause_toggle", if (isPlaying) "pause" else "play")
                    viewModel.togglePlayPause()
                },
                onNext = {
                    FirebaseDiagnostics.recordAction(context, "skip_next")
                    viewModel.playNext()
                },
                onSwipePrevious = viewModel::playPrevious,
                onSwipeNext = viewModel::playNext,
                onOpenWiki = {
                    currentSong?.let {
                        viewModel.loadSongWiki(it)
                        showSongWikiSheet = true
                    }
                },
                onToggleShuffle = {
                    viewModel.toggleShuffle()
                },
                onCycleRepeat = {
                    viewModel.cycleRepeatMode()
                },
                onToggleFavorite = {
                    currentSong?.let(viewModel::toggleFavorite)
                },
                onToggleLoop = viewModel::toggleABLoop,
                onOpenLoopPresets = { showLoopPresetsSheet = true },
                onCycleSpeed = viewModel::cyclePlaybackSpeed,
                onOpenSleepTimer = { showSleepDialog = true },
                onOpenEqualizer = {
                    viewModel.requestAudioSessionId()
                    showAudioEffectsSheet = true
                },
                onOpenGarage = { showSongGarageSheet = true },
                onOpenQueue = onNavigateToQueue,
                onOpenZen = { isZenModeEnabled = true },
                onRetryLyrics = { viewModel.retryLyricsForSong(currentSong) },
                onChangeArtwork = { galleryPickerLauncher.launch("image/*") }
            )
            if (false) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                val availableWidth = maxWidth
                val availableHeight = maxHeight
                val isCompactScreen = availableHeight < 740.dp
                val isUltraCompact = availableHeight < 620.dp

                val verticalOuterPadding = if (isUltraCompact) 2.dp else if (isCompactScreen) 4.dp else 8.dp
                val bottomNavClearance = if (isUltraCompact) 4.dp else if (isCompactScreen) 8.dp else 14.dp
                val adaptiveItemGap = if (isUltraCompact) 4.dp else if (isCompactScreen) 6.dp else 8.dp
                val adaptiveSectionGap = if (isUltraCompact) 6.dp else if (isCompactScreen) 8.dp else 12.dp

                // Adaptive Cover Size: dynamically balanced against available screen height and width
                val targetCoverHeight = (availableHeight * if (isUltraCompact) 0.30f else if (isCompactScreen) 0.33f else 0.37f).coerceIn(160.dp, 320.dp)
                val targetCoverWidth = (availableWidth - 44.dp).coerceAtLeast(160.dp)
                val coverSize = minOf(targetCoverWidth, targetCoverHeight)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .padding(top = verticalOuterPadding, bottom = bottomNavClearance),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (isCompactScreen) Arrangement.SpaceBetween else Arrangement.Top
                ) {
                // Header (Back, Lyrics Toggle, Zen Mode, Queue) - Balanced layout to eliminate crammed icons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Left: Collapse Arrow
                    BounceIconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(38.dp)
                            .align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Center: "NOW PLAYING" / "LYRICS" (Fades smoothly when header tool capsule expands)
                    val headerTitleAlpha by animateFloatAsState(
                        targetValue = if (isHeaderToolsExpanded) 0f else 1f,
                        animationSpec = tween(150),
                        label = "headerTitleAlpha"
                    )
                    Text(
                        text = if (isLyricsMode && lyrics != null && lyrics.lines.isNotEmpty()) "LYRICS" else "NOW PLAYING",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 12.sp,
                        letterSpacing = 3.sp,
                        color = if (isLyricsMode && lyrics != null && lyrics.lines.isNotEmpty()) animatedAccentColor else TextSecondary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .alpha(headerTitleAlpha)
                            .clickable {
                                if (lyrics != null && lyrics.lines.isNotEmpty()) {
                                    isLyricsMode = !isLyricsMode
                                }
                            }
                    )

                    // Right: Sexy Collapsible Action Capsule (< when collapsed, > + tools when expanded)
                    // When collapsed: totally frameless, just a bare `<` icon — no box, no border
                    // When expanded: frosted glass pill slides out with tools + > arrow
                    val capsuleBg by animateFloatAsState(
                        targetValue = if (isHeaderToolsExpanded) 0.09f else 0.0f,
                        animationSpec = tween(180),
                        label = "capsuleBgAlpha"
                    )
                    val capsuleBorder by animateFloatAsState(
                        targetValue = if (isHeaderToolsExpanded) 0.14f else 0.0f,
                        animationSpec = tween(180),
                        label = "capsuleBorderAlpha"
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = capsuleBg))
                            .border(1.dp, Color.White.copy(alpha = capsuleBorder), RoundedCornerShape(20.dp))
                            .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        // Slide-out Tools (visible when expanded)
                        AnimatedVisibility(
                            visible = isHeaderToolsExpanded,
                            enter = fadeIn(tween(180)) + expandHorizontally(spring(stiffness = Spring.StiffnessMedium)),
                            exit = fadeOut(tween(140)) + shrinkHorizontally(spring(stiffness = Spring.StiffnessMedium))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val hasLyrics = lyrics != null && lyrics.lines.isNotEmpty()

                                // 0. Favorite Button (heart)
                                val isFavInCapsule = currentSong?.let { song -> allFavorites.any { it.uri == song.uri } } ?: false
                                BounceIconButton(
                                    onClick = {
                                        currentSong?.let {
                                            viewModel.toggleFavorite(it)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFavInCapsule) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (isFavInCapsule) Color(0xFFE91E63) else Color.White.copy(alpha = 0.75f),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                // 1. Synchronized Lyrics Mode Toggle
                                BounceIconButton(
                                    onClick = {
                                        if (!hasLyrics) {
                                            currentSong?.let {
                                                viewModel.loadLyricsForSong(it)
                                                viewModel.showInAppNotification("Searching lyrics online...")
                                            }
                                            isLyricsMode = true
                                        } else {
                                            isLyricsMode = !isLyricsMode
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_lyrics),
                                        contentDescription = "Lyrics Mode",
                                        tint = if (isLyricsMode && hasLyrics) animatedAccentColor else Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                // 2. Zen Bedtime Mode Toggle
                                BounceIconButton(
                                    onClick = { isZenModeEnabled = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Brightness3,
                                        contentDescription = "Zen Bedtime Mode",
                                        tint = animatedAccentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // 3. Queue Navigation
                                BounceIconButton(
                                    onClick = onNavigateToQueue,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = "Queue",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Toggle Chevron: '<' when collapsed (bare icon), '>' when expanded (tap to collapse)
                        BounceIconButton(
                            onClick = {
                                isHeaderToolsExpanded = !isHeaderToolsExpanded
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isHeaderToolsExpanded) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = if (isHeaderToolsExpanded) "Collapse Tools" else "Expand Tools",
                                tint = if (isHeaderToolsExpanded) animatedAccentColor else Color.White.copy(alpha = 0.90f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(adaptiveItemGap))

                // ==========================================
                // THE STAGE VIEWPORT (Echo Music & Apple Music Style)
                // Modern Rounded-Corner Card or In-Place Synced Lyrics
                // ==========================================

                // Ambient Sound-Reactive Breathing Aura (Pulses gently with playback)
                val ambientTransition = rememberInfiniteTransition(label = "ambient_glow")
                val ambientPulseScale by ambientTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = if (isPlaying) 1.06f else 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2400, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "ambient_scale"
                )
                val ambientPulseAlpha by ambientTransition.animateFloat(
                    initialValue = 0.32f,
                    targetValue = if (isPlaying) 0.55f else 0.20f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2400, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "ambient_alpha"
                )

                // Real-time FFT Frequency & Bass Reactive Lighting
                val effectiveScale = if (isVisualizerRunning && isPlaying) {
                    1.0f + (bassMagnitude * 0.22f)
                } else {
                    ambientPulseScale
                }

                val effectiveAlpha = if (isVisualizerRunning && isPlaying) {
                    (0.30f + (bassMagnitude * 0.42f)).coerceIn(0.20f, 0.75f)
                } else {
                    ambientPulseAlpha
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Ambient Sound-Reactive Aura behind the card
                    Box(
                        modifier = Modifier
                            .size(coverSize + 44.dp)
                            .scale(effectiveScale)
                            .blur(44.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        animatedAccentColor.copy(alpha = effectiveAlpha),
                                        animatedAccentColor.copy(alpha = effectiveAlpha * 0.35f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    val hasLyrics = lyrics != null && lyrics.lines.isNotEmpty()
                    val showLyricsInStage = isLyricsMode && hasLyrics

                    AnimatedContent(
                        targetState = showLyricsInStage,
                        transitionSpec = {
                            (fadeIn(tween(350)) + slideInVertically { it / 4 }) togetherWith
                            (fadeOut(tween(300)) + slideOutVertically { -it / 4 })
                        },
                        label = "StageViewFlipper"
                    ) { inLyricsMode ->
                        if (inLyricsMode && lyrics != null) {
                            // Immersive Open Lyrics (Spotify & Apple Music style - Fluid edge-to-edge, NO box, NO border!)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((coverSize + 48.dp).coerceIn(260.dp, 360.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                LyricsView(
                                    lyrics = lyrics,
                                    isLoading = isLyricsLoading,
                                    currentPositionMs = progress,
                                    onSeekTo = { viewModel.seekTo(it) },
                                    onRetry = { viewModel.retryLyricsForSong(currentSong) },
                                    accentColor = animatedAccentColor,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            // View A: Modern Rounded-Rectangle Album Artwork Card (NO vinyl center hole!)
                            val animatedScale by animateFloatAsState(
                                targetValue = if (isPlaying) 1f else 0.94f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                label = "CardScale"
                            )

                            Box(
                                modifier = Modifier
                                    .size(coverSize)
                                    .scale(animatedScale)
                                    .shadow(20.dp, RoundedCornerShape(26.dp), spotColor = Color.Black.copy(alpha = 0.85f))
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(BlackSurface)
                                    .border(
                                        width = 1.5.dp,
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.22f),
                                                Color.White.copy(alpha = 0.05f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(26.dp)
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            if (hasLyrics) {
                                                isLyricsMode = true
                                            } else {
                                                currentSong?.let { viewModel.loadLyricsForSong(it) }
                                                viewModel.showInAppNotification("No synced lyrics found")
                                            }
                                        },
                                        onLongClick = {
                                            galleryPickerLauncher.launch("image/*")
                                            viewModel.showInAppNotification("Pick new cover from Gallery")
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                SubcomposeAsyncImage(
                                    model = albumArtModel,
                                    contentDescription = "Album Art",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    loading = {
                                        Image(
                                            painter = painterResource(R.drawable.audia_player_logo),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    },
                                    error = {
                                        Image(
                                            painter = painterResource(R.drawable.audia_player_logo),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                )

                                // Soft glass sheen gradient on card
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.White.copy(alpha = 0.12f),
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.35f)
                                                )
                                            )
                                        )
                                )
                            }
                        }
                    }

                    if (!showLyricsInStage) {
                        // Left Side Tap Zone: 34% of width (margin + left side of album art) -> -5s Seek
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .height(coverSize)
                                .fillMaxWidth(0.34f)
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { triggerSideSeek(false) },
                                    onLongClick = {
                                        galleryPickerLauncher.launch("image/*")
                                        viewModel.showInAppNotification("Pick new cover from Gallery")
                                    }
                                )
                        )

                        // Right Side Tap Zone: 34% of width (margin + right side of album art) -> +5s Seek
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .height(coverSize)
                                .fillMaxWidth(0.34f)
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { triggerSideSeek(true) },
                                    onLongClick = {
                                        galleryPickerLauncher.launch("image/*")
                                        viewModel.showInAppNotification("Pick new cover from Gallery")
                                    }
                                )
                        )

                        // Floating YouTube-style Seek Ripple Badge
                        val seekBadgeAlpha by animateFloatAsState(
                            targetValue = if (seekRippleText != null) 1f else 0f,
                            animationSpec = tween(150),
                            label = "seekBadgeAlpha"
                        )
                        val seekBadgeScale by animateFloatAsState(
                            targetValue = if (seekRippleText != null) 1f else 0.8f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                            label = "seekBadgeScale"
                        )

                        if (seekBadgeAlpha > 0.01f) {
                            Box(
                                modifier = Modifier
                                    .align(if (seekRippleIsForward) Alignment.CenterEnd else Alignment.CenterStart)
                                    .padding(horizontal = 24.dp)
                                    .graphicsLayer {
                                        alpha = seekBadgeAlpha
                                        scaleX = seekBadgeScale
                                        scaleY = seekBadgeScale
                                    }
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .border(1.dp, animatedAccentColor.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (seekRippleIsForward) "${seekRippleText} »" else "« ${seekRippleText}",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = animatedAccentColor
                                )
                            }
                        }
                    }
                }

                // Real-time Synced Lyric State Machine (Strictly deterministic slot height, zero layout shifts)
                val isInstrumentalSection = remember(currentLyrics, progress) {
                    val current = currentLyrics
                    if (current != null && current.isSynced && current.lines.isNotEmpty()) {
                        val idx = com.eplaytime.app.util.LrcParser.findActiveLyricIndex(current.lines, progress)
                        if (idx < 0) {
                            true // Pre-vocal intro
                        } else if (idx < current.lines.size - 1) {
                            val nextLine = current.lines[idx + 1]
                            val curLine = current.lines[idx]
                            (nextLine.timestampMs - curLine.timestampMs > 9_000L) && (progress - curLine.timestampMs > 4_500L)
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }

                val activeLyricText = remember(currentLyrics, progress) {
                    val current = currentLyrics
                    if (current == null || current.lines.isEmpty()) {
                        null
                    } else if (current.isSynced) {
                        val idx = com.eplaytime.app.util.LrcParser.findActiveLyricIndex(current.lines, progress)
                        if (idx >= 0 && idx < current.lines.size) {
                            current.lines[idx].content.trim()
                        } else {
                            null
                        }
                    } else {
                        current.lines.firstOrNull()?.content?.trim()
                    }
                }

                Spacer(modifier = Modifier.height(adaptiveItemGap))

                // Stable Fixed-Height 2-Line Lyric Stage Slot (Guarantees zero layout shifts across all states!)
                val lyricStageHeight = if (isUltraCompact) 38.dp else if (isCompactScreen) 44.dp else 48.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(lyricStageHeight),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        // State 1: Full In-Stage Lyrics Mode is Active
                        isLyricsMode -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(animatedAccentColor.copy(alpha = 0.12f))
                                    .clickable { isLyricsMode = false }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = animatedAccentColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Full lyrics active • Tap to view album art",
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 12.sp,
                                    color = animatedAccentColor
                                )
                            }
                        }

                        // State 2: Lyrics are Loading
                        isLyricsLoading && (lyrics == null || lyrics.lines.isEmpty()) -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 1.8.dp,
                                    color = animatedAccentColor.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = "Searching lyrics...",
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.50f)
                                )
                            }
                        }

                        // State 3: Instrumental Section
                        isInstrumentalSection -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = animatedAccentColor.copy(alpha = 0.70f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "♪ Instrumental ♪",
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.60f)
                                )
                            }
                        }

                        // State 4: Active Synced Lyric Text (1 or 2 lines)
                        !activeLyricText.isNullOrBlank() -> {
                            AnimatedContent(
                                targetState = activeLyricText,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                                },
                                label = "ActiveLyricPreviewText"
                            ) { lyricLine ->
                                Text(
                                    text = lyricLine,
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = if (isCompactScreen) 13.sp else 15.sp,
                                    lineHeight = if (isCompactScreen) 17.sp else 20.sp,
                                    color = Color.White.copy(alpha = 0.95f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (lyrics != null && lyrics.lines.isNotEmpty()) {
                                            isLyricsMode = true
                                        }
                                    }
                                )
                            }
                        }

                        // State 5: Lyrics Unavailable / No lyrics found
                        else -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.04f))
                                    .clickable {
                                        currentSong?.let {
                                            viewModel.loadLyricsForSong(it)
                                            viewModel.showInAppNotification("Searching lyrics online...")
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_lyrics),
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.35f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Lyrics unavailable • Tap to search",
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.40f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(adaptiveItemGap))

                // Track Title & Artist (Full width, no trailing favorite button!)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            currentSong?.let {
                                viewModel.loadSongWiki(it)
                                showSongWikiSheet = true
                            }
                        }
                ) {
                    MarqueeTitleText(
                        text = currentSong?.title ?: "Unknown Title",
                        fontSize = if (isCompactScreen) 19.sp else 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        alignStart = true,
                        initialDelayMillis = 2000
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentSong?.artist ?: "Unknown Artist",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = if (isCompactScreen) 13.sp else 15.sp,
                        color = animatedAccentColor.copy(alpha = 0.90f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(adaptiveSectionGap))

                // Bottom Controls & Scrubber (Always Fixed & Stable)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        PremiumProgressBar(
                            progress = progress,
                            duration = duration,
                            loopA = loopStart,
                            loopB = loopEnd,
                            onSeek = { viewModel.seekTo(it) },
                            accentColor = animatedAccentColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (loopStart != null && duration > 0) {
                            val startPercent = (loopStart!! / duration.toFloat())
                            Box(modifier = Modifier.fillMaxWidth(startPercent).align(Alignment.CenterStart).padding(start = 16.dp)) {
                                Text("A", color = animatedAccentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterEnd))
                            }
                        }
                        if (loopEnd != null && duration > 0) {
                            val endPercent = (loopEnd!! / duration.toFloat())
                            Box(modifier = Modifier.fillMaxWidth(endPercent).align(Alignment.CenterStart).padding(start = 16.dp)) {
                                Text("B", color = animatedAccentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterEnd))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatDurationFull(progress), fontFamily = OutfitFontFamily, fontWeight = FontWeight.Light, color = TextSecondary, fontSize = 12.sp)
                        Text("-${formatDurationFull((duration - progress).coerceAtLeast(0L))}", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Light, color = TextSecondary, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(adaptiveSectionGap))

                // =========================================================================
                // MASTER USER CONTROLS: Left 3x3 Grid (9 Buttons) + Right Playback Cluster
                // =========================================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // LEFT SIDE: 3x3 Grid of 9 Function & Utility Buttons
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Row 1: Shuffle | Repeat | Favorite
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Shuffle
                            BounceIconButton(
                                onClick = {
                                    viewModel.toggleShuffle()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (shuffleEnabled) animatedAccentColor else Color.White.copy(alpha = 0.50f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // 2. Repeat
                            BounceIconButton(
                                onClick = {
                                    viewModel.cycleRepeatMode()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                    contentDescription = "Repeat",
                                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) animatedAccentColor else Color.White.copy(alpha = 0.50f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // 3. Favorite / Heart
                            val isFavorite = currentSong?.let { song -> allFavorites.any { it.uri == song.uri } } ?: false
                            BounceIconButton(
                                onClick = {
                                    currentSong?.let {
                                        viewModel.toggleFavorite(it)
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) Color(0xFFE91E63) else Color.White.copy(alpha = 0.55f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Row 2: A-B Loop | Playback Speed | Sleep Timer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 4. A-B Loop
                            BounceIconButton(
                                onClick = {
                                    viewModel.toggleABLoop()
                                },
                                onLongClick = { showLoopPresetsSheet = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                val loopText = when {
                                    loopEnabled -> "A-B"
                                    loopStart != null -> "A"
                                    else -> "A-B"
                                }
                                val loopColor = when {
                                    loopEnabled -> animatedAccentColor
                                    loopStart != null -> animatedAccentColor.copy(alpha = 0.7f)
                                    else -> Color.White.copy(alpha = 0.50f)
                                }
                                Text(
                                    text = loopText,
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = loopColor
                                )
                            }

                            // 5. Playback Speed
                            BounceIconButton(
                                onClick = {
                                    viewModel.cyclePlaybackSpeed()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                val isCustomSpeed = playbackSpeed != 1.0f
                                Text(
                                    text = "${playbackSpeed}x",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (isCustomSpeed) animatedAccentColor else Color.White.copy(alpha = 0.50f)
                                )
                            }

                            // 6. Sleep Timer
                            BounceIconButton(
                                onClick = { showSleepDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Sleep Timer",
                                    tint = if (sleepTimerRemainingMs != null) animatedAccentColor else Color.White.copy(alpha = 0.50f),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        // Row 3: Equalizer / FX | Song Garage | Lyrics View Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 7. Equalizer
                            BounceIconButton(
                                onClick = {
                                    viewModel.requestAudioSessionId()
                                    showAudioEffectsSheet = true
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Equalizer",
                                    tint = animatedAccentColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            // 8. Song Garage (Audio Trimmer, Tag Editor, Tech Specs)
                            BounceIconButton(
                                onClick = { showSongGarageSheet = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = "Song Garage",
                                    tint = SoftGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // 9. Lyrics View Toggle
                            BounceIconButton(
                                onClick = {
                                    if (lyrics == null || lyrics.lines.isEmpty()) {
                                        currentSong?.let {
                                            viewModel.loadLyricsForSong(it)
                                            viewModel.showInAppNotification("Searching lyrics online...")
                                        }
                                        isLyricsMode = true
                                    } else {
                                        isLyricsMode = !isLyricsMode
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_lyrics),
                                    contentDescription = "Lyrics Mode",
                                    tint = if (isLyricsMode) animatedAccentColor else Color.White.copy(alpha = 0.55f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Elegant vertical glass divider between Left 3x3 Grid and Right Master Controls
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(112.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // RIGHT SIDE: Master Playback Cluster (Unified Frosted Glass Enclosure)
                    Column(
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.05f),
                                        Color.White.copy(alpha = 0.02f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.10f),
                                        Color.White.copy(alpha = 0.03f)
                                    )
                                ),
                                shape = RoundedCornerShape(26.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Smooth morphing between Arch (when paused) and Circle (when playing)
                        val morphBottomCorner by animateDpAsState(
                            targetValue = if (isPlaying) 34.dp else 16.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                            label = "playButtonBottomCorner"
                        )
                        val morphHeight by animateDpAsState(
                            targetValue = if (isPlaying) 68.dp else 74.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                            label = "playButtonHeight"
                        )
                        val dynamicShape = RoundedCornerShape(
                            topStart = 34.dp,
                            topEnd = 34.dp,
                            bottomStart = morphBottomCorner,
                            bottomEnd = morphBottomCorner
                        )

                        // Pure, Clean & Modern Play/Pause Button
                        Box(
                            modifier = Modifier
                                .width(68.dp)
                                .height(morphHeight)
                                .shadow(
                                    elevation = 12.dp,
                                    shape = dynamicShape,
                                    spotColor = animatedAccentColor.copy(alpha = 0.50f)
                                )
                                .clip(dynamicShape)
                                .background(animatedAccentColor)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    role = Role.Button
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.togglePlayPause()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPlaying) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Navigation Pair (< Previous & > Next)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Previous Button (<)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                                    .bounceClick(scaleDown = 0.85f) { viewModel.playPrevious() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous",
                                    tint = Color.White.copy(alpha = 0.95f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Next Button (>)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                                    .bounceClick(scaleDown = 0.85f) { viewModel.playNext() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next",
                                    tint = Color.White.copy(alpha = 0.95f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
            }
        }
    }

        if (showSleepDialog) {
            val stopAfterSong by viewModel.stopAfterCurrentSong.collectAsState()
            AlertDialog(
                onDismissRequest = { showSleepDialog = false },
                title = { Text("Sleep Timer", fontFamily = OutfitFontFamily, fontWeight = FontWeight.SemiBold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape = RoundedCornerShape(12.dp))
                                .background(if (stopAfterSong) animatedAccentColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                                .clickable { viewModel.toggleStopAfterCurrentSong(); showSleepDialog = false }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Stop after this song", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Medium, color = if (stopAfterSong) animatedAccentColor else Color.White)
                                Text(if (stopAfterSong) "Active — will stop when song ends" else "Pause when current song finishes", fontFamily = OutfitFontFamily, fontSize = 12.sp, color = TextSecondary)
                            }
                            Icon(if (stopAfterSong) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = if (stopAfterSong) animatedAccentColor else TextSecondary, modifier = Modifier.size(22.dp))
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        Text("Or set a timer:", fontFamily = OutfitFontFamily, fontSize = 13.sp, color = TextSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(onClick = { sleepMinutesInput = "15" }, label = { Text("15m") })
                            AssistChip(onClick = { sleepMinutesInput = "30" }, label = { Text("30m") })
                            AssistChip(onClick = { sleepMinutesInput = "45" }, label = { Text("45m") })
                            AssistChip(onClick = { sleepMinutesInput = "60" }, label = { Text("60m") })
                        }
                        OutlinedTextField(
                            value = sleepMinutesInput,
                            onValueChange = { sleepMinutesInput = it.filter { ch -> ch.isDigit() } },
                            singleLine = true,
                            label = { Text("Minutes") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val mins = sleepMinutesInput.toIntOrNull()?.coerceAtLeast(1) ?: 30
                        FirebaseDiagnostics.recordAction(context, "start_sleep_timer", "${mins}m")
                        viewModel.startSleepTimer(minutes = mins, fadeOut = true)
                        showSleepDialog = false
                    }) { Text("Start Timer") }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            FirebaseDiagnostics.recordAction(context, "cancel_sleep_timer")
                            viewModel.cancelSleepTimer()
                            if (viewModel.stopAfterCurrentSong.value) viewModel.toggleStopAfterCurrentSong()
                            showSleepDialog = false
                        }) { Text("Cancel") }
                        TextButton(onClick = { showSleepDialog = false }) { Text("Close") }
                    }
                }
            )
        }

        if (showAudioEffectsSheet) {
            AudioEffectsSheet(audioSessionId = audioSessionId, viewModel = viewModel, onDismiss = { showAudioEffectsSheet = false })
        }

        val activeSong = currentSong
        if (showSongGarageSheet && activeSong != null) {
            SongGarageBottomSheet(
                song = activeSong,
                viewModel = viewModel,
                onDismiss = { showSongGarageSheet = false },
                onOpenWiki = {
                    showSongGarageSheet = false
                    viewModel.loadSongWiki(activeSong)
                    showSongWikiSheet = true
                }
            )
        }

        if (showSongWikiSheet && activeSong != null) {
            SongWikiBottomSheet(
                song = activeSong,
                wikiData = currentSongWiki,
                isLoading = isSongWikiLoading,
                onDismiss = { showSongWikiSheet = false },
                onRetry = { viewModel.loadSongWiki(activeSong) }
            )
        }

        if (showLoopPresetsSheet) {
            LoopPresetsBottomSheet(
                song = activeSong,
                viewModel = viewModel,
                loopStart = loopStart,
                loopEnd = loopEnd,
                loopEnabled = loopEnabled,
                loopPresets = loopPresets,
                onDismiss = { showLoopPresetsSheet = false }
            )
        }

        if (showStopDialog) {
            AlertDialog(
                onDismissRequest = { showStopDialog = false },
                title = { Text("Stop Options", fontFamily = OutfitFontFamily, fontWeight = FontWeight.SemiBold) },
                text = { Text("Choose manual stop behavior.", fontFamily = OutfitFontFamily, color = TextSecondary) },
                confirmButton = { TextButton(onClick = { viewModel.setStopAfterCurrent(true); showStopDialog = false }) { Text("After This Song") } },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { viewModel.stopPlaybackNow(useFade = true); viewModel.setStopAfterCurrent(false); showStopDialog = false }) { Text("Stop Now") }
                        TextButton(onClick = { viewModel.setStopAfterCurrent(false); showStopDialog = false }) { Text("Clear") }
                    }
                }
            )
        }
    }
}

@Composable
private fun AdaptiveImmersivePlayer(
    song: Song,
    albumArtModel: Any,
    previousArtwork: String?,
    nextArtwork: String?,
    isPlaying: Boolean,
    audioEnergyFlow: kotlinx.coroutines.flow.StateFlow<Float>,
    progress: Long,
    duration: Long,
    accentColor: Color,
    lyrics: ParsedLyrics?,
    isLyricsLoading: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    playbackSpeed: Float,
    loopStart: Long?,
    loopEnd: Long?,
    loopEnabled: Boolean,
    isFavorite: Boolean,
    sleepTimerActive: Boolean,
    seekFeedback: String?,
    seekFeedbackForward: Boolean,
    onDismiss: () -> Unit,
    onSeek: (Long) -> Unit,
    onSideSeek: (Boolean) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    onOpenWiki: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleLoop: () -> Unit,
    onOpenLoopPresets: () -> Unit,
    onCycleSpeed: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenGarage: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenZen: () -> Unit,
    onRetryLyrics: () -> Unit,
    onChangeArtwork: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val landscape = maxWidth > maxHeight
        val compact = !landscape && maxHeight < 740.dp
        // Width preserves the portrait artwork feel; available height protects lyrics space.
        val heroHeight = if (landscape) {
            (maxHeight - 16.dp).coerceAtLeast(280.dp)
        } else {
            minOf(maxWidth * 1.12f, maxHeight * 0.52f).coerceIn(350.dp, 500.dp)
        }
        val pagerState = rememberPagerState(pageCount = { 2 })
        val scope = rememberCoroutineScope()

        val horizontalPadding = if (maxWidth < 360.dp) 12.dp else 16.dp
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding, vertical = 8.dp)

        if (landscape) {
            Row(
                modifier = contentModifier,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(0.44f)) {
                    ImmersiveHeroCover(
                        song, albumArtModel, previousArtwork, nextArtwork, heroHeight, false,
                        isPlaying, audioEnergyFlow, progress, duration, accentColor, loopStart, loopEnd,
                        seekFeedback, seekFeedbackForward, onDismiss,
                        { scope.launch { pagerState.animateScrollToPage(1) } }, onSeek, onSideSeek,
                        onPrevious, onPlayPause, onNext, onSwipePrevious, onSwipeNext, onOpenWiki,
                        shuffleEnabled, isFavorite, onToggleShuffle, onToggleFavorite, onChangeArtwork
                    )
                }
                VerticalPlaybackDeck(
                    isPlaying = isPlaying,
                    audioEnergyFlow = audioEnergyFlow,
                    accentColor = accentColor,
                    onPrevious = onPrevious,
                    onPlayPause = onPlayPause,
                    onNext = onNext
                )
                PlayerContentPager(
                    pagerState, lyrics, isLyricsLoading, progress, accentColor, shuffleEnabled,
                    repeatMode, playbackSpeed, loopStart, loopEnabled, isFavorite, sleepTimerActive,
                    { scope.launch { pagerState.animateScrollToPage(it) } }, onSeek, onRetryLyrics,
                    onToggleShuffle, onCycleRepeat, onToggleFavorite, onToggleLoop,
                    onOpenLoopPresets, onCycleSpeed, onOpenSleepTimer, onOpenEqualizer,
                    onOpenGarage, onOpenQueue, onOpenZen,
                    modifier = Modifier.weight(0.56f)
                )
            }
        } else Column(
            modifier = contentModifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ImmersiveHeroCover(
                song = song,
                albumArtModel = albumArtModel,
                previousArtwork = previousArtwork,
                nextArtwork = nextArtwork,
                height = heroHeight,
                compact = compact,
                isPlaying = isPlaying,
                audioEnergyFlow = audioEnergyFlow,
                progress = progress,
                duration = duration,
                accentColor = accentColor,
                loopStart = loopStart,
                loopEnd = loopEnd,
                seekFeedback = seekFeedback,
                seekFeedbackForward = seekFeedbackForward,
                onDismiss = onDismiss,
                onOverflow = { scope.launch { pagerState.animateScrollToPage(1) } },
                onSeek = onSeek,
                onSideSeek = onSideSeek,
                onPrevious = onPrevious,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onSwipePrevious = onSwipePrevious,
                onSwipeNext = onSwipeNext,
                onOpenWiki = onOpenWiki,
                shuffleEnabled = shuffleEnabled,
                isFavorite = isFavorite,
                onToggleShuffle = onToggleShuffle,
                onToggleFavorite = onToggleFavorite,
                onChangeArtwork = onChangeArtwork
            )

            if (!compact) {
                Spacer(Modifier.height(10.dp))
                PrimaryPlaybackDeck(
                    isPlaying = isPlaying,
                    audioEnergyFlow = audioEnergyFlow,
                    accentColor = accentColor,
                    onSeekBack = { onSeek((progress - 5_000L).coerceAtLeast(0L)) },
                    onPrevious = onPrevious,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onSeekForward = { onSeek((progress + 5_000L).coerceAtMost(duration)) }
                )
                Spacer(Modifier.height(8.dp))
            } else {
                Spacer(Modifier.height(6.dp))
            }

            PlayerContentPager(
                pagerState = pagerState,
                lyrics = lyrics,
                isLyricsLoading = isLyricsLoading,
                progress = progress,
                accentColor = accentColor,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                playbackSpeed = playbackSpeed,
                loopStart = loopStart,
                loopEnabled = loopEnabled,
                isFavorite = isFavorite,
                sleepTimerActive = sleepTimerActive,
                onSelectPage = { scope.launch { pagerState.animateScrollToPage(it) } },
                onSeek = onSeek,
                onRetryLyrics = onRetryLyrics,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onToggleFavorite = onToggleFavorite,
                onToggleLoop = onToggleLoop,
                onOpenLoopPresets = onOpenLoopPresets,
                onCycleSpeed = onCycleSpeed,
                onOpenSleepTimer = onOpenSleepTimer,
                onOpenEqualizer = onOpenEqualizer,
                onOpenGarage = onOpenGarage,
                onOpenQueue = onOpenQueue,
                onOpenZen = onOpenZen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun VerticalPlaybackDeck(
    isPlaying: Boolean,
    audioEnergyFlow: kotlinx.coroutines.flow.StateFlow<Float>,
    accentColor: Color,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PlaybackIcon(Icons.Default.SkipPrevious, "Previous track", onPrevious, 28.dp)
        AudiaPlayPause(isPlaying, audioEnergyFlow, accentColor, onPlayPause)
        PlaybackIcon(Icons.Default.SkipNext, "Next track", onNext, 28.dp)
    }
}

@Composable
private fun ImmersiveHeroCover(
    song: Song,
    albumArtModel: Any,
    previousArtwork: String?,
    nextArtwork: String?,
    height: androidx.compose.ui.unit.Dp,
    compact: Boolean,
    isPlaying: Boolean,
    audioEnergyFlow: kotlinx.coroutines.flow.StateFlow<Float>,
    progress: Long,
    duration: Long,
    accentColor: Color,
    loopStart: Long?,
    loopEnd: Long?,
    seekFeedback: String?,
    seekFeedbackForward: Boolean,
    onDismiss: () -> Unit,
    onOverflow: () -> Unit,
    onSeek: (Long) -> Unit,
    onSideSeek: (Boolean) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    onOpenWiki: () -> Unit,
    shuffleEnabled: Boolean,
    isFavorite: Boolean,
    onToggleShuffle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onChangeArtwork: () -> Unit
) {
    var horizontalDrag by remember { mutableFloatStateOf(0f) }
    val shape = RoundedCornerShape(30.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(Color(0xFF111111))
            .pointerInput(song.id) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontalDrag = 0f },
                    onHorizontalDrag = { _, amount -> horizontalDrag += amount },
                    onDragEnd = {
                        when {
                            horizontalDrag > 90f -> onSwipePrevious()
                            horizontalDrag < -90f -> onSwipeNext()
                        }
                        horizontalDrag = 0f
                    },
                    onDragCancel = { horizontalDrag = 0f }
                )
            }
            .pointerInput(song.id, progress, duration) {
                detectTapGestures(
                    onDoubleTap = { position -> onSideSeek(position.x > size.width / 2f) },
                    onLongPress = { onChangeArtwork() }
                )
            }
    ) {
        if (horizontalDrag != 0f) {
            AsyncImage(
                model = (if (horizontalDrag > 0f) previousArtwork else nextArtwork) ?: R.drawable.audia_player_logo,
                contentDescription = if (horizontalDrag > 0f) "Previous track preview" else "Next track preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        AsyncImage(
            model = albumArtModel,
            contentDescription = "${song.title} album artwork",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = horizontalDrag * 0.55f
                    rotationZ = horizontalDrag / size.width * 2.5f
                    scaleX = 1f - (kotlin.math.abs(horizontalDrag) / size.width).coerceAtMost(0.04f)
                    scaleY = scaleX
                }
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.48f),
                        0.34f to Color.Transparent,
                        0.62f to Color.Black.copy(alpha = 0.16f),
                        1f to Color.Black.copy(alpha = 0.92f)
                    )
                )
        )

        BounceIconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart).padding(8.dp).size(48.dp)) {
            Icon(Icons.Default.KeyboardArrowDown, "Minimize player", tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Text(
            "NOW PLAYING",
            color = Color.White.copy(alpha = 0.88f),
            fontFamily = OutfitFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.4.sp,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 23.dp)
        )
        BounceIconButton(onClick = onOverflow, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(48.dp)) {
            Icon(Icons.Default.MoreVert, "More player options", tint = Color.White, modifier = Modifier.size(25.dp))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenWiki)
                ) {
                    MarqueeTitleText(
                        text = song.title,
                        fontSize = if (compact) 22.sp else 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        alignStart = true,
                        initialDelayMillis = 1800
                    )
                    Text(
                        song.artist.ifBlank { "Unknown Artist" },
                        color = Color.White.copy(alpha = 0.76f),
                        fontFamily = OutfitFontFamily,
                        fontSize = if (compact) 14.sp else 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BounceIconButton(onClick = onToggleShuffle, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Default.Shuffle,
                        "Shuffle",
                        tint = if (shuffleEnabled) accentColor else Color.White.copy(alpha = 0.64f),
                        modifier = Modifier.size(21.dp)
                    )
                }
                BounceIconButton(onClick = onToggleFavorite, modifier = Modifier.size(40.dp)) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        if (isFavorite) "Remove favorite" else "Add favorite",
                        tint = if (isFavorite) Color(0xFFFF5B73) else Color.White.copy(alpha = 0.64f),
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
            Box(Modifier.fillMaxWidth()) {
                PremiumProgressBar(
                    progress = progress,
                    duration = duration,
                    loopA = loopStart,
                    loopB = loopEnd,
                    onSeek = onSeek,
                    accentColor = accentColor,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDurationFull(progress), color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
                Text("-${formatDurationFull((duration - progress).coerceAtLeast(0L))}", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
            }
            if (compact) {
                PrimaryPlaybackDeck(
                    isPlaying = isPlaying,
                    audioEnergyFlow = audioEnergyFlow,
                    accentColor = accentColor,
                    onSeekBack = { onSeek((progress - 5_000L).coerceAtLeast(0L)) },
                    onPrevious = onPrevious,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onSeekForward = { onSeek((progress + 5_000L).coerceAtMost(duration)) },
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        if (seekFeedback != null) {
            Text(
                if (seekFeedbackForward) "$seekFeedback »" else "« $seekFeedback",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(if (seekFeedbackForward) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            )
        }
    }
}

@Composable
private fun PrimaryPlaybackDeck(
    isPlaying: Boolean,
    audioEnergyFlow: kotlinx.coroutines.flow.StateFlow<Float>,
    accentColor: Color,
    onSeekBack: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onSeekForward: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaybackIcon(Icons.Default.Replay5, "Seek back 5 seconds", onSeekBack)
        PlaybackIcon(Icons.Default.SkipPrevious, "Previous track", onPrevious, 30.dp)
        AudiaPlayPause(isPlaying, audioEnergyFlow, accentColor, onPlayPause)
        PlaybackIcon(Icons.Default.SkipNext, "Next track", onNext, 30.dp)
        PlaybackIcon(Icons.Default.Forward5, "Seek forward 5 seconds", onSeekForward)
    }
}

@Composable
private fun PlaybackIcon(icon: ImageVector, description: String, onClick: () -> Unit, size: androidx.compose.ui.unit.Dp = 24.dp) {
    BounceIconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(icon, description, tint = Color.White.copy(alpha = 0.88f), modifier = Modifier.size(size))
    }
}

@Composable
private fun AudiaPlayPause(
    isPlaying: Boolean,
    audioEnergyFlow: kotlinx.coroutines.flow.StateFlow<Float>,
    accentColor: Color,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "audiaPlayPress"
    )
    val bottomCorner by animateDpAsState(if (isPlaying) 32.dp else 16.dp, label = "audiaPlayShape")
    val shape = RoundedCornerShape(32.dp, 32.dp, bottomCorner, bottomCorner)
    val buttonColor = if (accentColor == Color.Unspecified || accentColor.alpha < 0.2f) SoftGold else accentColor
    val isLightBackground = (0.299f * buttonColor.red + 0.587f * buttonColor.green + 0.114f * buttonColor.blue) > 0.45f
    val iconTint = if (isLightBackground) Color(0xFF121318) else Color.White

    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(pressScale)
            .clip(shape)
            .background(buttonColor)
            .border(1.dp, Color.White.copy(alpha = 0.25f), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = if (isPlaying) "Pause" else "Play"
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = iconTint,
            modifier = Modifier.size(34.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerContentPager(
    pagerState: androidx.compose.foundation.pager.PagerState,
    lyrics: ParsedLyrics?,
    isLyricsLoading: Boolean,
    progress: Long,
    accentColor: Color,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    playbackSpeed: Float,
    loopStart: Long?,
    loopEnabled: Boolean,
    isFavorite: Boolean,
    sleepTimerActive: Boolean,
    onSelectPage: (Int) -> Unit,
    onSeek: (Long) -> Unit,
    onRetryLyrics: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleLoop: () -> Unit,
    onOpenLoopPresets: () -> Unit,
    onCycleSpeed: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenGarage: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenZen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            listOf("Lyrics", "Tools").forEachIndexed { index, label ->
                TextButton(onClick = { onSelectPage(index) }) {
                    Text(
                        label,
                        color = if (pagerState.currentPage == index) Color.White else Color.White.copy(alpha = 0.46f),
                        fontWeight = if (pagerState.currentPage == index) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            if (page == 0) {
                when {
                    lyrics != null && lyrics.lines.isNotEmpty() -> LyricsView(
                        lyrics = lyrics,
                        isLoading = isLyricsLoading,
                        currentPositionMs = progress,
                        onSeekTo = onSeek,
                        onRetry = onRetryLyrics,
                        accentColor = accentColor,
                        modifier = Modifier.fillMaxSize()
                    )
                    isLyricsLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accentColor, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        TextButton(onClick = onRetryLyrics) { Text("Find lyrics", color = Color.White) }
                    }
                }
            } else {
                PlayerToolsGrid(
                    accentColor, shuffleEnabled, repeatMode, playbackSpeed, loopStart, loopEnabled,
                    isFavorite, sleepTimerActive, onToggleShuffle, onCycleRepeat, onToggleFavorite,
                    onToggleLoop, onOpenLoopPresets, onCycleSpeed, onOpenSleepTimer, onOpenEqualizer,
                    onOpenGarage, onOpenQueue, onOpenZen
                )
            }
        }
    }
}

@Composable
private fun PlayerToolsGrid(
    accentColor: Color,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    playbackSpeed: Float,
    loopStart: Long?,
    loopEnabled: Boolean,
    isFavorite: Boolean,
    sleepTimerActive: Boolean,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleLoop: () -> Unit,
    onOpenLoopPresets: () -> Unit,
    onCycleSpeed: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenGarage: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenZen: () -> Unit
) {
    val tools = listOf(
        Triple(
            if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
            when (repeatMode) {
                Player.REPEAT_MODE_ONE -> "Repeat 1"
                Player.REPEAT_MODE_ALL -> "Repeat all"
                else -> "Repeat"
            },
            repeatMode != Player.REPEAT_MODE_OFF
        ) to onCycleRepeat,
        Triple(Icons.Default.Loop, if (loopStart != null && !loopEnabled) "Set B" else "A–B loop", loopStart != null) to onToggleLoop,
        Triple(Icons.Default.Bookmarks, "Saved loops", false) to onOpenLoopPresets,
        Triple(Icons.Default.Speed, "${playbackSpeed}×", playbackSpeed != 1f) to onCycleSpeed,
        Triple(Icons.Default.Timer, "Sleep", sleepTimerActive) to onOpenSleepTimer,
        Triple(Icons.Default.Tune, "Equalizer", false) to onOpenEqualizer,
        Triple(Icons.Default.Build, "Garage", false) to onOpenGarage,
        Triple(Icons.AutoMirrored.Filled.QueueMusic, "Queue", false) to onOpenQueue,
        Triple(Icons.Default.Brightness3, "Zen", false) to onOpenZen
    )
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tools.chunked(3).forEach { rowTools ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowTools.forEach { (tool, action) ->
                    val (icon, label, active) = tool
                    PlayerTool(icon, label, active, accentColor, action, if (label.contains("loop", true) || label == "Set B") onOpenLoopPresets else null, Modifier.weight(1f))
                }
                repeat(3 - rowTools.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerTool(
    icon: ImageVector,
    label: String,
    active: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = if (active) 0.10f else 0.045f))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, label, tint = if (active) accentColor else Color.White.copy(alpha = 0.72f), modifier = Modifier.size(21.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun formatDurationFull(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.US, "%d:%02d", minutes, seconds)
}

@Composable
fun PremiumProgressBar(
    progress: Long,
    duration: Long,
    loopA: Long? = null,
    loopB: Long? = null,
    onSeek: (Long) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val safeDuration = duration.coerceAtLeast(1L)
    val totalDuration = safeDuration.toFloat()
    val playbackProgress = progress.coerceIn(0L, safeDuration).toFloat()
    val currentProgress = if (isDragging) dragProgress else playbackProgress
    val fraction = (currentProgress / totalDuration).coerceIn(0f, 1f)

    LaunchedEffect(playbackProgress, isDragging) {
        if (!isDragging) {
            dragProgress = playbackProgress
        }
    }

    Box(
        modifier = modifier
            .height(30.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(30.dp)) {
            val barHeight = 6.dp.toPx()
            val centerY = size.height / 2
            val trackWidth = size.width

            drawRoundRect(
                color = Color(0xFF333333),
                topLeft = Offset(0f, centerY - barHeight / 2),
                size = Size(trackWidth, barHeight),
                cornerRadius = CornerRadius(barHeight / 2)
            )

            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(accentColor, Color(0xFFFFD54F))
                ),
                topLeft = Offset(0f, centerY - barHeight / 2),
                size = Size(trackWidth * fraction, barHeight),
                cornerRadius = CornerRadius(barHeight / 2)
            )

            if (loopA != null) {
                val aX = (loopA.toFloat() / totalDuration) * trackWidth
                drawLine(color = Color.Cyan, start = Offset(aX, centerY - barHeight), end = Offset(aX, centerY + barHeight), strokeWidth = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
            if (loopB != null) {
                val bX = (loopB.toFloat() / totalDuration) * trackWidth
                drawLine(color = Color.Red, start = Offset(bX, centerY - barHeight), end = Offset(bX, centerY + barHeight), strokeWidth = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }

            val thumbX = trackWidth * fraction
            drawCircle(color = accentColor.copy(alpha = 0.3f), radius = 10.dp.toPx(), center = Offset(thumbX, centerY))
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(thumbX, centerY))
        }

        Slider(
            value = currentProgress,
            onValueChange = {
                isDragging = true
                dragProgress = it.coerceIn(0f, totalDuration)
            },
            onValueChangeFinished = {
                isDragging = false
                onSeek(dragProgress.toLong())
            },
            valueRange = 0f..totalDuration,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )
    }
}

@Composable
fun ZenModeView(
    currentSong: Song?,
    isPlaying: Boolean,
    progress: Long,
    duration: Long,
    currentLyrics: ParsedLyrics?,
    isLyricsLoading: Boolean,
    animatedAccentColor: Color,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onExitZen: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val calendar = Calendar.getInstance()
    val timeFormat = remember { java.text.SimpleDateFormat("hh:mm", Locale.US) }
    val amPmFormat = remember { java.text.SimpleDateFormat("a", Locale.US) }
    val dateFormat = remember { java.text.SimpleDateFormat("EEEE, d MMMM", Locale.US) }

    var currentTimeString by remember { mutableStateOf(timeFormat.format(calendar.time)) }
    var amPmString by remember { mutableStateOf(amPmFormat.format(calendar.time)) }
    var currentDateString by remember { mutableStateOf(dateFormat.format(calendar.time)) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            val cal = Calendar.getInstance()
            currentTimeString = timeFormat.format(cal.time)
            amPmString = amPmFormat.format(cal.time)
            currentDateString = dateFormat.format(cal.time)
        }
    }

    // High-visibility luminous glowing accent for dark bedtime environment
    val zenGlowColor = remember(animatedAccentColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(animatedAccentColor.toArgb(), hsv)
        hsv[1] = hsv[1].coerceIn(0.50f, 0.90f)
        hsv[2] = hsv[2].coerceIn(0.80f, 1.0f) // Always bright & readable
        Color(android.graphics.Color.HSVToColor(hsv))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. TOP BAR: Minimal dismiss button at top right
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            BounceIconButton(
                onClick = onExitZen,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit Zen Mode",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 2. CLOCK & DATE (Large Bedside Display)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = currentTimeString,
                    fontSize = 66.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OutfitFontFamily,
                    color = Color.White
                )
                Text(
                    text = amPmString,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = OutfitFontFamily,
                    color = zenGlowColor,
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currentDateString,
                fontFamily = OutfitFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary
            )
        }

        // 3. CENTER STAGE: 3-LINE AMBIENT SYNCED LYRICS (Karaoke Focus)
        val lyrics = currentLyrics
        val lines = lyrics?.lines ?: emptyList()

        val line1: String
        val line2: String
        val line3: String

        if (lines.isNotEmpty()) {
            val idx = if (lyrics?.isSynced == true) {
                LrcParser.findActiveLyricIndex(lines, progress)
            } else {
                val safeDur = duration.coerceAtLeast(1L)
                val progressFraction = (progress.toFloat() / safeDur.toFloat()).coerceIn(0f, 1f)
                (progressFraction * lines.size).toInt().coerceIn(0, lines.lastIndex)
            }

            if (idx < 0) {
                // Intro before first line starts
                line1 = "♪"
                line2 = "♪ • • Instrumental Intro • • ♪"
                line3 = lines[0].content
            } else {
                line1 = if (idx > 0) lines[idx - 1].content else "♪"
                line2 = lines.getOrNull(idx)?.content?.takeIf { it.isNotBlank() } ?: "♪ • • ♪"
                line3 = if (idx < lines.lastIndex) lines[idx + 1].content else "• • •"
            }
        } else {
            line1 = "♪"
            line2 = if (isLyricsLoading) "• • • Syncing lyrics • • •" else "♪ Peaceful Bedtime Session ♪"
            line3 = "♪"
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Line 1: Previous line
            Text(
                text = line1,
                fontFamily = OutfitFontFamily,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.35f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Line 2: Active lyric line (Large, Bold, Luminous Accent)
            AnimatedContent(
                targetState = line2,
                transitionSpec = {
                    (slideInVertically { it / 2 } + fadeIn(tween(250))) togetherWith
                    (slideOutVertically { -it / 2 } + fadeOut(tween(200)))
                },
                label = "ZenActiveLyric"
            ) { activeText ->
                Text(
                    text = activeText,
                    fontFamily = OutfitFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = zenGlowColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Line 3: Next line
            Text(
                text = line3,
                fontFamily = OutfitFontFamily,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.35f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 4. SCRUBBER BAR, THEN SONG TITLE/ARTIST DIRECTLY BELOW IT ("bar er niche song name"!), THEN CONTROLS
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            val safeDur = duration.coerceAtLeast(1L)
            val progressFraction = (progress.toFloat() / safeDur.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = zenGlowColor,
                trackColor = Color.White.copy(alpha = 0.15f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDurationFull(progress),
                    color = TextSecondary,
                    fontFamily = OutfitFontFamily,
                    fontSize = 11.sp
                )
                Text(
                    text = formatDurationFull(duration),
                    color = TextSecondary,
                    fontFamily = OutfitFontFamily,
                    fontSize = 11.sp
                )
            }

            // DIRECTLY BELOW THE BAR: Song Title & Artist ("bar er niche song name"!)
            Spacer(modifier = Modifier.height(16.dp))
            MarqueeTitleText(
                text = currentSong?.title ?: "No Song Playing",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                alignStart = false
            )
            Spacer(modifier = Modifier.height(4.dp))
            MarqueeTitleText(
                text = currentSong?.artist ?: "Unknown Artist",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = zenGlowColor,
                alignStart = false
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(0.75f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BounceIconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(48.dp),
                    scaleDown = 0.8f
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(30.dp)
                    )
                }

                val zenButtonColor = if (animatedAccentColor == Color.Unspecified || animatedAccentColor.alpha < 0.2f) SoftGold else animatedAccentColor
                val zenLightBg = (0.299f * zenButtonColor.red + 0.587f * zenButtonColor.green + 0.114f * zenButtonColor.blue) > 0.45f
                val zenIconTint = if (zenLightBg) Color(0xFF121318) else Color.White
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(zenButtonColor)
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClickLabel = if (isPlaying) "Pause" else "Play"
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPlayPause()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = zenIconTint,
                        modifier = Modifier.size(34.dp)
                    )
                }

                BounceIconButton(
                    onClick = onNext,
                    modifier = Modifier.size(48.dp),
                    scaleDown = 0.8f
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}
