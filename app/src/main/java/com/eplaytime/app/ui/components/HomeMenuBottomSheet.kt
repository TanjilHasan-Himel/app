package com.eplaytime.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eplaytime.app.R
import com.eplaytime.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMenuBottomSheet(
    hasNewSongs: Boolean,
    onScanClick: () -> Unit,
    onSchedulerClick: () -> Unit,
    onEffectsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161616),
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
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, SoftGold.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Audia Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Audia Player",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Local-first audiophile music player",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Scan Library
            MenuActionItem(
                icon = Icons.Default.Refresh,
                title = "Scan Music Library",
                subtitle = if (hasNewSongs) "New music detected on storage" else "Refresh and index local audio tracks",
                badge = if (hasNewSongs) "NEW" else null,
                iconTint = if (hasNewSongs) Color(0xFFFF5252) else SoftGold,
                onClick = {
                    onDismiss()
                    onScanClick()
                }
            )

            // 2. Scheduler & Sleep
            MenuActionItem(
                icon = Icons.Default.Alarm,
                title = "Sleep Timer & Alarms",
                subtitle = "Set auto-stop timer or scheduled musical alarms",
                iconTint = SoftGold,
                onClick = {
                    onDismiss()
                    onSchedulerClick()
                }
            )

            // 3. Audio Effects & Equalizer
            MenuActionItem(
                icon = Icons.Default.Tune,
                title = "Audio Effects & EQ",
                subtitle = "Bass boost, virtualizer, and 5-band equalizer",
                iconTint = SoftGold,
                onClick = {
                    onDismiss()
                    onEffectsClick()
                }
            )

            // 4. Settings
            MenuActionItem(
                icon = Icons.Default.Settings,
                title = "Settings",
                subtitle = "Audio scan filters, folder controls, and appearance",
                iconTint = SoftGold,
                onClick = {
                    onDismiss()
                    onSettingsClick()
                }
            )

            // 5. About
            MenuActionItem(
                icon = Icons.Default.Info,
                title = "About Audia",
                subtitle = "Version, licenses, legal & developer info",
                iconTint = TextSecondary,
                onClick = {
                    onDismiss()
                    onAboutClick()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MenuActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                if (badge != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFF5252))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }
                }
            }
            Text(
                text = subtitle,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

