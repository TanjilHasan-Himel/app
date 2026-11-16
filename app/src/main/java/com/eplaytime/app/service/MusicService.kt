package com.eplaytime.app.service

import com.eplaytime.app.R

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.eplaytime.app.data.repository.MusicRepository
import com.eplaytime.app.data.dao.PlaylistDao
import com.eplaytime.app.data.model.Playlist
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.eplaytime.app.util.AudioEffectManager

@AndroidEntryPoint
class MusicService : MediaLibraryService() {

    companion object {
        const val ACTION_PLAY_ALARM = "com.eplaytime.app.action.PLAY_ALARM"
        const val ACTION_STOP_ALARM = "com.eplaytime.app.action.STOP_ALARM"
        const val EXTRA_SONG_URI = "extra_song_uri"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_TARGET_VOLUME = "extra_target_volume"
        const val EXTRA_ALARM_SINGLE_SHOT = "extra_alarm_single_shot"
        const val EXTRA_ALARM_CONTENT_URIS = "extra_alarm_content_uris"
        const val EXTRA_ALARM_FADE_SECONDS = "extra_alarm_fade_seconds"
        const val EXTRA_ALARM_STOP_AFTER_MINUTES = "extra_alarm_stop_after_minutes"
        const val EXTRA_ALARM_RECOVERY = "extra_alarm_recovery"
        const val EXTRA_ALARM_ORIGINAL_VOLUME = "extra_alarm_original_volume"
        const val NOTIFICATION_ID_ALARM = 12345
        private const val TAG = "MusicService"

        // Android Auto & MediaBrowser navigation constants
        const val MEDIA_ROOT_ID = "root"
        const val MEDIA_CATEGORY_TRACKS = "category_tracks"
        const val MEDIA_CATEGORY_PLAYLISTS = "category_playlists"
        const val MEDIA_CATEGORY_ALBUMS = "category_albums"
        const val MEDIA_CATEGORY_ARTISTS = "category_artists"
        const val MEDIA_CATEGORY_FAVORITES = "category_favorites"
    }

    @Inject
    lateinit var musicRepository: MusicRepository
    @Inject
    lateinit var favoritesRepository: com.eplaytime.app.data.repository.FavoritesRepository
    @Inject
    lateinit var playlistDao: com.eplaytime.app.data.dao.PlaylistDao
    @Inject
    lateinit var scheduledTaskDao: com.eplaytime.app.data.dao.ScheduledTaskDao

    private var player: ExoPlayer? = null
    private var mediaSession: MediaLibrarySession? = null
    private var isAlarmSingleShotMode = false
    private var fadeCoroutine: kotlinx.coroutines.Job? = null
    private var scheduledStopJob: kotlinx.coroutines.Job? = null
    private var activeScheduledTaskId = -1L
    private var previousSystemVolume: Int? = null
    private val serviceScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate
    )
    private var radioRetryCount = 0
    private var radioFallbackTried = false
    private val retryHandler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            recoverInterruptedScheduledSession()
            return START_STICKY
        }
        if (intent?.action == ACTION_STOP_ALARM) {
            Log.d(TAG, "Stop alarm action received, terminating scheduled playback")
            isAlarmSingleShotMode = false
            fadeCoroutine?.cancel()
            scheduledStopJob?.cancel()
            player?.stop()
            player?.clearMediaItems()
            finishScheduledSession("STOPPED")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_PLAY_ALARM) {
            val uriString = intent.getStringExtra(EXTRA_SONG_URI)
            val volume = intent.getFloatExtra(EXTRA_TARGET_VOLUME, 1.0f)
            val singleShot = intent.getBooleanExtra(EXTRA_ALARM_SINGLE_SHOT, true)
            val isRecovery = intent.getBooleanExtra(EXTRA_ALARM_RECOVERY, false)
            activeScheduledTaskId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
            
            if (uriString != null) {
                // 1. Override the system media volume to perfectly match exactly what the user scheduled
                try {
                    val audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                    val maxSystemVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                    previousSystemVolume = intent.getIntExtra(EXTRA_ALARM_ORIGINAL_VOLUME, -1)
                        .takeIf { it >= 0 }
                        ?: audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                    if (!isRecovery && activeScheduledTaskId >= 0) serviceScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        scheduledTaskDao.updateOriginalSystemVolume(
                            activeScheduledTaskId,
                            previousSystemVolume ?: -1
                        )
                    }
                    
                    val devices = audioManager.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
                    val hasHeadphones = devices.any {
                        it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                            it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                            it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                            it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                            it.type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET
                    }
                    
                    val safeVolume = if (hasHeadphones) volume.coerceAtMost(0.5f) else volume
                    val targetSystemVolume = (maxSystemVolume * safeVolume.coerceIn(0f, 1f)).toInt()
                    
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, targetSystemVolume, 0)
                    Log.d(TAG, "System STREAM_MUSIC volume overridden to exactly $targetSystemVolume / $maxSystemVolume (Headphones: $hasHeadphones)")
                } catch (e: Exception) {
                    Log.e(TAG, "Could not override system volume (might lack permission)", e)
                }


                val songTitle = intent.getStringExtra("extra_song_title") ?: "Alarm Playback"
                val stopIntent = Intent(this, MusicService::class.java).apply {
                    action = ACTION_STOP_ALARM
                }
                val stopPendingIntent = android.app.PendingIntent.getService(
                    this,
                    1,
                    stopIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )

                // Start absolute FGS immediately to prevent Android 12+ 10-second timeout FGS crash
                try {
                    val nm = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        val channel = android.app.NotificationChannel("playtime_al", "Audia Player Scheduler", android.app.NotificationManager.IMPORTANCE_HIGH)
                        nm.createNotificationChannel(channel)
                    }
                    val notification = androidx.core.app.NotificationCompat.Builder(this, "playtime_al")
                        .setSmallIcon(R.drawable.ic_audia_notification)
                        .setContentTitle(songTitle)
                        .setContentText("Playing scheduled alarm")
                        .setOngoing(true)
                        .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Alarm", stopPendingIntent)
                        .build()
                    startForeground(NOTIFICATION_ID_ALARM, notification)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to startForeground", e)
                }


                val metadata = androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(songTitle)
                    .setArtist("Audia Player Scheduler")
                    .build()

                val contentUris = intent.getStringArrayListExtra(EXTRA_ALARM_CONTENT_URIS)
                    ?.filter(String::isNotBlank)
                    .orEmpty()
                    .ifEmpty { listOf(uriString) }
                val items = contentUris.mapIndexed { index, uri ->
                    MediaItem.Builder()
                        .setMediaId("schedule:$activeScheduledTaskId:$index")
                        .setUri(uri)
                        .setMediaMetadata(metadata)
                        .build()
                }
                player?.let { exo ->
                    isAlarmSingleShotMode = singleShot
                    exo.stop()
                    exo.clearMediaItems()
                    exo.repeatMode = Player.REPEAT_MODE_OFF
                    exo.shuffleModeEnabled = false
                    exo.setMediaItems(items)
                    
                    // 2. Start internally at completely silent (0f) for the fade-in effect
                    exo.volume = 0f
                    exo.prepare()
                    exo.play()

                    // 3. Launch a smooth fade-in coroutine gracefully turning it up over 10 seconds
                    fadeCoroutine?.cancel()
                    fadeCoroutine = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        val fadeDuration = intent.getIntExtra(EXTRA_ALARM_FADE_SECONDS, 10)
                            .coerceIn(0, 120) * 1_000L
                        val fadeInterval = 100L    // update every 100ms
                        val steps = (fadeDuration / fadeInterval).toInt().coerceAtLeast(1)
                        
                        // We fade internal volume from 0 to 1.0 (since the system-level volume is now perfect)
                        val targetInternalVolume = 1.0f
                        val volumeStep = targetInternalVolume / steps

                        for (i in 1..steps) {
                            kotlinx.coroutines.delay(fadeInterval)
                            if (player == null || player?.playWhenReady == false) break
                            exo.volume = (exo.volume + volumeStep).coerceAtMost(targetInternalVolume)
                        }
                        
                        exo.volume = targetInternalVolume
                        Log.d(TAG, "Fade-in playback complete and reached max level")
                    }

                    scheduledStopJob?.cancel()
                    val stopAfterMinutes = intent.getIntExtra(EXTRA_ALARM_STOP_AFTER_MINUTES, 0)
                    if (stopAfterMinutes > 0) {
                        scheduledStopJob = serviceScope.launch {
                            delay(stopAfterMinutes.coerceAtMost(24 * 60) * 60_000L)
                            exo.stop()
                            exo.clearMediaItems()
                            finishScheduledSession("COMPLETED")
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                        }
                    }

                    serviceScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        if (activeScheduledTaskId >= 0) {
                            scheduledTaskDao.getTaskById(activeScheduledTaskId)?.let { task ->
                                scheduledTaskDao.updateRunState(
                                    task.id, System.currentTimeMillis(), "PLAYING",
                                    task.activeCycleId, 0, task.recoveryAttempts
                                )
                            }
                        }
                    }

                    Log.d(TAG, "Playback scheduler started. singleShot=$singleShot uri=$uriString")
                }
            }
        }
        return if (intent?.action == ACTION_PLAY_ALARM) START_STICKY
        else super.onStartCommand(intent, flags, startId)
    }

    // Callback for MediaSession & Android Auto MediaLibrary events
    @OptIn(UnstableApi::class)
    private val sessionCallback = object : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val isInternal = controller.packageName == packageName
            val sessionCommandsBuilder = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
            // Security: only expose internal DSP session ID command to Audia's own UI
            if (isInternal) {
                sessionCommandsBuilder.add(SessionCommand("ACTION_GET_AUDIO_SESSION_ID", Bundle.EMPTY))
            }
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommandsBuilder.build())
                .build()
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId(MEDIA_ROOT_ID)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle("Audia")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val settable = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            serviceScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    var currentSongs = musicRepository.songs.value
                    if (currentSongs.isEmpty()) {
                        currentSongs = musicRepository.getAllSongs()
                    }
                    val items = buildMediaChildren(parentId, currentSongs)
                    settable.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
                } catch (e: Exception) {
                    Log.e(TAG, "Error building media children for $parentId", e)
                    settable.set(LibraryResult.ofItemList(ImmutableList.of(), params))
                }
            }
            return settable
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            if (mediaId.startsWith("song_")) {
                val songId = mediaId.removePrefix("song_").toLongOrNull()
                val song = musicRepository.songs.value.find { it.id == songId }
                if (song != null) {
                    return Futures.immediateFuture(LibraryResult.ofItem(songToMediaItem(song), null))
                }
            }
            return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            val currentSongs = musicRepository.songs.value
            val resolvedItems = mediaItems.map { item ->
                if (item.requestMetadata.mediaUri != null) {
                    item
                } else {
                    val idStr = item.mediaId
                    if (idStr.startsWith("song_")) {
                        val songId = idStr.removePrefix("song_").toLongOrNull()
                        val song = currentSongs.find { it.id == songId }
                        if (song != null) songToMediaItem(song) else item
                    } else item
                }
            }
            return Futures.immediateFuture(resolvedItems)
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<Void>> {
            session.notifySearchResultChanged(browser, query, 50, params)
            return Futures.immediateFuture(LibraryResult.ofVoid(params))
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val settable = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
            serviceScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    var currentSongs = musicRepository.songs.value
                    if (currentSongs.isEmpty()) {
                        currentSongs = musicRepository.getAllSongs()
                    }
                    val q = query.trim().lowercase()
                    val matched = currentSongs.filter {
                        it.title.lowercase().contains(q) ||
                        it.artist.lowercase().contains(q) ||
                        it.album.lowercase().contains(q)
                    }.take(50).map { songToMediaItem(it) }
                    settable.set(LibraryResult.ofItemList(ImmutableList.copyOf(matched), params))
                } catch (e: Exception) {
                    Log.e(TAG, "Error performing search for $query", e)
                    settable.set(LibraryResult.ofItemList(ImmutableList.of(), params))
                }
            }
            return settable
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            command: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (controller.packageName != packageName) {
                Log.w(TAG, "Rejected custom command ${command.customAction} from external package: ${controller.packageName}")
                return Futures.immediateFuture(SessionResult(SessionError.ERROR_PERMISSION_DENIED))
            }
            return try {
                val action = command.customAction
                if (action == "ACTION_GET_AUDIO_SESSION_ID") {
                    val sessionId = player?.audioSessionId?.takeIf { it != C.AUDIO_SESSION_ID_UNSET && it > 0 }
                        ?: AudioEffectManager.activeSessionId.value
                    if (sessionId > 0) {
                        AudioEffectManager.initialize(sessionId)
                        val extras = Bundle().apply {
                            putInt("AUDIO_SESSION_ID", sessionId)
                        }
                        session.setSessionExtras(extras)
                    }
                    val extras = Bundle()
                    extras.putInt("AUDIO_SESSION_ID", sessionId)
                    Log.d(TAG, "Responding to ACTION_GET_AUDIO_SESSION_ID with session: $sessionId")
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, extras))
                } else {
                    Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling custom command", e)
                Futures.immediateFuture(SessionResult(SessionError.ERROR_UNKNOWN))
            }
        }
    }

    private fun buildBrowsableItem(id: String, title: String, subtitle: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()
    }

    private fun songToMediaItem(song: com.eplaytime.app.data.model.Song): MediaItem {
        val songUri = if (song.uri.isNotBlank()) {
            Uri.parse(song.uri)
        } else {
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
        }
        return MediaItem.Builder()
            .setMediaId("song_${song.id}")
            .setUri(songUri)
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(songUri).build())
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.albumArtUri?.let { Uri.parse(it) })
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
    }

    private fun favoriteToMediaItem(fav: com.eplaytime.app.data.database.FavoriteSong): MediaItem {
        val songUri = if (fav.uri.isNotBlank()) {
            Uri.parse(fav.uri)
        } else {
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, fav.id)
        }
        return MediaItem.Builder()
            .setMediaId("song_${fav.id}")
            .setUri(songUri)
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(songUri).build())
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(fav.title)
                    .setArtist(fav.artist)
                    .setArtworkUri(fav.albumArtUri?.let { Uri.parse(it) })
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
    }

    private suspend fun buildMediaChildren(parentId: String, songs: List<com.eplaytime.app.data.model.Song>): List<MediaItem> {
        val favorites = try {
            favoritesRepository.getAllFavorites().firstOrNull() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        return when (parentId) {
            MEDIA_ROOT_ID -> {
                listOf(
                    buildBrowsableItem(MEDIA_CATEGORY_TRACKS, "All Songs", "${songs.size} tracks"),
                    buildBrowsableItem(MEDIA_CATEGORY_PLAYLISTS, "Playlists", "Custom playlists"),
                    buildBrowsableItem(MEDIA_CATEGORY_ALBUMS, "Albums", "Browse by album"),
                    buildBrowsableItem(MEDIA_CATEGORY_ARTISTS, "Artists", "Browse by artist"),
                    buildBrowsableItem(MEDIA_CATEGORY_FAVORITES, "Favorites", "${favorites.size} tracks")
                )
            }
            MEDIA_CATEGORY_TRACKS -> {
                songs.map { songToMediaItem(it) }
            }
            MEDIA_CATEGORY_PLAYLISTS -> {
                val playlists: List<Playlist> = try {
                    playlistDao.getAllPlaylists().firstOrNull() ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
                playlists.map { playlist ->
                    buildBrowsableItem(
                        "playlist_${playlist.id}",
                        playlist.name,
                        playlist.description.ifBlank { "Playlist" }
                    )
                }
            }
            MEDIA_CATEGORY_FAVORITES -> {
                favorites.map { favoriteToMediaItem(it) }
            }
            MEDIA_CATEGORY_ALBUMS -> {
                songs.groupBy { it.album }.map { (albumName, albumSongs) ->
                    val firstSong = albumSongs.firstOrNull()
                    val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(albumName.ifBlank { "Unknown Album" })
                        .setArtist(firstSong?.artist ?: "Unknown Artist")
                        .setArtworkUri(firstSong?.albumArtUri?.let { Uri.parse(it) })
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                    MediaItem.Builder()
                        .setMediaId("album_$albumName")
                        .setMediaMetadata(mediaMetadata)
                        .build()
                }
            }
            MEDIA_CATEGORY_ARTISTS -> {
                songs.groupBy { it.artist }.map { (artistName, artistSongs) ->
                    val firstSong = artistSongs.firstOrNull()
                    val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(artistName.ifBlank { "Unknown Artist" })
                        .setArtworkUri(firstSong?.albumArtUri?.let { Uri.parse(it) })
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                    MediaItem.Builder()
                        .setMediaId("artist_$artistName")
                        .setMediaMetadata(mediaMetadata)
                        .build()
                }
            }
            else -> {
                when {
                    parentId.startsWith("album_") -> {
                        val albumName = parentId.removePrefix("album_")
                        songs.filter { it.album == albumName }.map { songToMediaItem(it) }
                    }
                    parentId.startsWith("artist_") -> {
                        val artistName = parentId.removePrefix("artist_")
                        songs.filter { it.artist == artistName }.map { songToMediaItem(it) }
                    }
                    parentId.startsWith("playlist_") -> {
                        val playlistId = parentId.removePrefix("playlist_").toLongOrNull() ?: return emptyList()
                        val items = try {
                            playlistDao.getPlaylistItemsDirect(playlistId)
                        } catch (e: Exception) {
                            emptyList()
                        }
                        items.map { item ->
                            val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
                                .setTitle(item.title)
                                .setArtist(item.artist)
                                .setAlbumTitle(item.album)
                                .setArtworkUri(item.albumArtUri?.let { Uri.parse(it) })
                                .setIsBrowsable(false)
                                .setIsPlayable(true)
                                .build()
                            MediaItem.Builder()
                                .setMediaId("song_${item.songId}")
                                .setUri(item.songUri)
                                .setMediaMetadata(mediaMetadata)
                                .build()
                        }
                    }
                    else -> emptyList()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        initializePlayer()
        player?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                radioRetryCount = 0
                radioFallbackTried = false
                val taskId = activeScheduledTaskId
                // Read player on Main thread to avoid IllegalStateException: Player is accessed on the wrong thread
                val currentTrackIndex = player?.currentMediaItemIndex?.coerceAtLeast(0) ?: 0
                if (taskId >= 0) serviceScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    scheduledTaskDao.getTaskById(taskId)?.let { task ->
                        scheduledTaskDao.updateRunState(
                            task.id,
                            task.lastRunAt,
                            "PLAYING",
                            task.activeCycleId,
                            currentTrackIndex,
                            task.recoveryAttempts
                        )
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                com.eplaytime.app.util.FirebaseDiagnostics.recordPlaybackState(if (isPlaying) "PLAYING" else "PAUSED")
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (isAlarmSingleShotMode && playbackState == Player.STATE_ENDED) {
                    player?.pause()
                    player?.seekTo(0L)
                    isAlarmSingleShotMode = false
                    finishScheduledSession("COMPLETED")
                    Log.d(TAG, "Alarm single-shot completed, stopping service")
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (isAlarmSingleShotMode) {
                    isAlarmSingleShotMode = false
                    finishScheduledSession("FAILED")
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return
                }
                val exo = player ?: return
                val item = exo.currentMediaItem ?: return
                if (!item.mediaId.startsWith("radio:")) return

                val fallback = item.mediaMetadata.extras?.getString("fallback_url").orEmpty()
                if (!radioFallbackTried && fallback.isNotBlank() &&
                    item.localConfiguration?.uri?.toString() != fallback
                ) {
                    radioFallbackTried = true
                    exo.setMediaItem(item.buildUpon().setUri(fallback).build())
                    exo.prepare()
                    exo.play()
                    return
                }

                if (radioRetryCount < 3) {
                    val delayMs = 1_000L shl radioRetryCount
                    radioRetryCount++
                    retryHandler.postDelayed({
                        player?.takeIf { it.currentMediaItem?.mediaId == item.mediaId }?.let {
                            it.prepare()
                            it.play()
                        }
                    }, delayMs)
                }
            }
        })
    }

    private fun finishScheduledSession(result: String) {
        fadeCoroutine?.cancel()
        scheduledStopJob?.cancel()
        previousSystemVolume?.let { original ->
            runCatching {
                val audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, original, 0)
            }.onFailure { Log.e(TAG, "Unable to restore media volume", it) }
        }
        previousSystemVolume = null
        val taskId = activeScheduledTaskId
        activeScheduledTaskId = -1L
        if (taskId >= 0) serviceScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            scheduledTaskDao.getTaskById(taskId)?.let { task ->
                scheduledTaskDao.updateRunState(
                    task.id, task.lastRunAt, result, "", 0, 0
                )
                scheduledTaskDao.updateOriginalSystemVolume(task.id, -1)
            }
        }
    }

    private fun recoverInterruptedScheduledSession() {
        serviceScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val task = scheduledTaskDao.getRecoverableTask() ?: return@launch
            if (task.recoveryAttempts >= 2) {
                restorePersistedVolume(task.originalSystemVolume)
                scheduledTaskDao.updateRunState(task.id, task.lastRunAt, "FAILED", "", 0, 0)
                scheduledTaskDao.updateOriginalSystemVolume(task.id, -1)
                return@launch
            }
            scheduledTaskDao.updateRunState(
                task.id,
                task.lastRunAt,
                "STARTING",
                task.activeCycleId,
                task.activeTrackIndex,
                task.recoveryAttempts + 1
            )
            val recoveryIntent = Intent(this@MusicService, MusicService::class.java).apply {
                action = ACTION_PLAY_ALARM
                putExtra(EXTRA_SONG_URI, task.songUri)
                putExtra(EXTRA_ALARM_ID, task.id)
                putExtra(EXTRA_TARGET_VOLUME, task.targetVolume)
                putExtra("extra_song_title", task.songTitle)
                putExtra(EXTRA_ALARM_SINGLE_SHOT, true)
                putExtra(EXTRA_ALARM_FADE_SECONDS, task.fadeInSeconds)
                putExtra(EXTRA_ALARM_STOP_AFTER_MINUTES, task.stopAfterMinutes)
                putExtra(EXTRA_ALARM_RECOVERY, true)
                putExtra(EXTRA_ALARM_ORIGINAL_VOLUME, task.originalSystemVolume)
                putStringArrayListExtra(
                    EXTRA_ALARM_CONTENT_URIS,
                    ArrayList(
                        task.contentUris.lineSequence().filter(String::isNotBlank).toList()
                            .ifEmpty { listOf(task.songUri) }
                    )
                )
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onStartCommand(recoveryIntent, 0, 0)
                player?.seekToDefaultPosition(task.activeTrackIndex.coerceAtLeast(0))
            }
        }
    }

    private fun restorePersistedVolume(volume: Int) {
        if (volume < 0) return
        runCatching {
            val audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, volume, 0)
        }.onFailure { Log.e(TAG, "Unable to restore persisted media volume", it) }
    }

    private fun markScheduledInterrupted() {
        val taskId = activeScheduledTaskId
        // Read player on Main thread to avoid IllegalStateException: Player is accessed on the wrong thread
        val currentTrackIndex = player?.currentMediaItemIndex?.coerceAtLeast(0)
        restorePersistedVolume(previousSystemVolume ?: -1)
        previousSystemVolume = null
        if (taskId >= 0) serviceScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            scheduledTaskDao.getTaskById(taskId)?.let { task ->
                scheduledTaskDao.updateRunState(
                    task.id,
                    task.lastRunAt,
                    "INTERRUPTED",
                    task.activeCycleId,
                    currentTrackIndex ?: task.activeTrackIndex,
                    task.recoveryAttempts
                )
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL) // Keep CPU awake
            .build()

        val audioManager = getSystemService(AUDIO_SERVICE) as? android.media.AudioManager
        val generatedSessionId = audioManager?.generateAudioSessionId() ?: C.AUDIO_SESSION_ID_UNSET
        if (generatedSessionId > 0 && generatedSessionId != C.AUDIO_SESSION_ID_UNSET) {
            try {
                player?.setAudioSessionId(generatedSessionId)
                AudioEffectManager.initialize(generatedSessionId)
                Log.d(TAG, "Assigned proactive audioSessionId: $generatedSessionId to ExoPlayer")
            } catch (e: Exception) {
                Log.e(TAG, "Failed setting audioSessionId on ExoPlayer", e)
            }
        }

        // Intent to open app when clicking notification
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        val sessionExtras = Bundle().apply {
            val sid = player?.audioSessionId?.takeIf { it != C.AUDIO_SESSION_ID_UNSET && it > 0 }
                ?: AudioEffectManager.activeSessionId.value
            putInt("AUDIO_SESSION_ID", sid)
        }

        mediaSession = MediaLibrarySession.Builder(this, player!!, sessionCallback)
            .setId("MusicService")
            .setSessionActivity(pendingIntent!!)
            .setExtras(sessionExtras)
            .build()

        // Ensure Notification Channel for Media Playback has VISIBILITY_PUBLIC for Android 11/12 Lock Screen
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val nm = getSystemService(android.app.NotificationManager::class.java)
            val channel = android.app.NotificationChannel(
                "default_channel_id",
                "Music Playback",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Playback controls and lock screen media controls"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
            }
            nm?.createNotificationChannel(channel)
        }

        try {
            val notificationProvider = androidx.media3.session.DefaultMediaNotificationProvider.Builder(this)
                .setChannelId("default_channel_id")
                .setChannelName(R.string.app_name)
                .build()
            notificationProvider.setSmallIcon(R.drawable.ic_audia_notification)
            setMediaNotificationProvider(notificationProvider)
            Log.d(TAG, "Custom DefaultMediaNotificationProvider configured with ic_audia_notification & lockscreen visibility")
        } catch (e: Exception) {
            Log.w(TAG, "Could not set custom MediaNotificationProvider: ${e.message}")
        }
            
        player?.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != 0 && audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    AudioEffectManager.initialize(audioSessionId)
                    val extras = Bundle().apply {
                        putInt("AUDIO_SESSION_ID", audioSessionId)
                    }
                    mediaSession?.setSessionExtras(extras)
                    Log.d(TAG, "ExoPlayer onAudioSessionIdChanged: $audioSessionId")
                }
            }
        })
        val initialSessionId = player?.audioSessionId ?: 0
        if (initialSessionId != 0 && initialSessionId != C.AUDIO_SESSION_ID_UNSET) {
            AudioEffectManager.initialize(initialSessionId)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onDestroy() {
        retryHandler.removeCallbacksAndMessages(null)
        if (activeScheduledTaskId >= 0) markScheduledInterrupted()
        AudioEffectManager.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player?.release()
        player = null
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Stop service if task removed, but keep notification active if playing?
        // Usually handled by MediaSessionService logic.
        // For simplicity:
        if (player?.playWhenReady == false || player?.mediaItemCount == 0) {
            isAlarmSingleShotMode = false
            stopSelf()
        }
    }
}
