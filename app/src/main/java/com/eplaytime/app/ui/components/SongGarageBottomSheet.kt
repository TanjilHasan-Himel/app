package com.eplaytime.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.eplaytime.app.R
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.ui.theme.*
import com.eplaytime.app.ui.viewmodel.MusicViewModel
import com.eplaytime.app.util.AudioEffectManager
import com.eplaytime.app.util.AudioInspectorHelper
import java.io.File
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongGarageBottomSheet(
    song: Song,
    viewModel: MusicViewModel,
    onDismiss: () -> Unit,
    onOpenWiki: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Trimmer", "Tag Studio", "Sound Tweaker", "Tech Specs")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141414),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = SoftGold.copy(alpha = 0.4f),
                width = 36.dp,
                height = 4.dp
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SoftGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = SoftGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Song Garage",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = song.title,
                            fontFamily = OutfitFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Tabs - Scrollable to prevent any awkward multi-line text wrapping across screen sizes
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = SoftGold,
                edgePadding = 4.dp,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = SoftGold,
                            height = 2.5.dp
                        )
                    }
                },
                divider = {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontFamily = OutfitFontFamily,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = if (selectedTab == index) SoftGold else TextSecondary
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            when (selectedTab) {
                0 -> WaveformTrimmerTab(song = song, viewModel = viewModel, context = context)
                1 -> TagStudioTab(song = song, viewModel = viewModel, context = context)
                2 -> SoundTweakerTab(viewModel = viewModel)
                3 -> TechSpecsTab(song = song, context = context, viewModel = viewModel, onOpenWiki = onOpenWiki)
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 0: WAVEFORM TRIMMER & RINGTONE/ALARM CUTTER
// -------------------------------------------------------------
@Composable
private fun WaveformTrimmerTab(
    song: Song,
    viewModel: MusicViewModel,
    context: Context
) {
    val totalDurationMs = song.duration.coerceAtLeast(1000L)
    var trimStartFraction by remember { mutableFloatStateOf(0.15f) }
    var trimEndFraction by remember { mutableFloatStateOf(0.65f) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    val startMs = (trimStartFraction * totalDurationMs).toLong()
    val endMs = (trimEndFraction * totalDurationMs).toLong()
    val selectionDurationSec = ((endMs - startMs) / 1000).coerceAtLeast(0)

    val currentPos by viewModel.progress.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    // Loop playback logic within selection
    LaunchedEffect(currentPos, isPreviewPlaying) {
        if (isPreviewPlaying && isPlaying) {
            if (currentPos >= endMs || currentPos < startMs) {
                viewModel.seekTo(startMs)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Selection Info Badge
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ContentCut,
                contentDescription = null,
                tint = SoftGold,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${formatSeconds(startMs)}  ➔  ${formatSeconds(endMs)}  (${selectionDurationSec}s)",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = SoftGold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Waveform Canvas with Draggable Handles
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
        ) {
            val canvasWidthPx = constraints.maxWidth.toFloat()
            val canvasHeightPx = constraints.maxHeight.toFloat()

            // Pre-calculate 50 waveform bar heights
            val bars = remember(song.id) {
                List(50) { i ->
                    val angle = (i * 0.35f)
                    val base = kotlin.math.abs(sin(angle) * 0.7f + 0.25f)
                    base.coerceIn(0.15f, 0.95f)
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val barWidth = canvasWidthPx / (bars.size * 1.6f)
                val spacing = barWidth * 0.6f
                val totalBarsWidth = bars.size * (barWidth + spacing)
                val startOffset = (canvasWidthPx - totalBarsWidth) / 2f

                // Draw background bars
                bars.forEachIndexed { i, factor ->
                    val x = startOffset + i * (barWidth + spacing)
                    val barHeight = canvasHeightPx * factor * 0.75f
                    val y = (canvasHeightPx - barHeight) / 2f
                    val normalizedX = (x / canvasWidthPx).coerceIn(0f, 1f)

                    val isInsideSelection = normalizedX in trimStartFraction..trimEndFraction
                    val barColor = if (isInsideSelection) SoftGold else Color.White.copy(alpha = 0.25f)

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }

                // Draw Active Trim Region Highlight
                val selStartPx = trimStartFraction * canvasWidthPx
                val selEndPx = trimEndFraction * canvasWidthPx
                drawRect(
                    color = SoftGold.copy(alpha = 0.12f),
                    topLeft = Offset(selStartPx, 0f),
                    size = Size(selEndPx - selStartPx, canvasHeightPx)
                )

                // Start Marker Line
                drawLine(
                    color = SoftGold,
                    start = Offset(selStartPx, 0f),
                    end = Offset(selStartPx, canvasHeightPx),
                    strokeWidth = 3f
                )

                // End Marker Line
                drawLine(
                    color = SoftGold,
                    start = Offset(selEndPx, 0f),
                    end = Offset(selEndPx, canvasHeightPx),
                    strokeWidth = 3f
                )
            }

            // Draggable Start Handle
            Box(
                modifier = Modifier
                    .offset(x = (trimStartFraction * (canvasWidthPx / context.resources.displayMetrics.density) - 14).dp, y = 8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(SoftGold)
                    .pointerInput(canvasWidthPx) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newFraction = (trimStartFraction + (dragAmount.x / canvasWidthPx))
                                .coerceIn(0f, trimEndFraction - 0.05f)
                            trimStartFraction = newFraction
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            }

            // Draggable End Handle
            Box(
                modifier = Modifier
                    .offset(x = (trimEndFraction * (canvasWidthPx / context.resources.displayMetrics.density) - 14).dp, y = (130 - 36).dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(SoftGold)
                    .pointerInput(canvasWidthPx) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newFraction = (trimEndFraction + (dragAmount.x / canvasWidthPx))
                                .coerceIn(trimStartFraction + 0.05f, 1f)
                            trimEndFraction = newFraction
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Loop Preview Play/Pause Button
        Button(
            onClick = {
                if (isPreviewPlaying && isPlaying) {
                    viewModel.togglePlayPause()
                    isPreviewPlaying = false
                } else {
                    viewModel.seekTo(startMs)
                    if (!isPlaying) viewModel.togglePlayPause()
                    isPreviewPlaying = true
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPreviewPlaying && isPlaying) SoftGold else Color.White.copy(alpha = 0.1f),
                contentColor = if (isPreviewPlaying && isPlaying) Color.Black else Color.White
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.height(44.dp)
        ) {
            Icon(
                imageVector = if (isPreviewPlaying && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isPreviewPlaying && isPlaying) "Pause Preview" else "Play Trimmed Loop",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Actions: Set Ringtone & Set Alarm
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Set as Ringtone
            OutlinedButton(
                onClick = {
                    setSongAsRingtone(context, song, viewModel)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(SoftGold.copy(alpha = 0.6f), SoftGold)))
            ) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = SoftGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Set Ringtone", fontFamily = OutfitFontFamily, fontSize = 13.sp)
            }

            // Set as Alarm
            OutlinedButton(
                onClick = {
                    setSongAsAlarm(context, song, viewModel)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.4f))))
            ) {
                Icon(Icons.Default.Alarm, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Set Alarm", fontFamily = OutfitFontFamily, fontSize = 13.sp)
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: ID3 TAG & COVER ART STUDIO
// -------------------------------------------------------------
@Composable
private fun TagStudioTab(
    song: Song,
    viewModel: MusicViewModel,
    context: Context
) {
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }
    var year by remember { mutableStateOf("") }
    var albumArtUri by remember { mutableStateOf(song.albumArtUri) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            albumArtUri = uri.toString()
            viewModel.showInAppNotification("Cover artwork selected!")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cover Art Preview + Change Button
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(BlackSurface)
                .border(1.dp, SoftGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .clickable { imagePickerLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(albumArtUri ?: R.drawable.audia_player_logo)
                    .crossfade(true)
                    .build(),
                contentDescription = "Cover Art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Change Art",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Text(
            text = "Tap image to choose new cover art",
            fontFamily = OutfitFontFamily,
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Title Field
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title", fontFamily = OutfitFontFamily, color = TextSecondary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SoftGold,
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Artist Field
        OutlinedTextField(
            value = artist,
            onValueChange = { artist = it },
            label = { Text("Artist", fontFamily = OutfitFontFamily, color = TextSecondary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SoftGold,
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Album Field
        OutlinedTextField(
            value = album,
            onValueChange = { album = it },
            label = { Text("Album", fontFamily = OutfitFontFamily, color = TextSecondary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SoftGold,
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Year Field
        OutlinedTextField(
            value = year,
            onValueChange = { year = it.filter { ch -> ch.isDigit() }.take(4) },
            label = { Text("Year", fontFamily = OutfitFontFamily, color = TextSecondary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SoftGold,
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Save Tags Button
        Button(
            onClick = {
                viewModel.updateSongTags(
                    songId = song.id,
                    newTitle = title,
                    newArtist = artist,
                    newAlbum = album,
                    newArtUri = albumArtUri
                )
                viewModel.showInAppNotification("Tags updated successfully!")
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = SoftGold,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Save Metadata Tags",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

// -------------------------------------------------------------
// TAB 2: STUDIO SOUND TWEAKER
// -------------------------------------------------------------
@Composable
private fun SoundTweakerTab(
    viewModel: MusicViewModel
) {
    val currentSpeed by viewModel.playbackSpeed.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Speed Display Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Playback Rate",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = "Adjust tempo without changing audio pitch",
                    fontFamily = OutfitFontFamily,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Text(
                text = "%.2fx".format(currentSpeed),
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = SoftGold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Speed Slider
        Slider(
            value = currentSpeed,
            onValueChange = { viewModel.setPlaybackSpeed(it) },
            valueRange = 0.5f..2.0f,
            steps = 29, // 0.05 step
            colors = SliderDefaults.colors(
                thumbColor = SoftGold,
                activeTrackColor = SoftGold,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Studio Sound Presets",
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Color.White,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Preset 1: Slowed + Reverb
        PresetCard(
            title = "Slowed + Reverb",
            subtitle = "Tempo 0.85x with deep spatial presence",
            isActive = kotlin.math.abs(currentSpeed - 0.85f) < 0.02f,
            onClick = {
                viewModel.setPlaybackSpeed(0.85f)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Preset 2: Nightcore
        PresetCard(
            title = "Nightcore Beat",
            subtitle = "Tempo 1.25x high-energy fast pace",
            isActive = kotlin.math.abs(currentSpeed - 1.25f) < 0.02f,
            onClick = {
                viewModel.setPlaybackSpeed(1.25f)
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Preset 3: Vocal Focus
        PresetCard(
            title = "Vocal Clarity",
            subtitle = "1.00x normal tempo with vocal curve",
            isActive = false,
            onClick = {
                viewModel.setPlaybackSpeed(1.0f)
                AudioEffectManager.applyPreset("Vocal")
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Preset 4: Reset Normal
        PresetCard(
            title = "Original Master (Reset)",
            subtitle = "Standard 1.00x studio playback",
            isActive = kotlin.math.abs(currentSpeed - 1.00f) < 0.02f,
            onClick = {
                viewModel.setPlaybackSpeed(1.0f)
            }
        )
    }
}

@Composable
private fun PresetCard(
    title: String,
    subtitle: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isActive) SoftGold.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
            .border(
                1.dp,
                if (isActive) SoftGold.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = if (isActive) SoftGold else Color.White
            )
            Text(
                text = subtitle,
                fontFamily = OutfitFontFamily,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Icon(
            imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isActive) SoftGold else TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

// -------------------------------------------------------------
// TAB 3: TECH SPECS INSPECTOR
// -------------------------------------------------------------
@Composable
private fun TechSpecsTab(
    song: Song,
    context: Context,
    viewModel: MusicViewModel,
    onOpenWiki: () -> Unit = {}
) {
    val specs = remember(song.id) {
        AudioInspectorHelper.inspect(context, song)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Audio Stream Architecture",
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Color.White
        )
        Text(
            text = "Hardware codec decoding and container specifications",
            fontFamily = OutfitFontFamily,
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        SpecRow(label = "Format & Codec", value = specs.format)
        SpecRow(label = "Bitrate", value = specs.bitrateKbps)
        SpecRow(label = "Sample Rate", value = specs.sampleRateHz)
        SpecRow(label = "Channels", value = specs.channels)
        SpecRow(label = "Duration", value = specs.durationFormatted)
        SpecRow(label = "File Size", value = specs.fileSizeFormatted)

        Spacer(modifier = Modifier.height(12.dp))

        // Song Wiki & Origins Shortcut Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SoftGold.copy(alpha = 0.10f))
                .border(1.dp, SoftGold.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
                .clickable { onOpenWiki() }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = SoftGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Song Wiki & Origins",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Behind the music story & credits",
                            fontFamily = OutfitFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // File Path with Copy Action
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .clickable {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("File Path", specs.filePath))
                    viewModel.showInAppNotification("Path copied to clipboard!")
                }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Storage Location",
                    fontFamily = OutfitFontFamily,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Path",
                    tint = SoftGold,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = specs.filePath,
                fontFamily = OutfitFontFamily,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = OutfitFontFamily,
            fontSize = 13.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Color.White
        )
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
}

// -------------------------------------------------------------
// HELPER METHODS
// -------------------------------------------------------------
private fun formatSeconds(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}

private fun setSongAsRingtone(context: Context, song: Song, viewModel: MusicViewModel) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:" + context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            viewModel.showInAppNotification("Grant 'Modify system settings' permission to set ringtone")
            return
        }

        val uri = Uri.parse(song.uri)
        RingtoneManager.setActualDefaultRingtoneUri(
            context,
            RingtoneManager.TYPE_RINGTONE,
            uri
        )
        viewModel.showInAppNotification("Ringtone set to '${song.title}'!")
    } catch (e: Exception) {
        viewModel.showInAppNotification("Could not set ringtone: ${e.message}")
    }
}

private fun setSongAsAlarm(context: Context, song: Song, viewModel: MusicViewModel) {
    try {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_MESSAGE, song.title)
            putExtra(AlarmClock.EXTRA_RINGTONE, song.uri)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        viewModel.showInAppNotification("Alarm intent opened for '${song.title}'")
    }
}
