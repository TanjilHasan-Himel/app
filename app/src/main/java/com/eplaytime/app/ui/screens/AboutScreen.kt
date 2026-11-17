package com.eplaytime.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.eplaytime.app.BuildConfig
import com.eplaytime.app.R
import com.eplaytime.app.ui.theme.*
import com.eplaytime.app.util.FirebaseDiagnostics
import com.eplaytime.app.util.Legals
import java.util.Calendar

/**
 * AboutScreen - Professional app information and credits
 * Shows developer info, version, and app details
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        FirebaseDiagnostics.recordScreen(context, "AboutScreen")
    }
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val versionName = BuildConfig.VERSION_NAME ?: "9.1.3"
    val releaseDate = "November 18, 2026"
    
    // State for Legal Dialogs
    var showEula by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }

    if (showEula) {
        LegalDialog(title = "Terms & Conditions", content = Legals.EULA) { showEula = false }
    }
    if (showPrivacy) {
        LegalDialog(title = "Privacy Policy", content = Legals.PRIVACY_POLICY) { showPrivacy = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "About Audia Player",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // App Logo
            AsyncImage(
                model = R.drawable.audia_player_logo,
                contentDescription = "Audia Player Logo",
                modifier = Modifier.size(120.dp)
            )

            // App Name
            Text(
                text = "Audia Player",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = TextPrimary
            )

            // Version
            Text(
                text = "Version $versionName",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = 14.sp,
                color = SoftGold
            )

            Text(
                text = "Pulse Release • $releaseDate",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = 12.sp,
                color = TextSecondary
            )

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.6f),
                color = Color.White.copy(alpha = 0.1f),
                thickness = 1.dp
            )

            // Developer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Build by",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = "Tanjil Hasan Himel",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )

                // Developer GitHub Profile Link
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, SoftGold.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/TanjilHasan-Himel"))
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "GitHub",
                        tint = SoftGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "github.com/TanjilHasan-Himel",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = SoftGold
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.6f),
                color = Color.White.copy(alpha = 0.1f),
                thickness = 1.dp
            )
            
            // Legal Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = { showEula = true }) {
                    Text("Terms & Conditions", color = SoftGold, fontFamily = OutfitFontFamily)
                }
                TextButton(onClick = { showPrivacy = true }) {
                    Text("Privacy Policy", color = SoftGold, fontFamily = OutfitFontFamily)
                }
            }

            // Description
            Text(
                text = "A professional offline music player with smart auto-play scheduling. Experience music your way.",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            // Footer
            Text(
                text = "© $currentYear Audia Player\nUpdated: $releaseDate\nAll rights reserved.",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun LegalDialog(title: String, content: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E1E).copy(alpha = 0.95f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = OutfitFontFamily)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = Color.Gray)
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                
                // Scrollable Content
                Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Text(
                        text = content,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontFamily = OutfitFontFamily,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                
                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftGold)
                ) {
                    Text("Close", color = Color.Black)
                }
            }
        }
    }
}
