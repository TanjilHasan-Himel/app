package com.eplaytime.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.eplaytime.app.BuildConfig
import com.eplaytime.app.data.datastore.PlayTimeDataStore
import com.eplaytime.app.ui.theme.*
import com.eplaytime.app.util.FirebaseDiagnostics
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    currentFilterState: Boolean,
    onToggleFilter: (Boolean) -> Unit,
    currentCallFilterState: Boolean,
    onToggleCallFilter: (Boolean) -> Unit,
    onOpenScanFilters: () -> Unit = {},
    onTermsClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    val context = LocalContext.current
    val dataStore = remember { PlayTimeDataStore(context) }
    val isAnalyticsEnabled by dataStore.isAnalyticsEnabled.collectAsState(initial = true)
    val isCrashlyticsEnabled by dataStore.isCrashlyticsEnabled.collectAsState(initial = true)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(showDialog) {
        if (showDialog) {
            FirebaseDiagnostics.recordScreen(context, "SettingsDialog")
        }
    }

    var showPermissionsDialog by remember { mutableStateOf(false) }

    if (showPermissionsDialog) {
        PermissionsDialog(
            showDialog = showPermissionsDialog,
            onDismiss = { showPermissionsDialog = false }
        )
    }

    if (showDialog) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = BlackSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Settings",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = TextPrimary
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = GlassBorder)

                    // App Permissions & System Access entry
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showPermissionsDialog = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = SoftGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "App Permissions",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Audio, Notifications, Alarms & Battery",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Light,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = SoftGold.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    HorizontalDivider(color = GlassBorder)

                    // Scan Filters entry (Echo Music style)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onDismiss()
                                onOpenScanFilters()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = SoftGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Scan filters",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Duration limits, Excluded / Included folders",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Light,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = SoftGold.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    HorizontalDivider(color = GlassBorder)

                    // Smart Filter Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleFilter(!currentFilterState) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = SoftGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Smart Filter",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Hide audio < 30s",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Light,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = currentFilterState,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = SoftGold,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = BlackCard
                            )
                        )
                    }

                    // Call Recording Filter Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleCallFilter(!currentCallFilterState) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = SoftGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hide Call Recordings",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Exclude Calls & Voice Notes",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Light,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = currentCallFilterState,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = SoftGold,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = BlackCard
                            )
                        )
                    }

                    HorizontalDivider(color = GlassBorder)

                    // Anonymous Crash Reports (Crashlytics)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    dataStore.setCrashlyticsEnabled(!isCrashlyticsEnabled)
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = SoftGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Anonymous Crash Reports",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Send crash logs to help fix bugs",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Light,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = isCrashlyticsEnabled,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = SoftGold,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = BlackCard
                            )
                        )
                    }

                    // Anonymous Usage Statistics (Analytics)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    dataStore.setAnalyticsEnabled(!isAnalyticsEnabled)
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = SoftGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Anonymous Analytics",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Share non-sensitive feature usage",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Light,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = isAnalyticsEnabled,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = SoftGold,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = BlackCard
                            )
                        )
                    }

                    if (BuildConfig.DEBUG) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch { dataStore.setAnalyticsEnabled(true) }
                                    FirebaseDiagnostics.sendTestDiagnosticEvent(context)
                                    android.widget.Toast.makeText(context, "Test event sent! Check Firebase DebugView", android.widget.Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier.weight(1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SoftGold.copy(alpha = 0.5f))
                            ) {
                                Text("Test Event", fontSize = 12.sp, color = SoftGold, fontFamily = OutfitFontFamily)
                            }
                            OutlinedButton(
                                onClick = {
                                    FirebaseDiagnostics.recordAction(context, "test_crash_requested", "SettingsDialog")
                                    FirebaseDiagnostics.triggerTestCrash(context)
                                },
                                modifier = Modifier.weight(1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f))
                            ) {
                                Text("Test Crash", fontSize = 12.sp, color = Color(0xFFFF5252), fontFamily = OutfitFontFamily)
                            }
                        }
                    }

                    HorizontalDivider(color = GlassBorder)

                    // Links
                    SettingsLink(
                        icon = Icons.Default.Info,
                        title = "About App",
                        onClick = {
                            onDismiss()
                            onAboutClick()
                        }
                    )
                    
                    SettingsLink(
                        icon = Icons.Default.Policy,
                        title = "Terms & Conditions",
                        onClick = {
                            onDismiss()
                            onTermsClick()
                        }
                    )

                    SettingsLink(
                        icon = Icons.Default.Policy,
                        title = "Privacy Policy",
                        onClick = {
                            onDismiss()
                            onTermsClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsLink(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            color = TextPrimary
        )
    }
}
