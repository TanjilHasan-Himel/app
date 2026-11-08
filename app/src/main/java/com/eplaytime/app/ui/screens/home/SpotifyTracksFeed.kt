package com.eplaytime.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.ui.components.TurntableCarousel

@Composable
fun SpotifyTracksFeed(
    allSongs: List<Song>,
    recentAddedSongs: List<Song>,
    recentlyPlayedSongs: List<Song>,
    mostPlayedSongs: List<Song>,
    currentSongId: Long?,
    onSongClick: (Song) -> Unit,
    state: LazyListState,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 150.dp
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        val turntableSongs = if (recentlyPlayedSongs.isNotEmpty()) recentlyPlayedSongs.take(15) else allSongs.take(15)
        if (turntableSongs.isNotEmpty()) {
            item {
                HomeSectionTitle(if (recentlyPlayedSongs.isNotEmpty()) "Recently Played" else "Featured Turntable")
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .layout { measurable, constraints ->
                            val extraPx = 32.dp.roundToPx()
                            val placeable = measurable.measure(
                                constraints.copy(
                                    minWidth = constraints.minWidth + extraPx,
                                    maxWidth = constraints.maxWidth + extraPx
                                )
                            )
                            layout(placeable.width, placeable.height) {
                                placeable.place(-16.dp.roundToPx(), 0)
                            }
                        }
                ) {
                    TurntableCarousel(
                        songs = turntableSongs.take(15),
                        currentSongId = currentSongId,
                        onSongClick = onSongClick
                    )
                }
            }
        }

        if (mostPlayedSongs.isNotEmpty()) {
            item {
                HomeSectionTitle("Most Played")
            }
            item {
                SongHorizontalRail(
                    songs = mostPlayedSongs.take(7),
                    currentSongId = currentSongId,
                    onSongClick = onSongClick,
                    cardWidth = cardWidth
                )
            }
        }

        if (recentAddedSongs.isNotEmpty()) {
            item {
                HomeSectionTitle("Recently Added")
            }
            item {
                SongHorizontalRail(
                    songs = recentAddedSongs.take(12),
                    currentSongId = currentSongId,
                    onSongClick = onSongClick,
                    cardWidth = cardWidth
                )
            }
        }

        item {
            HomeSectionTitle("All Tracks")
        }

        items(items = allSongs, key = { it.uri }, contentType = { "track_row" }) { song ->
            SongRowItem(
                song = song,
                isCurrent = song.id == currentSongId,
                onClick = { onSongClick(song) }
            )
        }
    }
}

