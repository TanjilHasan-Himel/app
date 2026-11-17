package com.eplaytime.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.eplaytime.app.R
import com.eplaytime.app.data.model.RadioStation
import com.eplaytime.app.ui.theme.OutfitFontFamily
import com.eplaytime.app.ui.theme.SoftGold
import com.eplaytime.app.ui.theme.TextSecondary

@Composable
fun RadioMiniPlayer(
    station: RadioStation,
    nowPlaying: String?,
    isPlaying: Boolean,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(SoftGold.copy(alpha = 0.16f), Color(0xF016171D))))
            .clickable(onClick = onOpen)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = station.favicon.ifBlank { R.drawable.audia_player_logo },
            contentDescription = null,
            modifier = Modifier.size(52.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(station.name, fontFamily = OutfitFontFamily, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(nowPlaying ?: "LIVE • ${station.codec}", fontFamily = OutfitFontFamily, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onPlayPause, modifier = Modifier.size(48.dp)) {
            if (isPlaying) {
                Icon(
                    Icons.Default.Pause,
                    contentDescription = "Pause radio",
                    tint = SoftGold
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.icon),
                    contentDescription = "Play radio",
                    tint = SoftGold,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
