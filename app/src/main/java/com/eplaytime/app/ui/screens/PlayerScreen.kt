package com.eplaytime.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.eplaytime.app.ui.components.AudioEffectsSheet
import com.eplaytime.app.ui.components.AuraVisualizer
import com.eplaytime.app.ui.components.PlayerControls
import com.eplaytime.app.ui.components.FavoriteButton
import com.eplaytime.app.util.VisualizerHelper
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.withContext
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToQueue: () -> Unit = {},
    viewModel: MusicViewModel
) {
    val currentSong by viewModel.currentSong.collectAsState()
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

    var toastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            kotlinx.coroutines.delay(500)
            toastMessage = null
        }
    }
    val visualizerStyle by viewModel.visualizerStyle.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val stopAfterCurrent by viewModel.stopAfterCurrent.collectAsState()
    val fadeOnPlayPause by viewModel.fadeOnPlayPause.collectAsState()
    val sleepTimerRemainingMs by viewModel.sleepTimerRemainingMs.collectAsState()
    val loopPresets by viewModel.loopPresetsForCurrentSong.collectAsState()
    var showSleepDialog by remember { mutableStateOf(false) }
    var showStopDialog by remember { mutableStateOf(false) }
    var sleepMinutesInput by remember { mutableStateOf("30") }
    var showAudioEffectsSheet by remember { mutableStateOf(false) }

    var showSettingsDialog by remember { mutableStateOf(false) } 
    var isZenModeEnabled by remember { mutableStateOf(false) }

    val context = LocalContext.current
    
    var dynamicAccentColor by remember { mutableStateOf(SoftGold) }
    var dynamicBgColor by remember { mutableStateOf(Color.Black) }

    LaunchedEffect(currentSong?.albumArtUri) {
        dynamicAccentColor = SoftGold
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

                palette.vibrantSwatch?.rgb?.let { dynamicAccentColor = Color(it) }
                    ?: palette.dominantSwatch?.rgb?.let { dynamicAccentColor = Color(it) }

                palette.darkVibrantSwatch?.rgb?.let { dynamicBgColor = Color(it) }
                    ?: palette.darkMutedSwatch?.rgb?.let { dynamicBgColor = Color(it) }
            }
        }
    }

    val animatedAccentColor by animateColorAsState(targetValue = dynamicAccentColor, tween(1000))
    val animatedBgColor by animateColorAsState(targetValue = dynamicBgColor, tween(1000))

    if (currentSong == null) {
        SideEffect { onNavigateBack() }
        return
    }

    var vinylRotation by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                withFrameMillis {
                    vinylRotation += 0.5f
                    if (vinylRotation >= 360f) vinylRotation -= 360f
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(animatedBgColor, Color.Black)))
    ) {
        val albumArtModel = remember(currentSong?.albumArtUri) {
            val artUri = currentSong?.albumArtUri
            if (!artUri.isNullOrBlank()) {
                ImageRequest.Builder(context)
                    .data(artUri)
                    .crossfade(true)
                    .placeholder(R.drawable.audia_player_logo)
                    .error(R.drawable.audia_player_logo)
                    .fallback(R.drawable.audia_player_logo)
                    .build()
            } else {
                R.drawable.audia_player_logo
            }
        }

        SubcomposeAsyncImage(
            model = albumArtModel,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(80.dp)
                .alpha(0.4f),
            contentScale = ContentScale.Crop,
            loading = {
                Image(
                    painter = painterResource(R.drawable.audia_player_logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.2f
                )
            },
            error = {
                Image(
                    painter = painterResource(R.drawable.audia_player_logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.2f
                )
            }
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isZenModeEnabled) 0.3f else 0.4f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isZenModeEnabled) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { isZenModeEnabled = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brightness3,
                                contentDescription = "Zen Mode",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "NOW PLAYING",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Light,
                            fontSize = 12.sp,
                            letterSpacing = 3.sp,
                            color = TextSecondary
                        )
                    }
                    
                    IconButton(onClick = onNavigateToQueue) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Queue",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                val coverSize = (screenWidth * 0.8f)
                    .coerceAtMost(screenHeight * 0.45f)
                    .coerceIn(240.dp, 350.dp)
                    
                val animatedScale by animateFloatAsState(
                    targetValue = if (isPlaying) 1f else 0.9f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "AlbumArtScale"
                )
                
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(coverSize)
                            .scale(animatedScale)
                            .graphicsLayer { rotationZ = vinylRotation }
                            .shadow(24.dp, CircleShape, spotColor = animatedAccentColor, ambientColor = animatedAccentColor.copy(alpha = 0.5f))
                            .border(4.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                            .clip(CircleShape),
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
                        Box(
                            modifier = Modifier
                                .size(coverSize * 0.15f)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color.Black)
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentSong?.title ?: "Unknown Title",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentSong?.artist ?: "Unknown Artist",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 18.sp,
                        color = animatedAccentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

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

                Box(modifier = Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = toastMessage != null,
                        enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(50)),
                        exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200))
                    ) {
                        Box(
                            modifier = Modifier
                                .wrapContentSize(unbounded = true)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(toastMessage ?: "", color = animatedAccentColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = OutfitFontFamily)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { 
                        viewModel.toggleShuffle() 
                        toastMessage = if (!shuffleEnabled) "Shuffle Enabled" else "Shuffle Disabled"
                    }) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = if (shuffleEnabled) animatedAccentColor else TextTertiary)
                    }

                    IconButton(onClick = { 
                        viewModel.toggleABLoop()
                        val msg = when {
                            loopEnabled -> "A-B Loop Cleared"
                            loopStart != null -> "Loop End (B) Set"
                            else -> "Loop Start (A) Set"
                        }
                        toastMessage = msg
                    }) {
                        val loopText = when {
                            loopEnabled -> "A-B"
                            loopStart != null -> "A"
                            else -> "A-B"
                        }
                        val loopColor = when {
                            loopEnabled -> animatedAccentColor
                            loopStart != null -> animatedAccentColor.copy(alpha = 0.6f)
                            else -> TextTertiary
                        }
                        Text(loopText, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = loopColor)
                    }

                    IconButton(onClick = { 
                        viewModel.cycleRepeatMode() 
                        val nextMode = when(repeatMode) {
                            Player.REPEAT_MODE_OFF -> "Repeat All"
                            Player.REPEAT_MODE_ALL -> "Repeat One"
                            else -> "Repeat Off"
                        }
                        toastMessage = nextMode
                    }) {
                        val repeatIcon = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                        Icon(repeatIcon, contentDescription = "Repeat", tint = if (repeatMode != Player.REPEAT_MODE_OFF) animatedAccentColor else TextTertiary)
                    }

                    val isFavorite = currentSong?.let { song -> allFavorites.any { it.uri == song.uri } } ?: false
                    FavoriteButton(
                        isFavorite = isFavorite,
                        onToggle = { 
                            currentSong?.let { 
                                viewModel.toggleFavorite(it) 
                                toastMessage = if (isFavorite) "Removed from Favorites" else "Added to Favorites"
                            } 
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { showSleepDialog = true }) {
                            Icon(Icons.Default.Bedtime, contentDescription = "Sleep", tint = if (sleepTimerRemainingMs != null) animatedAccentColor else TextSecondary)
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { showAudioEffectsSheet = true }) {
                            Icon(Icons.Default.Tune, contentDescription = "FX", tint = animatedAccentColor)
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { 
                            viewModel.cyclePlaybackSpeed() 
                            val speedStr = when(playbackSpeed) {
                                1.0f -> "1.25x"
                                1.25f -> "1.5x"
                                1.5f -> "2.0x"
                                2.0f -> "0.5x"
                                0.5f -> "0.75x"
                                0.75f -> "1.0x"
                                else -> "1.0x"
                            }
                            toastMessage = "Speed: $speedStr"
                        }) {
                            Icon(Icons.Default.Speed, contentDescription = "Speed", tint = animatedAccentColor)
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { 
                            viewModel.toggleFadeOnPlayPause() 
                            toastMessage = if (!fadeOnPlayPause) "Fade Enabled" else "Fade Disabled"
                        }) {
                            Icon(Icons.Default.BlurOn, contentDescription = "Fade", tint = if (fadeOnPlayPause) animatedAccentColor else TextSecondary)
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { 
                            viewModel.saveCurrentLoopPreset() 
                            toastMessage = "Loop Preset Saved"
                        }, enabled = loopStart != null && loopEnd != null) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = "Loop+", tint = if (loopStart != null && loopEnd != null) animatedAccentColor else TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                PlayerControls(
                    isPlaying = isPlaying,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onPrevious = { viewModel.playPrevious() },
                    onNext = { viewModel.playNext() },
                    onSeekBackward = { viewModel.seekTo((progress - 5000).coerceAtLeast(0)) },
                    onSeekForward = { viewModel.seekTo((progress + 5000).coerceAtMost(duration)) }
                )
            } 
            if (isZenModeEnabled) {
                Spacer(modifier = Modifier.weight(1f))
                
                val calendar = Calendar.getInstance()
                val timeFormat = java.text.SimpleDateFormat("hh:mm", Locale.US)
                val amPmFormat = java.text.SimpleDateFormat("a", Locale.US)
                var currentTimeString by remember { mutableStateOf(timeFormat.format(calendar.time)) }
                var amPmString by remember { mutableStateOf(amPmFormat.format(calendar.time)) }

                LaunchedEffect(Unit) {
                    while(true) {
                        kotlinx.coroutines.delay(1000)
                        val cal = Calendar.getInstance()
                        currentTimeString = timeFormat.format(cal.time)
                        amPmString = amPmFormat.format(cal.time)
                    }
                }
                
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = currentTimeString,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OutfitFontFamily,
                        color = Color.White
                    )
                    Text(
                        text = amPmString,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = OutfitFontFamily,
                        color = animatedAccentColor,
                        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                TextButton(onClick = { isZenModeEnabled = false }) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                    Spacer(Modifier.width(8.dp))
                    Text("Exit Zen Mode", color = Color.White.copy(alpha = 0.7f), fontFamily = OutfitFontFamily)
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
                                Text(if (stopAfterSong) "Active ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â will stop when song ends" else "Pause when current song finishes", fontFamily = OutfitFontFamily, fontSize = 12.sp, color = TextSecondary)
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
                        viewModel.startSleepTimer(minutes = mins, fadeOut = true)
                        showSleepDialog = false
                    }) { Text("Start Timer") }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
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
