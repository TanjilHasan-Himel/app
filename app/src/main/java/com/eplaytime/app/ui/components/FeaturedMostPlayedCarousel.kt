package com.eplaytime.app.ui.components

import android.animation.ValueAnimator
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.eplaytime.app.R
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.ui.theme.BlackSurface
import com.eplaytime.app.ui.theme.OutfitFontFamily
import com.eplaytime.app.ui.theme.SoftGold
import kotlinx.coroutines.launch

@Composable
fun FeaturedMostPlayedCarousel(
    songs: List<Song>,
    currentSongId: Long?,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { songs.size })
    val scope = rememberCoroutineScope()
    val motionEnabled = remember {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ValueAnimator.areAnimatorsEnabled()
    }
    val goToPage: (Int) -> Unit = { page ->
        scope.launch {
            val target = page.coerceIn(0, songs.lastIndex)
            if (motionEnabled) pagerState.animateScrollToPage(target) else pagerState.scrollToPage(target)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        val indicatorCount = minOf(songs.size, 7)
        val activeIndicator = (pagerState.currentPage * indicatorCount / songs.size)
            .coerceAtMost(indicatorCount - 1)
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your top repeated tracks",
                color = Color.White.copy(alpha = 0.68f),
                fontFamily = OutfitFontFamily,
                fontSize = 14.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { goToPage(pagerState.currentPage - 1) },
                    enabled = pagerState.currentPage > 0,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous most played song"
                    )
                }
                IconButton(
                    onClick = { goToPage(pagerState.currentPage + 1) },
                    enabled = pagerState.currentPage < songs.lastIndex,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next most played song"
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(indicatorCount) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .width(if (index == activeIndicator) 24.dp else 7.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == activeIndicator) SoftGold
                            else Color.White.copy(alpha = 0.22f)
                        )
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            pageSpacing = 14.dp,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val song = songs[page]
            val isCurrent = song.id == currentSongId
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.25f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(BlackSurface)
                    .then(
                        if (isCurrent) Modifier.border(1.5.dp, SoftGold, RoundedCornerShape(28.dp))
                        else Modifier
                    )
                    .semantics { role = Role.Button }
                    .clickable { onSongClick(song) }
            ) {
                AsyncImage(
                    model = if (!song.albumArtUri.isNullOrBlank()) song.albumArtUri else song.uri,
                    contentDescription = "${song.title} album artwork",
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.audia_player_logo),
                    error = painterResource(R.drawable.audia_player_logo),
                    fallback = painterResource(R.drawable.audia_player_logo),
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.50f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.92f)
                            )
                        )
                )
                Text(
                    text = "#${(page + 1).toString().padStart(2, '0')}",
                    color = SoftGold,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.58f))
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 25.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = song.artist.ifBlank { "Unknown artist" },
                        color = Color.White.copy(alpha = 0.76f),
                        fontFamily = OutfitFontFamily,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
