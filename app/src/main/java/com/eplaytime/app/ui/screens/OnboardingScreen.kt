package com.eplaytime.app.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.eplaytime.app.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

private enum class SetupRequirement {
    MEDIA,
    NOTIFICATIONS,
    RECORD_AUDIO,
    BATTERY_OPTIMIZATION
}

@OptIn(ExperimentalPermissionsApi::class)
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
                        Color(0xFF0F2027),
                        Color(0xFF203A43),
                        Color(0xFF2C5364)
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

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp)
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
                                if (pagerState.currentPage == index) Color.White
                                else Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            if (pagerState.currentPage < 2) {
                Button(
                    onClick = {
                        if (pagerState.currentPage == 0 && name.isBlank()) return@Button
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Next", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { onComplete(if (name.isBlank()) "Friend" else name) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Start Using", color = Color.Black, fontWeight = FontWeight.Bold)
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
            painter = painterResource(id = R.drawable.audia_player),
            contentDescription = null,
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Welcome to Audia Player",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Let's personalize your experience. What should we call you?",
            style = MaterialTheme.typography.bodyMedium,
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
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
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
                        Text("Your Name", color = Color.White.copy(alpha = 0.3f), fontSize = 18.sp)
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
            style = MaterialTheme.typography.headlineMedium,
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
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(desc, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionsStep(name: String, onComplete: (String) -> Unit) {
    val context = LocalContext.current
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val powerManager = context.getSystemService(PowerManager::class.java)
    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val mediaPermissionState = rememberPermissionState(mediaPermission)
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null
    val recordPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    var settingsRefreshTick by remember { mutableIntStateOf(0) }

    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        settingsRefreshTick++
    }

    val steps = remember {
        listOf(
            SetupRequirement.MEDIA,
            SetupRequirement.NOTIFICATIONS,
            SetupRequirement.RECORD_AUDIO,
            SetupRequirement.BATTERY_OPTIMIZATION
        )
    }

    fun isGranted(step: SetupRequirement): Boolean {
        return when (step) {
            SetupRequirement.MEDIA -> mediaPermissionState.status.isGranted
            SetupRequirement.NOTIFICATIONS -> notificationPermissionState?.status?.isGranted ?: true
            SetupRequirement.RECORD_AUDIO -> recordPermissionState.status.isGranted
            SetupRequirement.BATTERY_OPTIMIZATION -> Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true)
        }
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(
        mediaPermissionState.status.isGranted,
        notificationPermissionState?.status?.isGranted,
        recordPermissionState.status.isGranted,
        settingsRefreshTick
    ) {
        while (currentStepIndex < steps.size && isGranted(steps[currentStepIndex])) {
            currentStepIndex++
        }
    }

    val currentStep = steps.getOrNull(currentStepIndex)
    val allGranted = currentStep == null

    val title = when (currentStep) {
        SetupRequirement.MEDIA -> "Allow Music Access"
        SetupRequirement.NOTIFICATIONS -> "Allow Notifications"
        SetupRequirement.RECORD_AUDIO -> "Allow Visualizer"
        SetupRequirement.BATTERY_OPTIMIZATION -> "Allow Battery Bypass"
        null -> "All set"
    }
    val description = when (currentStep) {
        SetupRequirement.MEDIA -> "We need access to your audio files once so Audia Player can build your library."
        SetupRequirement.NOTIFICATIONS -> "Playback controls and scheduler alerts use notifications."
        SetupRequirement.RECORD_AUDIO -> "This powers the spectrum and visualizer in the player."
        SetupRequirement.BATTERY_OPTIMIZATION -> "This reduces missed schedules on aggressive battery systems."
        null -> "Everything is enabled. You won’t see these setup prompts again."
    }

    val buttonLabel = when (currentStep) {
        SetupRequirement.MEDIA -> "Grant Music Access"
        SetupRequirement.NOTIFICATIONS -> "Grant Notifications"
        SetupRequirement.RECORD_AUDIO -> "Grant Visualizer"
        SetupRequirement.BATTERY_OPTIMIZATION -> "Open Battery Settings"
        null -> "Start Using"
    }

    val buttonAction: () -> Unit = when (currentStep) {
        SetupRequirement.MEDIA -> ({ mediaPermissionState.launchPermissionRequest() })
        SetupRequirement.NOTIFICATIONS -> ({ notificationPermissionState?.launchPermissionRequest() })
        SetupRequirement.RECORD_AUDIO -> ({ recordPermissionState.launchPermissionRequest() })
        SetupRequirement.BATTERY_OPTIMIZATION -> ({
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            batteryOptimizationLauncher.launch(intent)
        })
        null -> ({ onComplete(if (name.isBlank()) "Friend" else name) })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.audia_player),
            contentDescription = null,
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (allGranted) "You’re ready, ${if (name.isBlank()) "Friend" else name}!" else title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PermissionRow("Music files", isGranted(SetupRequirement.MEDIA))
                PermissionRow("Notifications", isGranted(SetupRequirement.NOTIFICATIONS))
                PermissionRow("Visualizer", isGranted(SetupRequirement.RECORD_AUDIO))
                PermissionRow("Battery bypass", isGranted(SetupRequirement.BATTERY_OPTIMIZATION))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = buttonAction,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(buttonLabel, color = Color.Black, fontWeight = FontWeight.Bold)
        }

        if (!allGranted) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "We ask these once during first launch. After setup, no more popups.",
                color = Color.White.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (granted) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.25f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = if (granted) 0.95f else 0.65f),
            fontSize = 14.sp,
            fontWeight = if (granted) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
