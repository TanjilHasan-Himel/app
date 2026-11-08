package com.eplaytime.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.eplaytime.app.R
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.ui.theme.*
import com.eplaytime.app.ui.viewmodel.MusicViewModel
import java.util.Locale
import org.burnoutcrew.reorderable.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    onNavigateBack: () -> Unit,
    viewModel: MusicViewModel
) {
    val songList by viewModel.songList.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Queue",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = BlackBackground,
        contentWindowInsets = WindowInsets.systemBars
    ) { paddingValues ->
        if (songList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Queue is empty",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Light,
                    color = TextSecondary,
                    fontSize = 16.sp
                )
            }
        } else {
            val currentIndex = songList.indexOfFirst { it.id == currentSong?.id }
            val upNext = if (currentIndex >= 0 && currentIndex < songList.size - 1) {
                songList.subList(currentIndex + 1, songList.size)
            } else {
                songList
            }

            var upNextState by remember(upNext) { mutableStateOf(upNext) }
            val offset = if (currentSong != null) 2 else 1
            
            val state = rememberReorderableLazyListState(onMove = { from, to ->
                val mappedFrom = from.index - offset
                val mappedTo = to.index - offset
                
                if (mappedFrom in upNextState.indices && mappedTo in upNextState.indices) {
                    upNextState = upNextState.toMutableList().apply {
                        add(mappedTo, removeAt(mappedFrom))
                    }
                }
            }, canDragOver = { draggedOver, _ ->
                draggedOver.key != "header" && draggedOver.key != "now_playing" && draggedOver.key != "footer"
            }, onDragEnd = { startIndex, endIndex ->
                val mappedFrom = startIndex - offset
                val mappedTo = endIndex - offset
                
                if (mappedFrom in upNextState.indices && mappedTo in upNextState.indices) {
                    val actualFrom = currentIndex + 1 + mappedFrom
                    val actualTo = currentIndex + 1 + mappedTo
                    if (actualFrom != actualTo) {
                        viewModel.moveQueueItem(actualFrom, actualTo)
                    }
                }
            })

            LazyColumn(
                state = state.listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .reorderable(state),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { viewModel.removeUpcomingDuplicates() },
                            label = { Text("De-dup", fontFamily = OutfitFontFamily, fontSize = 11.sp) }
                        )
                        AssistChip(
                            onClick = { viewModel.autoCleanQueue(removeShortAudio = true, removeCallRecordings = true) },
                            label = { Text("Auto Clean", fontFamily = OutfitFontFamily, fontSize = 11.sp) }
                        )
                    }
                }

                currentSong?.let { song ->
                    item(key = "now_playing") {
                        Text(
                            text = "NOW PLAYING",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Light,
                            fontSize = 12.sp,
                            color = SoftGold,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        QueueItem(
                            song = song,
                            isCurrentlyPlaying = true,
                            onClick = { }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "UP NEXT",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Light,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                itemsIndexed(upNextState, key = { _, song -> song.id }) { index, song ->
                    ReorderableItem(state, key = song.id) { isDragging ->
                        val actualIndex = currentIndex + 1 + index
                        val elevation = if (isDragging) 8.dp else 0.dp
                        
                        QueueItem(
                            song = song,
                            isCurrentlyPlaying = false,
                            onRemove = { viewModel.removeFromQueue(actualIndex) },
                            onPlayNext = { viewModel.playNextFromQueue(song.uri) },
                            onPlayLater = { viewModel.playLaterFromQueue(song.uri) },
                            onClick = { viewModel.playSong(song.uri) },
                            modifier = Modifier
                                .detectReorderAfterLongPress(state)
                        )
                    }
                }

                item(key = "footer") {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun QueueItem(
    song: Song,
    isCurrentlyPlaying: Boolean,
    onRemove: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onPlayLater: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isCurrentlyPlaying) {
                    Brush.horizontalGradient(
                        colors = listOf(SoftGold.copy(alpha = 0.15f), BlackSurface)
                    )
                } else {
                    Brush.horizontalGradient(listOf(BlackSurface, BlackSurface))
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.albumArtUri ?: R.drawable.audia_player_logo,
            contentDescription = "Album Art",
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BlackCard),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontFamily = OutfitFontFamily,
                fontWeight = if (isCurrentlyPlaying) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 14.sp,
                color = if (isCurrentlyPlaying) SoftGold else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formatDuration(song.duration),
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 11.sp,
            color = TextTertiary
        )

        if (onRemove != null && !isCurrentlyPlaying) {
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Song options",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(BlackSurface)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text("Play Next", fontFamily = OutfitFontFamily, fontSize = 13.sp, color = TextPrimary)
                        },
                        onClick = {
                            showMenu = false
                            onPlayNext?.invoke()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text("Play Later", fontFamily = OutfitFontFamily, fontSize = 13.sp, color = TextPrimary)
                        },
                        onClick = {
                            showMenu = false
                            onPlayLater?.invoke()
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    DropdownMenuItem(
                        text = {
                            Text("Remove from Queue", fontFamily = OutfitFontFamily, fontSize = 13.sp, color = Color.Red.copy(alpha = 0.85f))
                        },
                        onClick = {
                            showMenu = false
                            onRemove.invoke()
                        }
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                tint = TextSecondary.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(24.dp)
                    .padding(start = 2.dp)
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, remainingSeconds)
}