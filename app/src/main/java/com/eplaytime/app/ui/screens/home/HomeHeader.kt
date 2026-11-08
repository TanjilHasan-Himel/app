package com.eplaytime.app.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eplaytime.app.R
import com.eplaytime.app.ui.components.BounceIconButton
import com.eplaytime.app.ui.theme.*
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeHeader(
    onSchedulerClick: () -> Unit,
    onAboutClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRefreshClick: () -> Unit,
    isLoading: Boolean,
    hasNewSongs: Boolean,
    userName: String?,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    searchIconOffset: Dp = 0.dp,
    refreshIconOffset: Dp = 0.dp,
    otherIconsOffset: Dp = 0.dp
) {
    val haptic = LocalHapticFeedback.current
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Shimmer Animation for Logo Border
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            SoftGold.copy(alpha = 0.3f),
            SoftGold,
            SoftGold.copy(alpha = 0.3f)
        ),
        start = Offset(0f, 0f),
        end = Offset(shimmerTranslate, shimmerTranslate)
    )

    // Pulse animation for New Songs
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (hasNewSongs) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Dynamic Greeting Logic
    val greeting = remember(userName) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..4 -> "Still Awake?"
            in 5..7 -> "Early Morning"
            in 8..11 -> "Good Morning"
            in 12..13 -> "Good Noon"
            in 14..16 -> "Good Afternoon"
            in 17..18 -> "Good Evening"
            in 19..21 -> "Calm Evening"
            in 22..23 -> "Good Night"
            else -> "Midnight Focus"
        }
    }

    val singleLineTitle = remember(userName, greeting) {
        if (!userName.isNullOrBlank()) {
            "$greeting, $userName"
        } else {
            "Audia Player"
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // LEFT: Name Plate - Single-line, well aligned, Echo Music / YT Music style
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Sleek Frosted Glass Badge with Audia Logo
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, shimmerBrush, RoundedCornerShape(12.dp))
                        .clickable { onAboutClick() }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                    )
                }

                Text(
                    text = singleLineTitle,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // RIGHT: Clean Actions (Search + Burger / Sandwich Menu)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Search
                BounceIconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .size(38.dp)
                        .offset(y = searchIconOffset)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SoftGold,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 2. Burger / Sandwich Menu (Drawer / Menu Sheet)
                BounceIconButton(
                    onClick = onMenuClick,
                    modifier = Modifier
                        .size(38.dp)
                        .scale(if (hasNewSongs) pulseScale else 1f)
                        .offset(y = otherIconsOffset)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = if (hasNewSongs) Color(0xFFFF5252) else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        if (hasNewSongs) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF5252))
                            )
                        }
                    }
                }
            }
        }
    }
}

