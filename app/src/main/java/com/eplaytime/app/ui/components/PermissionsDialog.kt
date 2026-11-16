package com.eplaytime.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.eplaytime.app.ui.theme.*
import com.eplaytime.app.util.PermissionHelper

@Composable
fun PermissionsDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit
) {
    if (!showDialog) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var musicGranted by remember { mutableStateOf(PermissionHelper.hasAudioPermission(context)) }
    var notifGranted by remember { mutableStateOf(PermissionHelper.hasNotificationPermission(context)) }
    var alarmGranted by remember { mutableStateOf(PermissionHelper.canScheduleExactAlarms(context)) }
    var batteryGranted by remember { mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context)) }
    var visualizerGranted by remember { mutableStateOf(PermissionHelper.hasRecordAudioPermission(context)) }

    fun refreshStates() {
        musicGranted = PermissionHelper.hasAudioPermission(context)
        notifGranted = PermissionHelper.hasNotificationPermission(context)
        alarmGranted = PermissionHelper.canScheduleExactAlarms(context)
        batteryGranted = PermissionHelper.isIgnoringBatteryOptimizations(context)
        visualizerGranted = PermissionHelper.hasRecordAudioPermission(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(24.dp),
            color = BlackSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "App Permissions",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "System access & feature permissions",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Light,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                HorizontalDivider(color = GlassBorder)

                // 1. Music & Audio Storage
                PermissionItemRow(
                    icon = Icons.Default.MusicNote,
                    title = "Music & Audio Files",
                    subtitle = "Required to scan and play your songs",
                    isGranted = musicGranted,
                    actionText = "Manage",
                    onAction = { PermissionHelper.openAppSettings(context) }
                )

                // 2. Notifications
                PermissionItemRow(
                    icon = Icons.Default.Notifications,
                    title = "Notification Controls",
                    subtitle = "Lock screen playback & status bar controls",
                    isGranted = notifGranted,
                    actionText = "Configure",
                    onAction = { PermissionHelper.openNotificationSettings(context) }
                )

                // 3. Exact Alarms
                PermissionItemRow(
                    icon = Icons.Default.Schedule,
                    title = "Exact Alarms (Scheduler)",
                    subtitle = "Accurate timing for scheduled music sessions",
                    isGranted = alarmGranted,
                    actionText = "Allow",
                    onAction = { PermissionHelper.openExactAlarmSettings(context) }
                )

                // 4. Battery Optimization
                PermissionItemRow(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "Battery Saver Bypass",
                    subtitle = "Ensures background playback is uninterrupted",
                    isGranted = batteryGranted,
                    actionText = "Whitelist",
                    onAction = { PermissionHelper.openBatteryOptimizationSettings(context) }
                )

                // 5. Visualizer
                PermissionItemRow(
                    icon = Icons.Default.GraphicEq,
                    title = "Live Audio Spectrum",
                    subtitle = "Enables real-time frequency visualizer",
                    isGranted = visualizerGranted,
                    actionText = "Manage",
                    onAction = { PermissionHelper.openAppSettings(context) }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom button: Open All Android Settings
                Button(
                    onClick = { PermissionHelper.openAppSettings(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftGold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open System App Settings",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun PermissionItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    actionText: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isGranted) Color(0x224CAF50) else Color(0x1AFFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF4CAF50) else SoftGold,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (isGranted) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x224CAF50))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Enabled",
                    fontFamily = OutfitFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
        } else {
            OutlinedButton(
                onClick = onAction,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftGold),
                border = androidx.compose.foundation.BorderStroke(1.dp, SoftGold.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    text = actionText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OutfitFontFamily
                )
            }
        }
    }
}
