package com.eplaytime.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.VideoView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.zIndex
import com.eplaytime.app.ui.components.InAppNotificationCapsule
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.eplaytime.app.ui.screens.HomeScreen
import com.eplaytime.app.ui.screens.SplashScreen
import com.eplaytime.app.ui.theme.BlackBackground
import com.eplaytime.app.ui.theme.OutfitFontFamily
import com.eplaytime.app.ui.theme.SoftGold
import com.eplaytime.app.ui.theme.PlayTimeTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.eplaytime.app.ui.viewmodel.MusicViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val PERMISSION_CODE = 100
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            PlayTimeTheme {
                MainScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scheduleEngagementWorker()
        scheduleRecommendationWorker()
    }

    private fun scheduleEngagementWorker() {
        // Schedule worker to run periodically
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.eplaytime.app.worker.EngagementWorker>(
            2, java.util.concurrent.TimeUnit.HOURS
        ).build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "engagement_worker",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE, // Update policy to ensure latest worker version
            workRequest
        )
    }

    private fun scheduleRecommendationWorker() {
        // Schedule recommendations every 3 hours
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.eplaytime.app.worker.SongRecommendationWorker>(
            3, java.util.concurrent.TimeUnit.HOURS
        ).build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "song_recommendation_worker",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}

fun sendWelcomeNotification(context: android.content.Context) {
    val nm = context.getSystemService(android.app.NotificationManager::class.java)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val channel = android.app.NotificationChannel("playtime_welcome", "Welcome", android.app.NotificationManager.IMPORTANCE_DEFAULT)
        nm.createNotificationChannel(channel)
    }
    val notification = androidx.core.app.NotificationCompat.Builder(context, "playtime_welcome")
        .setSmallIcon(com.eplaytime.app.R.drawable.ic_audia_notification)
        .setContentTitle("Welcome to Audia Player!")
        .setContentText("Your music sounds better with us.")
        .setAutoCancel(true)
        .build()
    nm.notify(1001, notification)
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dataStore = remember { com.eplaytime.app.data.datastore.PlayTimeDataStore(context) }
    val hasShownWelcome by dataStore.hasShownWelcome.collectAsState(initial = true)

    LaunchedEffect(hasShownWelcome) {
        if (!hasShownWelcome) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    sendWelcomeNotification(context)
                    dataStore.setHasShownWelcome(true)
                }
            } else {
                sendWelcomeNotification(context)
                dataStore.setHasShownWelcome(true)
            }
        }
    }

    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(
            onSplashComplete = { showSplash = false }
        )
    } else {
        MainNavigation()
    }
}

@Composable
private fun MainNavigation() {
    val navController = rememberNavController()
    val sharedViewModel: MusicViewModel = hiltViewModel()
    val inAppNotification by sharedViewModel.inAppNotification.collectAsState()

    LaunchedEffect(Unit) {
        sharedViewModel.updateLastOpenedTime()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = sharedViewModel,
                    onNavigateToScheduler = { navController.navigate("scheduler") },
                    onNavigateToPlayer = { navController.navigate("player") },
                    onNavigateToAbout = { navController.navigate("about") },
                    onNavigateToQueue = { navController.navigate("queue") },
                    onNavigateToFolder = { path -> 
                        val encodedPath = Uri.encode(path)
                        navController.navigate("folders?path=$encodedPath") 
                    }
                )
            }

            composable("scheduler") {
                com.eplaytime.app.ui.screens.SchedulerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    musicViewModel = sharedViewModel
                )
            }

            composable("player") {
                com.eplaytime.app.ui.screens.PlayerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToQueue = { navController.navigate("queue") },
                    viewModel = sharedViewModel
                )
            }

            composable("folders?path={path}", 
                arguments = listOf(androidx.navigation.navArgument("path") { 
                    nullable = true 
                    defaultValue = null 
                    type = androidx.navigation.NavType.StringType
                })
            ) { backStackEntry ->
                val path = backStackEntry.arguments?.getString("path")
                com.eplaytime.app.ui.screens.FolderScreen(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = sharedViewModel,
                    initialFolderPath = path
                )
            }

            composable("about") {
                com.eplaytime.app.ui.screens.AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("queue") {
                com.eplaytime.app.ui.screens.QueueScreen(
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = sharedViewModel
                )
            }
        }

        // Global In-App Notification Capsule (Apple Dynamic Island / Luxury Floating HUD)
        InAppNotificationCapsule(
            message = inAppNotification,
            onDismiss = { sharedViewModel.dismissInAppNotification() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 10.dp)
                .zIndex(9999f)
        )
    }
}