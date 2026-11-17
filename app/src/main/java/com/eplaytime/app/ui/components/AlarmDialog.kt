package com.eplaytime.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import android.widget.Toast
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.eplaytime.app.data.model.ScheduledTask
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.ui.theme.OswaldFontFamily
import com.eplaytime.app.ui.theme.OutfitFontFamily
import com.eplaytime.app.ui.theme.SoftGold
import java.util.*

data class ScheduledContentSelection(
    val type: String,
    val title: String,
    val songs: List<Song>
)

/**
 * Glassmorphism Alarm Dialog (Version 3.0 Pro)
 * Supports: Creating, Editing, Specific Time, and Countdown Timer.
 */
@Composable
fun AlarmDialog(
    songList: List<Song>,
    alarmToEdit: ScheduledTask? = null,
    onDismiss: () -> Unit,
    onConfirm: (ScheduledContentSelection, Float, String, Int, Int, Int, Int) -> Unit
) {
    // === State Initialization ===
    
    // Determine initial time (Edit Mode or Current Time)
    val initialCalendar = remember {
        Calendar.getInstance().apply {
            if (alarmToEdit != null) {
                set(Calendar.HOUR_OF_DAY, alarmToEdit.hour)
                set(Calendar.MINUTE, alarmToEdit.minute)
            } else {
                // Default to next hour for new alarms? Or current time.
                // Let's stick to current time + 1 minute for convenience
                add(Calendar.MINUTE, 1) 
            }
        }
    }

    // Working state for Time (Target Time)
    var selectedHour by remember { mutableStateOf(initialCalendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(initialCalendar.get(Calendar.MINUTE)) }

    // Timer Tab State (Duration)
    var timerHours by remember { mutableStateOf(0) }
    var timerMinutes by remember { mutableStateOf(30) }

    // Other settings
    var selectedSong by remember { 
        mutableStateOf(
            if (alarmToEdit != null) songList.find { it.uri == alarmToEdit.songUri } 
            else songList.firstOrNull() 
        ) 
    }
    var selectedContentType by remember { mutableStateOf(alarmToEdit?.contentType ?: "TRACK") }
    var selectedContentSongs by remember(songList, alarmToEdit) {
        mutableStateOf(
            alarmToEdit?.contentUris
                ?.lineSequence()
                ?.filter(String::isNotBlank)
                ?.mapNotNull { uri -> songList.firstOrNull { it.uri == uri } }
                ?.toList()
                .orEmpty()
                .ifEmpty { listOfNotNull(selectedSong) }
        )
    }
    var selectedVolume by remember { mutableStateOf(alarmToEdit?.targetVolume ?: 0.5f) }
    var stopAfterMinutes by remember { mutableIntStateOf(alarmToEdit?.stopAfterMinutes ?: 0) }
    var fadeInSeconds by remember { mutableIntStateOf(alarmToEdit?.fadeInSeconds ?: 10) }
    var selectedDays by remember { 
        mutableStateOf(
            if (!alarmToEdit?.repeatDays.isNullOrEmpty()) alarmToEdit!!.repeatDays.split(",").toSet()
            else emptySet()
        ) 
    }

    // Tabs: 0 = Specific Time, 1 = Countdown
    var selectedTab by remember { mutableStateOf(0) }

    // Search State
    var showSearchOverlay by remember { mutableStateOf(false) }

    // === Calculations ===
    
    // For Timer Tab: Update the "Target Time" whenever Timer Input changes
    LaunchedEffect(timerHours, timerMinutes) {
        if (selectedTab == 1) {
            val now = Calendar.getInstance()
            now.add(Calendar.HOUR_OF_DAY, timerHours)
            now.add(Calendar.MINUTE, timerMinutes)
            selectedHour = now.get(Calendar.HOUR_OF_DAY)
            selectedMinute = now.get(Calendar.MINUTE)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(enabled = false) {}
                .padding(16.dp)
        ) {
            val dialogMaxHeight = maxHeight * 0.92f
            val dialogWidthFraction = if (maxWidth < 360.dp) 0.99f else 0.95f

            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(dialogWidthFraction)
                    .heightIn(max = dialogMaxHeight)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E).copy(alpha = 0.95f))
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Text(
                        text = if (alarmToEdit != null) "Edit Session" else "New Session",
                        fontFamily = OswaldFontFamily,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // === TABS ===
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TabButton(
                            text = "Specific Time",
                            isSelected = selectedTab == 0,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTab = 0 }
                        )
                        TabButton(
                            text = "Countdown",
                            isSelected = selectedTab == 1,
                            modifier = Modifier.weight(1f),
                            onClick = { 
                                selectedTab = 1
                                // Trigger calculation immediately
                                val now = Calendar.getInstance()
                                now.add(Calendar.HOUR_OF_DAY, timerHours)
                                now.add(Calendar.MINUTE, timerMinutes)
                                selectedHour = now.get(Calendar.HOUR_OF_DAY)
                                selectedMinute = now.get(Calendar.MINUTE)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // === CONTENT ===
                    if (selectedTab == 0) {
                        // SPECIFIC TIME
                        SimpleTimeButtons(
                            hour24 = selectedHour,
                            minute = selectedMinute,
                            onTimeChanged = { h, m ->
                                selectedHour = h
                                selectedMinute = m
                            }
                        )
                    } else {
                        // COUNTDOWN TIMER
                        TimerInput(
                            hours = timerHours,
                            minutes = timerMinutes,
                            onDurationChanged = { h, m ->
                                timerHours = h
                                timerMinutes = m
                            },
                            resultingTime = formatTime(selectedHour, selectedMinute)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // === SETTINGS (Song, Vol, Days) ===
                    
                    Text("Content", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        items(listOf("TRACK", "ALBUM", "FOLDER", "PLAYLIST", "QUEUE")) { type ->
                            FilterChip(
                                selected = selectedContentType == type,
                                onClick = {
                                    selectedContentType = type
                                    selectedContentSongs = emptyList()
                                    showSearchOverlay = true
                                },
                                label = { Text(type.lowercase().replaceFirstChar(Char::uppercase)) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .clickable { showSearchOverlay = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                         Column(modifier = Modifier.weight(1f)) {
                             Text(
                                 text = when {
                                     selectedContentSongs.isEmpty() -> "Select ${selectedContentType.lowercase()}..."
                                     selectedContentType == "TRACK" -> selectedContentSongs.firstOrNull()?.title ?: "Select track..."
                                     else -> alarmToEdit?.songTitle?.takeIf {
                                         alarmToEdit.contentType == selectedContentType
                                     } ?: "${selectedContentSongs.size} tracks selected"
                                 },
                                 color = Color.White,
                                 fontWeight = FontWeight.Medium,
                                 maxLines = 1
                             )
                             if (selectedContentSongs.isNotEmpty()) {
                                 Text(
                                     text = if (selectedContentType == "TRACK") selectedContentSongs.firstOrNull()?.artist.orEmpty()
                                     else "Plays once, then stops",
                                     color = Color.Gray,
                                     fontSize = 12.sp,
                                     maxLines = 1
                                 )
                             }
                         }
                         Icon(Icons.Default.Search, "Search", tint = SoftGold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Volume
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Volume", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("${(selectedVolume * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
                    }
                    Slider(
                        value = selectedVolume,
                        onValueChange = { selectedVolume = it },
                        modifier = Modifier.height(20.dp),
                         colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF1DB954),
                            activeTrackColor = Color(0xFF1DB954),
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Fade in", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(if (fadeInSeconds == 0) "Off" else "${fadeInSeconds}s", color = Color.White, fontSize = 12.sp)
                    }
                    Slider(
                        value = fadeInSeconds.toFloat(),
                        onValueChange = { fadeInSeconds = (it / 5f).toInt() * 5 },
                        valueRange = 0f..60f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = SoftGold,
                            activeTrackColor = SoftGold,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Stop", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        items(listOf(0 to "When finished", 30 to "30 min", 60 to "1 hour", 120 to "2 hours")) { option ->
                            FilterChip(
                                selected = stopAfterMinutes == option.first,
                                onClick = { stopAfterMinutes = option.first },
                                label = { Text(option.second) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Weekdays
                    Text("Repeat", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(8.dp))
                    WeekDaySelector(
                        selectedDays = selectedDays,
                        onDayToggle = { day, isSelected ->
                             selectedDays = if (isSelected) selectedDays - day else selectedDays + day
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // === BOTTOM ACTIONS ===
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                        }
                        
                        Button(
                            onClick = {
                                if (selectedContentSongs.isNotEmpty()) {
                                    val daysString = selectedDays.joinToString(",")
                                    val firstSong = selectedContentSongs.firstOrNull()
                                    val title = when (selectedContentType) {
                                        "TRACK" -> firstSong?.title ?: "Track"
                                        "ALBUM" -> firstSong?.album.takeUnless { it.isNullOrBlank() } ?: "Album"
                                        "FOLDER" -> firstSong?.bucketDisplayName?.ifBlank { "Folder" } ?: "Folder"
                                        "PLAYLIST" -> "Playlist (${selectedContentSongs.size})"
                                        else -> "Custom queue (${selectedContentSongs.size})"
                                    }
                                    onConfirm(
                                        ScheduledContentSelection(selectedContentType, title, selectedContentSongs),
                                        selectedVolume,
                                        daysString,
                                        selectedHour,
                                        selectedMinute,
                                        stopAfterMinutes,
                                        fadeInSeconds
                                    )
                                }
                            },
                             colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                             enabled = selectedContentSongs.isNotEmpty(),
                             modifier = Modifier.weight(1f)
                        ) {
                            Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // === SONG SEARCH OVERLAY (Glass Bottom Sheet Style) ===
    if (showSearchOverlay) {
        Dialog(
            onDismissRequest = { showSearchOverlay = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val sheetHeight = (maxHeight * 0.78f).coerceAtLeast(360.dp)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showSearchOverlay = false }
                )

                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .heightIn(max = sheetHeight)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E).copy(alpha = 0.98f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Drag Handle
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.Gray.copy(alpha = 0.5f))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Select ${selectedContentType.lowercase().replaceFirstChar(Char::uppercase)}",
                            fontFamily = OswaldFontFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        ScheduleContentPicker(
                            songList = songList,
                            type = selectedContentType,
                            initialSelection = selectedContentSongs,
                            onSelected = {
                                selectedContentSongs = it
                                selectedSong = it.firstOrNull()
                                showSearchOverlay = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// ================= COMPONENT SUB-FUNCTIONS =================

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color(0xFF1DB954) else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun TimerInput(
    hours: Int,
    minutes: Int,
    onDurationChanged: (Int, Int) -> Unit,
    resultingTime: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Hours
            TimeStepper(
                label = "Hours", 
                value = hours, 
                onIncrement = { if (hours < 23) onDurationChanged(hours + 1, minutes) },
                onDecrement = { if (hours > 0) onDurationChanged(hours - 1, minutes) }
            )
            
            Text(":", fontSize = 32.sp, color = Color.White.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp))

            // Minutes
            TimeStepper(
                label = "Mins", 
                value = minutes, 
                onIncrement = { 
                    if (minutes < 59) onDurationChanged(hours, minutes + 1)
                    else {
                        // Roll over to hour
                        if (hours < 23) onDurationChanged(hours + 1, 0)
                    }
                },
                onDecrement = { 
                    if (minutes > 0) onDurationChanged(hours, minutes - 1)
                    else {
                        // Roll back hour
                         if (hours > 0) onDurationChanged(hours - 1, 59)
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Helper Text
        Text(
            text = "Playback will start at $resultingTime",
            color = Color(0xFF1DB954),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SimpleTimeButtons(
    hour24: Int,
    minute: Int,
    onTimeChanged: (Int, Int) -> Unit
) {
    var isAm = hour24 < 12
    var hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
    
    // Helper to commit changes
    fun updateTime(newH12: Int, newMin: Int, newAm: Boolean) {
         val finalHour = when {
             newAm && newH12 == 12 -> 0
             newAm -> newH12
             !newAm && newH12 == 12 -> 12
             else -> newH12 + 12
         }
         onTimeChanged(finalHour, newMin)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeStepper(
                label = "Hour",
                value = hour12,
                onDecrement = {
                    val next = if (hour12 == 1) 12 else hour12 - 1
                    updateTime(next, minute, isAm)
                },
                onIncrement = {
                    val next = if (hour12 == 12) 1 else hour12 + 1
                    updateTime(next, minute, isAm)
                }
            )

            TimeStepper(
                label = "Min",
                value = minute,
                onDecrement = {
                    val next = if (minute == 0) 59 else minute - 1
                    updateTime(hour12, next, isAm)
                },
                onIncrement = {
                    val next = if (minute == 59) 0 else minute + 1
                    updateTime(hour12, next, isAm)
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Row {
             FilterChip(
                 selected = isAm,
                 onClick = { updateTime(hour12, minute, true) },
                 label = { Text("AM") },
                 colors = FilterChipDefaults.filterChipColors(
                     selectedContainerColor = Color(0xFF1DB954),
                     selectedLabelColor = Color.White
                 ),
                 border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isAm, borderColor = Color.Transparent)
             )
             Spacer(modifier = Modifier.width(12.dp))
             FilterChip(
                 selected = !isAm,
                 onClick = { updateTime(hour12, minute, false) },
                 label = { Text("PM") },
                 colors = FilterChipDefaults.filterChipColors(
                     selectedContainerColor = Color(0xFF1DB954),
                     selectedLabelColor = Color.White
                 ),
                 border = FilterChipDefaults.filterChipBorder(enabled = true, selected = !isAm, borderColor = Color.Transparent)
             )
        }
    }
}

@Composable
private fun TimeStepper(
    label: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(4.dp))
        var dragAccumulator by remember { mutableFloatStateOf(0f) }
        Container(
             modifier = Modifier
                 .clip(RoundedCornerShape(12.dp))
                 .background(Color.Black.copy(alpha = 0.3f))
                 .pointerInput(Unit) {
                     detectVerticalDragGestures(
                         onDragEnd = { dragAccumulator = 0f }
                     ) { change, dragAmount ->
                         change.consume()
                         dragAccumulator += dragAmount
                         if (dragAccumulator > 30f) { // Swiped down (decrement)
                             onDecrement()
                             dragAccumulator = 0f
                         } else if (dragAccumulator < -30f) { // Swiped up (increment)
                             onIncrement()
                             dragAccumulator = 0f
                         }
                     }
                 }
        ) {
             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                 IconButton(onClick = onIncrement) {
                     Icon(Icons.Default.Add, "Up", tint = Color.Gray)
                 }
                 Text(
                     text = String.format("%02d", value),
                     fontSize = 32.sp,
                     fontWeight = FontWeight.Bold,
                     color = Color.White,
                     fontFamily = OswaldFontFamily
                 )
                 IconButton(onClick = onDecrement) {
                     Icon(Icons.Default.Remove, "Down", tint = Color.Gray)
                 }
             }
        }
    }
}

@Composable
private fun Container(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier) { content() }
}

@Composable
private fun WeekDaySelector(
    selectedDays: Set<String>,
    onDayToggle: (String, Boolean) -> Unit
) {
    val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(days) { day ->
            val isSelected = selectedDays.contains(day)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0xFF1DB954) else Color.White.copy(alpha = 0.1f))
                    .clickable { onDayToggle(day, isSelected) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.first().toString(),
                    color = if (isSelected) Color.White else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private data class SchedulePickerItem(
    val uniqueKey: String,
    val title: String,
    val subtitle: String,
    val songs: List<Song>
)

@Composable
private fun ScheduleContentPicker(
    songList: List<Song>,
    type: String,
    initialSelection: List<Song>,
    onSelected: (List<Song>) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var queueSelection by remember(type, initialSelection) {
        mutableStateOf(initialSelection.map(Song::uri).toSet())
    }
    val items = remember(type, query, songList) {
        val source = if (query.isBlank()) songList else songList.filter {
            it.title.contains(query, true) || it.artist.contains(query, true) ||
                it.album.contains(query, true) || it.bucketDisplayName.contains(query, true)
        }
        when (type) {
            "ALBUM" -> {
                source.filter { it.album.isNotBlank() }
                    .groupBy(Song::album)
                    .entries
                    .mapIndexed { index, (albumName, songs) ->
                        SchedulePickerItem(
                            uniqueKey = "album_${albumName}_$index",
                            title = albumName,
                            subtitle = "${songs.size} tracks • ${songs.firstOrNull()?.artist.orEmpty()}",
                            songs = songs
                        )
                    }
            }
            "FOLDER" -> {
                source.groupBy { it.folderPath.ifBlank { it.bucketDisplayName } }
                    .entries
                    .mapIndexed { index, (path, songs) ->
                        val displayName = songs.firstOrNull()?.bucketDisplayName?.ifBlank { path.substringAfterLast('/') } ?: "Folder"
                        SchedulePickerItem(
                            uniqueKey = "folder_${path}_$index",
                            title = displayName,
                            subtitle = "${songs.size} tracks • $path",
                            songs = songs
                        )
                    }
            }
            "QUEUE", "PLAYLIST" -> {
                source.mapIndexed { index, song ->
                    SchedulePickerItem(
                        uniqueKey = "multi_${song.id}_${song.uri}_$index",
                        title = song.title.ifBlank { "Unknown Title" },
                        subtitle = song.artist.ifBlank { "Unknown Artist" },
                        songs = listOf(song)
                    )
                }
            }
            else -> { // "TRACK"
                source.mapIndexed { index, song ->
                    SchedulePickerItem(
                        uniqueKey = "track_${song.id}_${song.uri}_$index",
                        title = song.title.ifBlank { "Unknown Title" },
                        subtitle = song.artist.ifBlank { "Unknown Artist" },
                        songs = listOf(song)
                    )
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search ${type.lowercase()}...", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF1DB954),
                focusedBorderColor = Color(0xFF1DB954),
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Search, "Search", tint = Color.Gray) }
        )

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (query.isNotBlank()) "No matching ${type.lowercase()} found" else "No audio items available",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items, key = { it.uniqueKey }) { item ->
                    val isMultiSelect = type == "QUEUE" || type == "PLAYLIST"
                    val itemUri = item.songs.firstOrNull()?.uri
                    val selected = isMultiSelect && itemUri != null && itemUri in queueSelection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isMultiSelect) {
                                    if (itemUri != null) {
                                        queueSelection = if (selected) queueSelection - itemUri else queueSelection + itemUri
                                    }
                                } else if (item.songs.isNotEmpty()) {
                                    onSelected(item.songs)
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon Left
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (type) {
                                    "FOLDER" -> Icons.Default.Folder
                                    "QUEUE", "PLAYLIST" -> Icons.Default.QueueMusic
                                    else -> Icons.Default.MusicNote
                                },
                                contentDescription = null,
                                tint = SoftGold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.subtitle,
                                color = Color.Gray,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isMultiSelect) {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = SoftGold)
                            )
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                }
            }
        }

        if (type == "QUEUE" || type == "PLAYLIST") {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onSelected(songList.filter { it.uri in queueSelection }) },
                enabled = queueSelection.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SoftGold)
            ) {
                Text("Use ${queueSelection.size} selected tracks", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = if (hour % 12 == 0) 12 else hour % 12
    return String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm)
}
