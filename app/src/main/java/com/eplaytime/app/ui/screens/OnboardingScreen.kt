package com.eplaytime.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.eplaytime.app.R
import com.eplaytime.app.ui.theme.*
import com.eplaytime.app.util.PermissionHelper
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onComplete: (name: String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF16130B),
                        BlackBackground,
                        Color.Black
                    )
                )
            )
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> WelcomeNameStep(name) { name = it }
                1 -> FeaturesStep()
                2 -> PermissionsStep(name, onComplete)
            }
        }

        // Bottom Indicator and navigation (Only visible on page 0 & 1)
        if (pagerState.currentPage < 2) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp, start = 24.dp, end = 24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == index) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index) SoftGold
                                    else Color.White.copy(alpha = 0.25f)
                                )
                        )
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Next", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = OutfitFontFamily)
                }
            }
        }
    }
}

@Composable
private fun WelcomeNameStep(name: String, onNameChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.audia_player_logo),
            contentDescription = null,
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .border(2.dp, SoftGold.copy(alpha = 0.35f), CircleShape)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Welcome to Audia Player",
            fontFamily = OutfitFontFamily, fontSize = 24.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Let's personalize your experience. What should we call you?",
            fontFamily = OutfitFontFamily, fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .border(1.dp, SoftGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = name,
                onValueChange = onNameChange,
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium),
                singleLine = true,
                cursorBrush = Brush.verticalGradient(listOf(Color.White, Color.White)),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (name.isEmpty()) {
                        Text("Your Name (Optional)", color = Color.White.copy(alpha = 0.3f), fontSize = 18.sp, fontFamily = OutfitFontFamily)
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
private fun FeaturesStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Premium Features",
            fontFamily = OutfitFontFamily, fontSize = 24.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))
        FeatureItem(Icons.Default.Schedule, "Smart Scheduler", "Schedule songs with precise fade-in volumes to wake up or relax.")
        Spacer(modifier = Modifier.height(24.dp))
        FeatureItem(Icons.Default.MusicNote, "A-B Looping", "Loop exact portions of your favorite tracks.")
        Spacer(modifier = Modifier.height(24.dp))
        FeatureItem(Icons.Default.CheckCircle, "Pro Audio FX", "Built-in Bass Boost and Zen Mode for perfect listening.")
    }
}

@Composable
private fun FeatureItem(icon: ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = SoftGold, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = OutfitFontFamily)
            Text(desc, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontFamily = OutfitFontFamily)
        }
    }
}

@Composable
private fun PermissionsStep(
    name: String,
    onComplete: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var musicGranted by remember { mutableStateOf(PermissionHelper.hasAudioPermission(context)) }
    var notifGranted by remember { mutableStateOf(PermissionHelper.hasNotificationPermission(context)) }
    var visualizerGranted by remember { mutableStateOf(PermissionHelper.hasRecordAudioPermission(context)) }
    var batteryGranted by remember { mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context)) }

    fun refreshPermissions() {
        musicGranted = PermissionHelper.hasAudioPermission(context)
        notifGranted = PermissionHelper.hasNotificationPermission(context)
        visualizerGranted = PermissionHelper.hasRecordAudioPermission(context)
        batteryGranted = PermissionHelper.isIgnoringBatteryOptimizations(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val mediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        musicGranted = granted
        refreshPermissions()
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted
        refreshPermissions()
    }

    val visualizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        visualizerGranted = granted
        refreshPermissions()
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.audia_player_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, SoftGold.copy(alpha = 0.4f), CircleShape)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (musicGranted) "You're All Set!" else "Permissions & Setup",
                fontFamily = OutfitFontFamily,
                fontSize = 20.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (musicGranted)
                    "Audio access is granted. Optional permissions can be configured below or anytime in Settings."
                    else "Audia Player needs access to your local audio tracks to play music.",
                fontFamily = OutfitFontFamily,
                fontSize = 12.5.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        // Permission Items List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PermissionCard(
                icon = Icons.Default.MusicNote,
                title = "Music & Audio Files",
                description = "Required to scan and play your offline music",
                isGranted = musicGranted,
                isRequired = true,
                onAction = {
                    mediaLauncher.launch(PermissionHelper.audioPermission)
                }
            )

            PermissionCard(
                icon = Icons.Default.Notifications,
                title = "Notification Controls",
                description = "Lock-screen & background playback controls",
                isGranted = notifGranted,
                isRequired = false,
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        PermissionHelper.openNotificationSettings(context)
                    }
                }
            )

            PermissionCard(
                icon = Icons.Default.BatteryChargingFull,
                title = "Battery Saver Whitelist",
                description = "Ensures scheduled alarms wake phone on time",
                isGranted = batteryGranted,
                isRequired = false,
                onAction = {
                    PermissionHelper.openBatteryOptimizationSettings(context)
                }
            )

            PermissionCard(
                icon = Icons.Default.GraphicEq,
                title = "Live Audio Visualizer",
                description = "Powers the real-time frequency spectrum",
                isGranted = visualizerGranted,
                isRequired = false,
                onAction = {
                    visualizerLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bottom CTA
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    if (!musicGranted) {
                        mediaLauncher.launch(PermissionHelper.audioPermission)
                    } else {
                        onComplete(name.ifBlank { "Friend" })
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftGold,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (musicGranted) "Start Using Audia" else "Grant Music Access",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = OutfitFontFamily
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Having trouble? ",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontFamily = OutfitFontFamily
                )
                Text(
                    text = "Open App Settings",
                    fontSize = 12.sp,
                    color = SoftGold,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = OutfitFontFamily,
                    modifier = Modifier.clickable { PermissionHelper.openAppSettings(context) }
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    isRequired: Boolean,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) Color(0x331E293B) else Color(0x40161822)
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isGranted) Color(0xFF4CAF50).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.10f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isGranted) Color(0x224CAF50) else Color(0x1AFFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF4CAF50) else SoftGold,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontFamily = OutfitFontFamily,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isRequired && !isGranted) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SoftGold.copy(alpha = 0.18f))
                                .border(0.5.dp, SoftGold.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = "REQUIRED",
                                fontFamily = OutfitFontFamily,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SoftGold,
                                softWrap = false
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontFamily = OutfitFontFamily,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 2,
                    lineHeight = 14.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isGranted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x2E4CAF50))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Enabled",
                        fontFamily = OutfitFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        softWrap = false
                    )
                }
            } else {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRequired) SoftGold else Color.White.copy(alpha = 0.15f),
                        contentColor = if (isRequired) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = if (isRequired) "Grant" else "Allow",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OutfitFontFamily,
                        softWrap = false
                    )
                }
            }
        }
    }
}
