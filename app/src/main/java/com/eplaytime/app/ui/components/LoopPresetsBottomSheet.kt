package com.eplaytime.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eplaytime.app.data.datastore.LoopPreset
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.ui.theme.*
import com.eplaytime.app.ui.viewmodel.MusicViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoopPresetsBottomSheet(
    song: Song?,
    viewModel: MusicViewModel,
    loopStart: Long?,
    loopEnd: Long?,
    loopEnabled: Boolean,
    loopPresets: List<LoopPreset>,
    onDismiss: () -> Unit
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val allPresetsMap by viewModel.allLoopPresets.collectAsState()
    val allSongs by viewModel.songList.collectAsState()
    val songsByUri = remember(allSongs) { allSongs.associateBy { it.uri } }
    val totalSavedCount = remember(allPresetsMap) { allPresetsMap.values.sumOf { it.size } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141414),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = SoftGold.copy(alpha = 0.5f),
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SoftGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            tint = SoftGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "A-B Loop Presets",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = song?.title ?: "Current Track",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Normal,
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

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-tabs: "This Track" and "All Saved Loops"
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = SoftGold,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = SoftGold,
                            height = 2.dp
                        )
                    }
                },
                divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.08f)) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "This Track (${loopPresets.size})",
                            fontFamily = OutfitFontFamily,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) SoftGold else TextSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "All Saved Loops ($totalSavedCount)",
                            fontFamily = OutfitFontFamily,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) SoftGold else TextSecondary
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedTab == 0) {
                // Current Active Loop Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(
                            1.dp,
                            if (loopEnabled) SoftGold.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ACTIVE LOOP STATUS",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = if (loopEnabled) SoftGold else TextSecondary
                            )

                            if (loopEnabled || loopStart != null) {
                                TextButton(
                                    onClick = { viewModel.clearLoop() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Clear",
                                        fontFamily = OutfitFontFamily,
                                        fontSize = 12.sp,
                                        color = Color(0xFFFF5252)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (loopEnabled && loopStart != null && loopEnd != null) {
                            val durationSec = ((loopEnd - loopStart) / 1000f)
                            Text(
                                text = "${formatLoopTime(loopStart)}  ➔  ${formatLoopTime(loopEnd)}  (${String.format(Locale.US, "%.1fs", durationSec)})",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    newPresetName = "Loop ${loopPresets.size + 1}"
                                    showNameDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SoftGold),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkAdd,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Save Active Loop as Preset",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.Black
                                )
                            }
                        } else if (loopStart != null) {
                            Text(
                                text = "Point A set at ${formatLoopTime(loopStart)}. Set Point B on player to activate loop.",
                                fontFamily = OutfitFontFamily,
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        } else {
                            Text(
                                text = "No loop currently active. Tap 'A-B' on the player controls to set loop points.",
                                fontFamily = OutfitFontFamily,
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Saved Presets Title
                Text(
                    text = "SAVED PRESETS FOR THIS TRACK (${loopPresets.size})",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (loopPresets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved loops for this song yet.\nSave your favorite choruses, solos, or intros!",
                            fontFamily = OutfitFontFamily,
                            fontSize = 13.sp,
                            color = TextTertiary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = loopPresets, key = { it.name }) { preset ->
                            val isPresetActive = loopEnabled && loopStart == preset.startMs && loopEnd == preset.endMs
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isPresetActive) SoftGold.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                                    .border(
                                        1.dp,
                                        if (isPresetActive) SoftGold.copy(alpha = 0.5f) else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        viewModel.applyLoopPreset(preset.name)
                                        viewModel.seekTo(preset.startMs)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.name,
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = if (isPresetActive) SoftGold else TextPrimary
                                    )
                                    val durationSec = ((preset.endMs - preset.startMs) / 1000f)
                                    Text(
                                        text = "${formatLoopTime(preset.startMs)} ➔ ${formatLoopTime(preset.endMs)} (${String.format(Locale.US, "%.1fs", durationSec)})",
                                        fontFamily = OutfitFontFamily,
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isPresetActive) {
                                        Text(
                                            text = "PLAYING",
                                            fontFamily = OutfitFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            letterSpacing = 1.sp,
                                            color = SoftGold,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteLoopPreset(preset.name) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete preset",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Tab 1: All Saved Loops Across All Songs
                if (allPresetsMap.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved loops across your library yet.\nCreate loops on any song and they will appear here!",
                            fontFamily = OutfitFontFamily,
                            fontSize = 13.sp,
                            color = TextTertiary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        allPresetsMap.forEach { (songUri, presets) ->
                            val targetSong = songsByUri[songUri]
                            val songTitle = targetSong?.title ?: "Song: ${songUri.substringAfterLast('/')}"

                            item(key = songUri) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = songTitle,
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = SoftGold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    presets.forEach { preset ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clickable {
                                                    viewModel.playLoopPreset(songUri, preset)
                                                },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = preset.name,
                                                    fontFamily = OutfitFontFamily,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 12.sp,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "${formatLoopTime(preset.startMs)} ➔ ${formatLoopTime(preset.endMs)}",
                                                    fontFamily = OutfitFontFamily,
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { viewModel.playLoopPreset(songUri, preset) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "Play Loop",
                                                        tint = SoftGold,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { viewModel.deleteLoopPreset(preset.name, songUri) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Delete preset",
                                                        tint = TextSecondary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Storage transparent note
            Text(
                text = "✓ Loops are stored locally on your device in Jetpack Preferences DataStore. They are 100% offline, persistent across app updates and phone restarts, and never uploaded to any cloud server.",
                fontFamily = OutfitFontFamily,
                fontSize = 11.sp,
                color = TextTertiary,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = {
                Text(
                    text = "Save Loop Preset",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = newPresetName,
                    onValueChange = { newPresetName = it },
                    label = { Text("Preset Name", fontFamily = OutfitFontFamily) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftGold,
                        focusedLabelColor = SoftGold,
                        cursorColor = SoftGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newPresetName.trim().ifEmpty { "Loop ${loopPresets.size + 1}" }
                        viewModel.saveCurrentLoopPreset(name)
                        showNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftGold)
                ) {
                    Text("Save", color = Color.Black, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel", color = TextSecondary, fontFamily = OutfitFontFamily)
                }
            },
            containerColor = Color(0xFF1E1E1E)
        )
    }
}

private fun formatLoopTime(millis: Long): String {
    val totalSec = millis / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.US, "%d:%02d", min, sec)
}

