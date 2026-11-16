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
import com.eplaytime.app.ui.components.FeaturedMostPlayedCarousel
import com.eplaytime.app.ui.components.TurntableCarousel

@Composable
fun SpotifyTracksFeed(
    allSongs: List<Song>,
    recentAddedSongs: List<Song>,
    recentlyPlayedSongs: List<Song>,
    mostPlayedSongs: List<Song>,
    currentSongId: Long?,
    onSongClick: (Song, List<Song>) -> Unit,
    state: LazyListState,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 150.dp
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        val turntableSongs = if (recentlyPlayedSongs.isNotEmpty()) recentlyPlayedSongs.take(10) else allSongs.take(10)
        if (turntableSongs.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HomeSectionTitle(
                        title = if (recentlyPlayedSongs.isNotEmpty()) "Recently Played" else "Featured Turntable",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    TurntableCarousel(
                        songs = turntableSongs,
                        currentSongId = currentSongId,
                        onSongClick = { song -> onSongClick(song, turntableSongs) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (mostPlayedSongs.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HomeSectionTitle(
                        title = "Most Played",
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    FeaturedMostPlayedCarousel(
                        songs = mostPlayedSongs.take(15),
                        currentSongId = currentSongId,
                        onSongClick = { song -> onSongClick(song, mostPlayedSongs) }
                    )
                }
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
                    onSongClick = { song -> onSongClick(song, recentAddedSongs) },
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
                onClick = { onSongClick(song, allSongs) }
            )
        }
    }
}

