package com.eplaytime.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.eplaytime.app.R
import com.eplaytime.app.data.model.Playlist
import com.eplaytime.app.data.model.PlaylistItem
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.ui.screens.home.RenamePlaylistDialog
import com.eplaytime.app.ui.theme.*
import com.eplaytime.app.ui.viewmodel.MusicViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    val context = LocalContext.current
    val allPlaylists by viewModel.allPlaylists.collectAsState()
    val playlist = remember(allPlaylists, playlistId) {
        allPlaylists.find { it.id == playlistId }
    }

    val itemsFlow = remember(playlistId) { viewModel.getPlaylistItems(playlistId) }
    val items by itemsFlow.collectAsState(initial = emptyList())
    val currentSong by viewModel.currentSong.collectAsState()
    val allLibrarySongs by viewModel.songList.collectAsState()

    var showAddSongsDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // M3U Export Launcher
    val exportM3uLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri: Uri? ->
        if (uri != null && playlist != null) {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    viewModel.exportPlaylistToM3u(
                        playlistId = playlist.id,
                        outputStream = outputStream,
                        onSuccess = {},
                        onError = {}
                    )
                }
            } catch (e: Exception) {
                viewModel.showInAppNotification("Export failed: ${e.localizedMessage}")
            }
        }
    }

    Scaffold(
        containerColor = BlackBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SoftGold
                    )
                }

                Text(
                    text = playlist?.name ?: "Playlist",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showAddSongsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Songs",
                            tint = SoftGold
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = TextSecondary
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .background(BlackSurface)
                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export to .M3U", fontFamily = OutfitFontFamily, color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null, tint = SoftGold) },
                                onClick = {
                                    showMenu = false
                                    playlist?.let {
                                        val fileName = "${it.name.replace("[^a-zA-Z0-9-_]".toRegex(), "_")}.m3u"
                                        exportM3uLauncher.launch(fileName)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename", fontFamily = OutfitFontFamily, color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary) },
                                onClick = {
                                    showMenu = false
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Playlist", fontFamily = OutfitFontFamily, color = Color(0xFFE53935)) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935)) },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }

            // Playlist Body
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Card
                item {
                    val totalDurationMs = remember(items) { items.sumOf { it.duration } }
                    val totalDurationText = remember(totalDurationMs) { formatTotalDuration(totalDurationMs) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = BlackSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Hero Art Plate
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(SoftGold.copy(alpha = 0.3f), BlackCard)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                val firstArt = items.firstOrNull { !it.albumArtUri.isNullOrBlank() }?.albumArtUri
                                    ?: playlist?.customArtUri
                                if (!firstArt.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(firstArt)
                                            .size(256, 256)
                                            .crossfade(true)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = SoftGold,
                                        modifier = Modifier.size(52.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = playlist?.name ?: "Playlist",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = TextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            if (!playlist?.description.isNullOrBlank()) {
                                Text(
                                    text = playlist!!.description,
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Light,
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Text(
                                text = "${items.size} songs • $totalDurationText",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Light,
                                fontSize = 12.sp,
                                color = TextTertiary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                            )

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.playPlaylist(
                                            playlistId = playlistId,
                                            items = items,
                                            shuffle = false
                                        )
                                        onNavigateToPlayer()
                                    },
                                    enabled = items.isNotEmpty(),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SoftGold,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Play All",
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.playPlaylist(
                                            playlistId = playlistId,
                                            items = items,
                                            shuffle = true
                                        )
                                        onNavigateToPlayer()
                                    },
                                    enabled = items.isNotEmpty(),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftGold)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Shuffle",
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Empty Track List
                if (items.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "This playlist is empty",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showAddSongsDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SoftGold.copy(alpha = 0.2f),
                                    contentColor = SoftGold
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Songs", fontFamily = OutfitFontFamily, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                        val isPlaying = currentSong?.uri == item.songUri

                        PlaylistTrackRow(
                            item = item,
                            index = index + 1,
                            isPlaying = isPlaying,
                            canMoveUp = index > 0,
                            canMoveDown = index < items.size - 1,
                            onClick = {
                                viewModel.playPlaylist(
                                    playlistId = playlistId,
                                    items = items,
                                    startSongUri = item.songUri
                                )
                                onNavigateToPlayer()
                            },
                            onMoveUp = {
                                if (index > 0) {
                                    val mutable = items.toMutableList()
                                    val temp = mutable[index]
                                    mutable[index] = mutable[index - 1]
                                    mutable[index - 1] = temp
                                    viewModel.reorderPlaylist(playlistId, mutable)
                                }
                            },
                            onMoveDown = {
                                if (index < items.size - 1) {
                                    val mutable = items.toMutableList()
                                    val temp = mutable[index]
                                    mutable[index] = mutable[index + 1]
                                    mutable[index + 1] = temp
                                    viewModel.reorderPlaylist(playlistId, mutable)
                                }
                            },
                            onRemove = {
                                viewModel.removePlaylistItem(playlistId, item.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Songs Picker Dialog
    if (showAddSongsDialog) {
        AddSongsToPlaylistPicker(
            allSongs = allLibrarySongs,
            existingSongUris = items.map { it.songUri }.toSet(),
            onDismiss = { showAddSongsDialog = false },
            onAdd = { selectedSongs ->
                viewModel.addSongsToPlaylist(playlistId, selectedSongs)
                showAddSongsDialog = false
            }
        )
    }

    // Rename Dialog
    if (showRenameDialog && playlist != null) {
        RenamePlaylistDialog(
            initialName = playlist.name,
            onDismiss = { showRenameDialog = false },
            onRename = { newName ->
                viewModel.renamePlaylist(playlist.id, newName)
                showRenameDialog = false
            }
        )
    }

    // Delete Dialog
    if (showDeleteDialog && playlist != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = BlackSurface,
            title = {
                Text("Delete Playlist?", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    "Are you sure you want to delete '${playlist.name}'? Audio files on device will not be removed.",
                    fontFamily = OutfitFontFamily,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlaylist(playlist.id)
                        showDeleteDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Delete", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", fontFamily = OutfitFontFamily, color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun PlaylistTrackRow(
    item: PlaylistItem,
    index: Int,
    isPlaying: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isPlaying) SoftGold.copy(alpha = 0.12f) else BlackSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track number
        Text(
            text = "$index",
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = if (isPlaying) SoftGold else TextTertiary,
            modifier = Modifier.width(24.dp)
        )

        // Thumbnail Art
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.albumArtUri ?: R.drawable.audia_player_logo)
                .size(96, 96)
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BlackCard),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Title & Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontFamily = OutfitFontFamily,
                fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 15.sp,
                color = if (isPlaying) SoftGold else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.artist,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Reorder Up / Down Controls
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Move Up",
                    tint = if (canMoveUp) TextSecondary else Color.Transparent,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Move Down",
                    tint = if (canMoveDown) TextSecondary else Color.Transparent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Remove Track Button
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = TextTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun AddSongsToPlaylistPicker(
    allSongs: List<Song>,
    existingSongUris: Set<String>,
    onDismiss: () -> Unit,
    onAdd: (List<Song>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedSongs = remember { mutableStateListOf<Song>() }

    val filtered = remember(allSongs, searchQuery) {
        val q = searchQuery.trim().lowercase(Locale.ROOT)
        allSongs.filter { song ->
            q.isBlank() || song.title.lowercase(Locale.ROOT).contains(q) || song.artist.lowercase(Locale.ROOT).contains(q)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BlackSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Text(
                    text = "Add Songs to Playlist",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search title, artist...", color = TextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftGold,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = SoftGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Song selection list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(filtered, key = { _, s -> s.id }) { _, song ->
                        val alreadyInPlaylist = existingSongUris.contains(song.uri)
                        val isSelected = selectedSongs.contains(song)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) SoftGold.copy(alpha = 0.15f) else BlackCard)
                                .clickable(enabled = !alreadyInPlaylist) {
                                    if (isSelected) {
                                        selectedSongs.remove(song)
                                    } else {
                                        selectedSongs.add(song)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected || alreadyInPlaylist,
                                onCheckedChange = { checked ->
                                    if (!alreadyInPlaylist) {
                                        if (checked) selectedSongs.add(song) else selectedSongs.remove(song)
                                    }
                                },
                                enabled = !alreadyInPlaylist,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = SoftGold,
                                    checkmarkColor = Color.Black
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = if (alreadyInPlaylist) TextTertiary else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (alreadyInPlaylist) "${song.artist} • Already added" else song.artist,
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Light,
                                    fontSize = 12.sp,
                                    color = if (alreadyInPlaylist) SoftGold.copy(alpha = 0.6f) else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", fontFamily = OutfitFontFamily, color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAdd(selectedSongs.toList()) },
                        enabled = selectedSongs.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (selectedSongs.isEmpty()) "Add" else "Add (${selectedSongs.size})",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun formatTotalDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}
