package com.eplaytime.app.data.repository

import com.eplaytime.app.data.dao.PlaylistDao
import com.eplaytime.app.data.dao.SavedQueueDao
import com.eplaytime.app.data.model.Playlist
import com.eplaytime.app.data.model.PlaylistItem
import com.eplaytime.app.data.model.SavedQueue
import com.eplaytime.app.data.model.SavedQueueItem
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.util.M3uHelper
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PlaylistRepository - Single source of truth for persistent playlists & saved queues.
 */
@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val savedQueueDao: SavedQueueDao
) {
    /**
     * Reactive stream of all user playlists sorted by last update timestamp.
     */
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    suspend fun getPlaylistById(id: Long): Playlist? = playlistDao.getPlaylistById(id)

    fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItem>> =
        playlistDao.getPlaylistItems(playlistId)

    suspend fun createPlaylist(name: String, description: String = ""): Long {
        val trimmed = name.trim().ifBlank { "New Playlist" }
        val playlist = Playlist(
            name = trimmed,
            description = description.trim(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return playlistDao.insertPlaylist(playlist)
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return
        playlistDao.updatePlaylist(
            playlist.copy(
                name = newName.trim().ifBlank { playlist.name },
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.clearPlaylist(playlistId)
        playlistDao.deletePlaylistById(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, song: Song): Long {
        val currentMax = playlistDao.getMaxPosition(playlistId) ?: -1
        val nextPosition = currentMax + 1
        val item = PlaylistItem(
            playlistId = playlistId,
            songId = song.id,
            songUri = song.uri,
            title = song.title,
            artist = song.artist,
            album = song.album,
            duration = song.duration,
            albumArtUri = song.albumArtUri,
            position = nextPosition
        )
        val itemId = playlistDao.insertItem(item)
        touchPlaylist(playlistId)
        return itemId
    }

    suspend fun addSongsToPlaylist(playlistId: Long, songs: List<Song>): Int {
        var currentPosition = (playlistDao.getMaxPosition(playlistId) ?: -1) + 1
        val items = songs.map { song ->
            PlaylistItem(
                playlistId = playlistId,
                songId = song.id,
                songUri = song.uri,
                title = song.title,
                artist = song.artist,
                album = song.album,
                duration = song.duration,
                albumArtUri = song.albumArtUri,
                position = currentPosition++
            )
        }
        playlistDao.insertItems(items)
        touchPlaylist(playlistId)
        return items.size
    }

    suspend fun removePlaylistItem(playlistId: Long, itemId: Long) {
        playlistDao.deleteItemById(playlistId, itemId)
        // Normalize positions
        val remaining = playlistDao.getPlaylistItemsDirect(playlistId)
        playlistDao.updatePositions(remaining)
        touchPlaylist(playlistId)
    }

    suspend fun reorderPlaylist(playlistId: Long, newOrder: List<PlaylistItem>) {
        playlistDao.updatePositions(newOrder)
        touchPlaylist(playlistId)
    }

    private suspend fun touchPlaylist(playlistId: Long) {
        playlistDao.getPlaylistById(playlistId)?.let {
            playlistDao.updatePlaylist(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    /**
     * Exports a playlist into standard Extended M3U text format.
     */
    suspend fun exportPlaylistToM3u(playlistId: Long): String {
        val playlist = playlistDao.getPlaylistById(playlistId)
            ?: throw IllegalArgumentException("Playlist $playlistId not found")
        val items = playlistDao.getPlaylistItemsDirect(playlistId)
        return M3uHelper.generateM3u(playlist.name, items)
    }

    /**
     * Imports an M3U playlist file, resolving tracks against the device library.
     */
    suspend fun importM3u(name: String, m3uContent: String, knownSongs: List<Song>): Long {
        val entries = M3uHelper.parseM3u(m3uContent)
        val playlistId = createPlaylist(name)

        var position = 0
        val items = entries.map { entry ->
            // Match against local library by URI or Title+Artist
            val matchedSong = knownSongs.find { it.uri == entry.uriOrPath }
                ?: knownSongs.find {
                    it.title.equals(entry.title, ignoreCase = true) &&
                    (entry.artist.isBlank() || it.artist.equals(entry.artist, ignoreCase = true))
                }

            PlaylistItem(
                playlistId = playlistId,
                songId = matchedSong?.id ?: -1L,
                songUri = matchedSong?.uri ?: entry.uriOrPath,
                title = matchedSong?.title ?: entry.title,
                artist = matchedSong?.artist ?: entry.artist,
                album = matchedSong?.album ?: "Imported Playlist",
                duration = matchedSong?.duration ?: (entry.durationSeconds * 1000).coerceAtLeast(0),
                albumArtUri = matchedSong?.albumArtUri,
                position = position++
            )
        }

        if (items.isNotEmpty()) {
            playlistDao.insertItems(items)
        }
        return playlistId
    }

    /**
     * Saved Queue Persistence (Process Death / Session Snapshot)
     */
    suspend fun saveActiveQueue(
        title: String,
        songs: List<Song>,
        activeTrackIndex: Int,
        activePositionMs: Long
    ): Long {
        val queue = SavedQueue(
            title = title,
            activeTrackIndex = activeTrackIndex,
            activePositionMs = activePositionMs
        )
        val items = songs.mapIndexed { index, song ->
            SavedQueueItem(
                queueId = 0,
                songId = song.id,
                songUri = song.uri,
                title = song.title,
                artist = song.artist,
                album = song.album,
                duration = song.duration,
                albumArtUri = song.albumArtUri,
                position = index
            )
        }
        return savedQueueDao.saveFullQueue(queue, items)
    }

    suspend fun getSavedQueue(queueId: Long): Pair<SavedQueue, List<SavedQueueItem>>? {
        val queue = savedQueueDao.getSavedQueueById(queueId) ?: return null
        val items = savedQueueDao.getQueueItems(queueId)
        return Pair(queue, items)
    }
}

