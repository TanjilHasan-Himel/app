package com.eplaytime.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import android.content.Context
import android.content.Intent
import android.app.Activity
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.eplaytime.app.util.PermissionHelper
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.app.ActivityCompat
import com.eplaytime.app.data.model.ScheduledTask
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.ui.theme.*
import com.eplaytime.app.util.FirebaseDiagnostics
import com.eplaytime.app.ui.viewmodel.MusicViewModel
import com.eplaytime.app.ui.viewmodel.SchedulerViewModel
import com.eplaytime.app.util.ScheduledOccurrenceCalculator
import java.util.*

/**
 * SchedulerScreen - UI for creating and managing scheduled playback alarms
 * USP Feature: Auto-Play Scheduler
 * Rich & Soft aesthetic with Outfit font
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulerScreen(
    onNavigateBack: () -> Unit,
    schedulerViewModel: SchedulerViewModel = hiltViewModel(),
    musicViewModel: MusicViewModel
) {
    val tasks by schedulerViewModel.tasks.collectAsState()
    val songList by musicViewModel.songList.collectAsState()
    val selectedHour by schedulerViewModel.selectedHour.collectAsState()
    val selectedMinute by schedulerViewModel.selectedMinute.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var alarmToEdit by remember { mutableStateOf<ScheduledTask?>(null) }
    
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        FirebaseDiagnostics.recordScreen(context, "SchedulerScreen")
    }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var showBatteryWarning by remember { mutableStateOf(false) }
    var showAlarmWarning by remember { mutableStateOf(false) }
    var showNotificationWarning by remember { mutableStateOf(false) }
    var dismissedBatteryWarningForSession by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                showBatteryWarning = !PermissionHelper.isIgnoringBatteryOptimizations(context) && !dismissedBatteryWarningForSession
                showAlarmWarning = !PermissionHelper.canScheduleExactAlarms(context)
                showNotificationWarning = !PermissionHelper.hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        // Initial check
        showBatteryWarning = !PermissionHelper.isIgnoringBatteryOptimizations(context) && !dismissedBatteryWarningForSession
        showAlarmWarning = !PermissionHelper.canScheduleExactAlarms(context)
        showNotificationWarning = !PermissionHelper.hasNotificationPermission(context)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Scheduled Playback",
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
        contentWindowInsets = WindowInsets.systemBars,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = SoftGold
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create scheduled playback",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (showBatteryWarning) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFC62828).copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color(0xFFFFCDD2))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Battery restriction detected", color = Color(0xFFFFCDD2), fontWeight = FontWeight.Bold, fontFamily = OutfitFontFamily)
                                Text("Allow battery optimization bypass for more reliable scheduled playback.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontFamily = OutfitFontFamily, lineHeight = 16.sp)
                            }
                            TextButton(onClick = {
                                PermissionHelper.openBatteryOptimizationSettings(context)
                                dismissedBatteryWarningForSession = true
                                showBatteryWarning = false
                            }) {
                                Text("OPEN", color = SoftGold, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                if (showAlarmWarning && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFC62828).copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color(0xFFFFCDD2))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Exact timing needs permission", color = Color(0xFFFFCDD2), fontWeight = FontWeight.Bold, fontFamily = OutfitFontFamily)
                                Text("Allow Alarms & reminders so Audia can start this session on time.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontFamily = OutfitFontFamily)
                            }
                            TextButton(onClick = {
                                PermissionHelper.openExactAlarmSettings(context)
                                showAlarmWarning = false
                            }) {
                                Text("ALLOW", color = SoftGold, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                if (showNotificationWarning) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE65100).copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color(0xFFFFE0B2))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Notifications Off", color = Color(0xFFFFE0B2), fontWeight = FontWeight.Bold, fontFamily = OutfitFontFamily)
                                Text("Needed to keep the system awake during playback.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontFamily = OutfitFontFamily)
                            }
                            TextButton(onClick = {
                                PermissionHelper.openNotificationSettings(context)
                                showNotificationWarning = false
                            }) {
                                Text("SETTINGS", color = SoftGold, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (tasks.isEmpty()) {
                        // Empty state
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Scheduled Sessions",
                        fontFamily = OswaldFontFamily,
                        fontSize = 24.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Choose music and a time for your first session",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            } else {
                // Task list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggle = { schedulerViewModel.toggleTask(task) },
                            onDelete = { schedulerViewModel.deleteTask(task) },
                            onEdit = { 
                                alarmToEdit = task
                                showCreateDialog = true
                            }
                        )
                    }
                }
                }
            }
            } // Close added Column
        }
    }

    // Create/Edit alarm dialog
    if (showCreateDialog) {
        com.eplaytime.app.ui.components.AlarmDialog(
            songList = songList,
            alarmToEdit = alarmToEdit, // Pass the task to edit
            onConfirm = { content, volume, repeatDays, hour, minute, stopAfterMinutes, fadeInSeconds ->
                if (alarmToEdit != null) {
                    // Update existing
                    schedulerViewModel.updateTask(
                        task = alarmToEdit!!,
                        contentType = content.type,
                        contentUris = content.songs.map(Song::uri),
                        contentTitle = content.title,
                        hour = hour,
                        minute = minute,
                        targetVolume = volume,
                        repeatDays = repeatDays,
                        stopAfterMinutes = stopAfterMinutes,
                        fadeInSeconds = fadeInSeconds
                    )
                } else {
                    // Create new
                    schedulerViewModel.createTask(
                        contentType = content.type,
                        contentUris = content.songs.map(Song::uri),
                        contentTitle = content.title,
                        hour = hour,
                        minute = minute,
                        targetVolume = volume,
                        repeatDays = repeatDays,
                        stopAfterMinutes = stopAfterMinutes,
                        fadeInSeconds = fadeInSeconds
                    )
                }
                showCreateDialog = false
                alarmToEdit = null // Reset
            },
            onDismiss = { 
                showCreateDialog = false 
                alarmToEdit = null // Reset
            }
        )
    }
}

/**
 * Task card showing scheduled alarm details
 */
@Composable
private fun TaskCard(
    task: ScheduledTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }, // Make card clickable to edit
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time display
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatTime(task.hour, task.minute),
                    fontFamily = OswaldFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = if (task.isEnabled) Color.White else Color.White.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${task.songTitle} • ${task.contentType.lowercase().replaceFirstChar(Char::uppercase)} • Vol ${(task.targetVolume * 100).toInt()}%",
                    fontSize = 14.sp,
                    color = if (task.isEnabled) Color.White.copy(alpha = 0.7f)
                           else Color.White.copy(alpha = 0.3f),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                val nextRun = remember(task.hour, task.minute, task.repeatDays, task.nextRunAt) {
                    java.text.DateFormat.getDateTimeInstance(
                        java.text.DateFormat.MEDIUM,
                        java.text.DateFormat.SHORT
                    ).format(
                        Date(
                            task.nextRunAt.takeIf { it > System.currentTimeMillis() }
                                ?: ScheduledOccurrenceCalculator.nextTriggerMillis(task.hour, task.minute, task.repeatDays)
                        )
                    )
                }
                Text(
                    text = when {
                        !task.isEnabled -> "Disabled"
                        task.lastResult == "NEEDS_PERMISSION" -> "Needs exact timing permission"
                        task.lastResult == "PLAYING" || task.lastResult == "STARTING" -> "Playing scheduled session"
                        task.lastResult == "FAILED" -> "Last session failed • Next $nextRun"
                        task.lastResult == "MISSING_CONTENT" -> "Selected music is missing or unavailable"
                        task.lastResult == "MISSED" -> "Previous session was missed • Next $nextRun"
                        task.lastResult == "INTERRUPTED" -> "Recovering interrupted session"
                        task.lastResult == "COMPLETED" -> "Completed • Next $nextRun"
                        else -> "Ready • Next $nextRun${if (task.repeatDays.isNotEmpty()) " • ${task.repeatDays}" else ""}"
                    },
                    fontSize = 12.sp,
                    color = if (task.isEnabled) SoftGold.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.35f),
                    maxLines = 2
                )
            }

            // Toggle switch
            Switch(
                checked = task.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF1DB954),
                    checkedTrackColor = Color(0xFF1DB954).copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Edit button
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Format time as HH:MM
 */
private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = if (hour % 12 == 0) 12 else hour % 12
    return String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm)
}
