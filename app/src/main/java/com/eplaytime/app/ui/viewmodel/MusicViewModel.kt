package com.eplaytime.app.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.eplaytime.app.data.datastore.LoopPreset
import com.eplaytime.app.data.datastore.PlayTimeDataStore
import com.eplaytime.app.data.database.FavoriteSong
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.data.repository.Album
import com.eplaytime.app.data.repository.Artist
import com.eplaytime.app.data.repository.FavoritesRepository
import com.eplaytime.app.data.repository.MusicFolder
import com.eplaytime.app.data.repository.MusicRepository
import com.eplaytime.app.service.MusicService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MusicViewModel - Connects Music Service to UI
 * Manages playback state and communicates with MediaController
 * Supports: Songs, Albums, Artists, Folders, Favorites, Smart Resume
 */
@HiltViewModel
class MusicViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val favoritesRepository: FavoritesRepository,
    private val dataStore: PlayTimeDataStore
) : ViewModel() {

    companion object {
        private const val PREFS_NAME = "playtime_prefs"
        private const val PREFS_FAVORITES = "favorite_song_ids"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // MediaController for controlling playback
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var isControllerReady = false
    private var pendingPlayUri: String? = null
    private var playlistPrepared = false
    private var isRestoring = true // Lock: Prevent overwrite during restore
    private var sleepTimerJob: Job? = null
    private var volumeFadeJob: Job? = null
    private var stopAfterCurrentSongJob: Job? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // State flows for UI
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0L)
    val progress: StateFlow<Long> = _progress.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _songList = MutableStateFlow<List<Song>>(emptyList())
    val songList: StateFlow<List<Song>> = _songList.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    private val _folders = MutableStateFlow<List<MusicFolder>>(emptyList())
    val folders: StateFlow<List<MusicFolder>> = _folders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _favoriteSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteSongIds: StateFlow<Set<Long>> = _favoriteSongIds.asStateFlow()

    private val _recentPlayedSongs = MutableStateFlow<List<Song>>(emptyList())
    val recentPlayedSongs: StateFlow<List<Song>> = _recentPlayedSongs.asStateFlow()

    private val _mostPlayedSongs = MutableStateFlow<List<Song>>(emptyList())
    val mostPlayedSongs: StateFlow<List<Song>> = _mostPlayedSongs.asStateFlow()

    private var recentPlayedUrisCache: List<String> = emptyList()
    private var playCountsCache: Map<String, Int> = emptyMap()

    // Flag for Sync needed
    private val _hasNewSongs = MutableStateFlow(false)
    val hasNewSongs: StateFlow<Boolean> = _hasNewSongs.asStateFlow()

    // Database-backed favorites
    val allFavorites = favoritesRepository.getAllFavorites()

    // A-B Loop state
    private val _loopStart = MutableStateFlow<Long?>(null)
    val loopStart: StateFlow<Long?> = _loopStart.asStateFlow()

    private val _loopEnd = MutableStateFlow<Long?>(null)
    val loopEnd: StateFlow<Long?> = _loopEnd.asStateFlow()

    private val _loopEnabled = MutableStateFlow(false)
    val loopEnabled: StateFlow<Boolean> = _loopEnabled.asStateFlow()

    // Audio Session ID for Visualizer
    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _stopAfterCurrent = MutableStateFlow(false)
    val stopAfterCurrent: StateFlow<Boolean> = _stopAfterCurrent.asStateFlow()

    private val _fadeOnPlayPause = MutableStateFlow(true)
    val fadeOnPlayPause: StateFlow<Boolean> = _fadeOnPlayPause.asStateFlow()

    private val _sleepTimerRemainingMs = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingMs: StateFlow<Long?> = _sleepTimerRemainingMs.asStateFlow()

    private val _stopAfterCurrentSong = MutableStateFlow(false)
    val stopAfterCurrentSong: StateFlow<Boolean> = _stopAfterCurrentSong.asStateFlow()

    private val _loopPresetsForCurrentSong = MutableStateFlow<List<LoopPreset>>(emptyList())
    val loopPresetsForCurrentSong: StateFlow<List<LoopPreset>> = _loopPresetsForCurrentSong.asStateFlow()

    // Visualizer Style Carousel
    enum class VisualizerStyle { LIQUID_FILL, NEON_LINE, CLASSIC_BARS }
    private val _visualizerStyle = MutableStateFlow(VisualizerStyle.LIQUID_FILL)
    val visualizerStyle: StateFlow<VisualizerStyle> = _visualizerStyle.asStateFlow()

    fun nextVisualizerStyle() {
        val nextOrdinal = (_visualizerStyle.value.ordinal + 1) % VisualizerStyle.values().size
        _visualizerStyle.value = VisualizerStyle.values()[nextOrdinal]
    }

    // Audio Effects
    val bassBoostStrength: StateFlow<Int> = dataStore.bassBoostStrength
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 0)

    val eqBandsLevels: StateFlow<String> = dataStore.eqBandsLevels
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), "")

    fun setBassBoostStrength(strength: Int) {
        viewModelScope.launch {
            dataStore.setBassBoostStrength(strength)
            com.eplaytime.app.util.AudioEffectManager.setBassBoostStrength(strength.toShort())
        }
    }

    fun setEqBandLevel(band: Short, level: Short) {
        viewModelScope.launch {
            com.eplaytime.app.util.AudioEffectManager.setBandLevel(band, level)
            
            // Re-read current levels and store as String
            val numBands = com.eplaytime.app.util.AudioEffectManager.getNumberOfBands()
            val levelsList = mutableListOf<Short>()
            for (i in 0 until numBands) {
                levelsList.add(com.eplaytime.app.util.AudioEffectManager.getBandLevel(i.toShort()))
            }
            val levelsStr = levelsList.joinToString(",")
            dataStore.setEqBandsLevels(levelsStr)
        }
    }

    // User Name
    val userName: StateFlow<String?> = dataStore.userName
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)

    fun setUserName(name: String) {
        viewModelScope.launch {
            dataStore.setUserName(name)
        }
    }

    val hasSeenOnboarding: StateFlow<Boolean?> = dataStore.hasSeenOnboarding
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), null)

    fun setHasSeenOnboarding(seen: Boolean) {
        viewModelScope.launch {
            dataStore.setHasSeenOnboarding(seen)
        }
    }

    // Engagement: Update last opened time
    fun updateLastOpenedTime() {
        viewModelScope.launch {
             dataStore.setLastOpenedTime(System.currentTimeMillis())
        }
    }

    // Progress update job
    private var progressUpdateJob: Job? = null

    init {
        initializeMediaController()
        loadLocalSongs()
        loadFavorites()
        observePremiumPrefs()
        observeLoopPresets()
        observePlayHistory()
        
        // Check for new songs in background
        viewModelScope.launch {
            val changes = musicRepository.checkForChanges()
            if (changes) {
                _hasNewSongs.value = true
            }
        }

        // CRITICAL: Sync with service BEFORE restoring state from DataStore
        syncWithRunningService()
    }

    private fun observePremiumPrefs() {
        viewModelScope.launch {
            dataStore.stopAfterCurrent.collect { _stopAfterCurrent.value = it }
        }
        viewModelScope.launch {
            dataStore.fadeOnPlayPause.collect { _fadeOnPlayPause.value = it }
        }
        viewModelScope.launch {
            dataStore.playbackState.collect { state ->
                _playbackSpeed.value = state.lastPlaybackSpeed
            }
        }
        viewModelScope.launch {
            dataStore.bassBoostStrength.collect { strength ->
                com.eplaytime.app.util.AudioEffectManager.setBassBoostStrength(strength.toShort())
            }
        }
        viewModelScope.launch {
            dataStore.eqBandsLevels.collect { levelsStr ->
                if (levelsStr.isNotBlank()) {
                    val levels = levelsStr.split(",").mapNotNull { it.toShortOrNull() }
                    levels.forEachIndexed { index, level ->
                        com.eplaytime.app.util.AudioEffectManager.setBandLevel(index.toShort(), level)
                    }
                }
            }
        }
    }

    private fun observePlayHistory() {
        viewModelScope.launch {
            dataStore.recentPlayedUris.collect { uris ->
                recentPlayedUrisCache = uris
                recalculatePlayLists()
            }
        }
        viewModelScope.launch {
            dataStore.playCounts.collect { counts ->
                playCountsCache = counts
                recalculatePlayLists()
            }
        }
    }

    private fun recalculatePlayLists() {
        val songsByUri = _songList.value.associateBy { it.uri }
        _recentPlayedSongs.value = recentPlayedUrisCache
            .mapNotNull { songsByUri[it] }
            .distinctBy { it.uri }

        _mostPlayedSongs.value = playCountsCache.entries
            .sortedByDescending { it.value }
            .mapNotNull { (uri, _) -> songsByUri[uri] }
    }

    private fun markSongPlayed(uri: String) {
        viewModelScope.launch {
            dataStore.addRecentPlayedSong(uri)
            dataStore.incrementPlayCount(uri)
        }
    }

    private fun observeLoopPresets() {
        viewModelScope.launch {
            dataStore.loopPresets.collect { map ->
                val uri = _currentSong.value?.uri
                _loopPresetsForCurrentSong.value = if (uri == null) emptyList() else map[uri].orEmpty()
            }
        }
    }

    private fun refreshCurrentSongLoopPresets() {
        viewModelScope.launch {
            val map = dataStore.loopPresets.first()
            val uri = _currentSong.value?.uri
            _loopPresetsForCurrentSong.value = if (uri == null) emptyList() else map[uri].orEmpty()
        }
    }

    private fun detectAudioRoute(): String {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val hasHeadphones = devices.any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }
        return if (hasHeadphones) "headphones" else "speaker"
    }

    private fun routeAdjustedStartPosition(routePosition: Long, route: String): Long {
        if (routePosition <= 0L) return 0L
        return if (route == "speaker") {
            // Speaker profile: rewind a little context instead of exact resume.
            (routePosition - 15_000L).coerceAtLeast(0L)
        } else {
            routePosition
        }
    }

    /**
     * SINGLE SOURCE OF TRUTH FIX:
     * If music is already playing in MusicService, sync to it.
     * Do NOT reset playback or load from DataStore.
     * This prevents the MiniPlayer -> PlayerScreen reset bug.
     */
    private fun syncWithRunningService() {
        viewModelScope.launch {
            // Wait for controller to be ready
            while (mediaController == null) {
                delay(100)
            }
            
            mediaController?.let { controller ->
                val isServiceActive = controller.playbackState != Player.STATE_IDLE && 
                                     (controller.isPlaying || controller.mediaItemCount > 0)
                
                if (isServiceActive) {
                    // Service is active - sync UI to service state
                    val currentItem = controller.currentMediaItem
                    if (currentItem != null) {
                        val songUri = currentItem.localConfiguration?.uri.toString()
                        // Find song object from loaded list (wait if list is empty)
                        var retryCount = 0
                        while (_songList.value.isEmpty() && retryCount < 10) {
                            delay(200)
                            retryCount++
                        }

                        val song = _songList.value.find { it.uri == songUri }
                        if (song != null) {
                            _currentSong.value = song
                            _duration.value = controller.duration
                            _isPlaying.value = controller.isPlaying
                            _shuffleEnabled.value = controller.shuffleModeEnabled
                            _repeatMode.value = controller.repeatMode
                            _playbackSpeed.value = controller.playbackParameters.speed
                            
                            android.util.Log.d("MusicViewModel", "âœ“ Synced with running service: ${song.title}")
                            
                            // Start progress updates if playing
                            if (controller.isPlaying) {
                                startProgressUpdates()
                            }
                            
                            // Important: Mark playlist as prepared so loadLocalSongs doesn't overwrite it
                            playlistPrepared = true
                            isRestoring = false // Unlock
                            return@launch // Don't load from DataStore
                        } else {
                            // EDGE CASE: Alarm or External file playing (not in our DB)
                            // Create a temporary "Foreign" song so UI doesn't crash or reset
                            val mediaItem = controller.currentMediaItem
                            val tempSong = Song(
                                id = -1L,
                                title = mediaItem?.mediaMetadata?.title?.toString() ?: "External Audio",
                                artist = mediaItem?.mediaMetadata?.artist?.toString() ?: "System",
                                uri = songUri,
                                album = "Unknown",
                                duration = controller.duration,
                                albumArtUri = null,
                                dateAdded = 0L,
                                folderPath = ""
                            )
                            _currentSong.value = tempSong
                            _isPlaying.value = controller.isPlaying
                            _duration.value = controller.duration
                            _playbackSpeed.value = controller.playbackParameters.speed
                            
                            android.util.Log.d("MusicViewModel", "âš  Synced with FOREIGN audio (Alarm?): ${tempSong.title}")
                            
                            if (controller.isPlaying) startProgressUpdates()
                            playlistPrepared = true
                            isRestoring = false // Unlock
                            return@launch
                        }
                    }
                }
                
                // Service is idle or completely new session - restore from DataStore
                android.util.Log.d("MusicViewModel", "Service idle, restoring from DataStore")
                restorePlaybackState()
            }
        }
    }

    /**
     * Initialize MediaController to connect to MusicService
     */
    private fun initializeMediaController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicService::class.java)
        )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                mediaController = controllerFuture?.get()
                isControllerReady = true
                isControllerReady = true
                setupPlayerListener()
                preparePlaylistIfReady()
                requestAudioSessionId()
                pendingPlayUri?.let { uri ->
                    playSongInternal(uri)
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    /**
     * Setup listener to sync player state with UI
     */
    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)

                if (_stopAfterCurrent.value && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    mediaController?.let { controller ->
                        val previousIndex = (controller.currentMediaItemIndex - 1).coerceAtLeast(0)
                        controller.seekTo(previousIndex, 0L)
                        controller.pause()
                    }
                    _stopAfterCurrent.value = false
                    viewModelScope.launch { dataStore.setStopAfterCurrent(false) }
                    return
                }

                // Reset progress immediately on song change
                _progress.value = 0L
                _duration.value = mediaController?.duration?.takeIf { it > 0 } ?: 0L
                
                // Update current song object if needed
                mediaItem?.localConfiguration?.uri?.toString()?.let { uri ->
                    val song = _songList.value.find { it.uri == uri }
                    if (song != null) {
                        _currentSong.value = song
                        // Fallback duration from metadata if controller isn't ready
                        if (_duration.value == 0L && song.duration > 0) {
                            _duration.value = song.duration
                        }
                    } else if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                         // Only create temp song if we really have to
                    }

                    // Save to DataStore
                    viewModelScope.launch {
                        dataStore.saveLastSong(uri, _currentSong.value?.title ?: "Song")
                    }
                    markSongPlayed(uri)
                    refreshCurrentSongLoopPresets()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_READY) {
                    _duration.value = mediaController?.duration?.takeIf { it > 0 } ?: 0L
                    _isPlaying.value = mediaController?.isPlaying == true
                }
                if (playbackState == Player.STATE_ENDED) {
                    _isPlaying.value = false
                    stopProgressUpdates()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressUpdates()
                } else {
                    stopProgressUpdates()
                    // EAGER SAVE: Save state immediately on Pause (Notification/Earbuds)
                    viewModelScope.launch {
                        _currentSong.value?.let { song ->
                            dataStore.savePlaybackState(song.uri, mediaController?.currentPosition ?: 0L, _playbackSpeed.value, _shuffleEnabled.value, _repeatMode.value)
                        }
                    }
                }
            }

            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                 // EAGER SAVE: Save state on Seek/Skip
                 viewModelScope.launch {
                    _currentSong.value?.let { song ->
                        dataStore.savePlaybackState(song.uri, mediaController?.currentPosition ?: 0L, _playbackSpeed.value, _shuffleEnabled.value, _repeatMode.value)
                    }
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleEnabled.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }
        })
    }

    /**
     * Load songs from device storage
     */
    fun loadLocalSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // OPTIMIZATION: Move heavy loading to IO thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    // Get filter settings
                    val filterShortAudio = dataStore.filterShortAudio.first()
                    val filterCallRecordings = dataStore.filterCallRecordings.first()
                    
                    val songs = musicRepository.getAllSongs(filterShortAudio, filterCallRecordings)
                    _songList.value = songs
                    recalculatePlayLists()

                    // Load categories
                    _albums.value = musicRepository.getAllAlbums()
                    _artists.value = musicRepository.getAllArtists()
                    _folders.value = musicRepository.getAllFolders()
                }

                // CRITICAL FIX: Do NOT force prepare. Use false.
                // If we force true, we overwrite the restored state (position) with 0!
                preparePlaylistIfReady(force = false)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Rescan device for new music
     */
    fun rescanDevice() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // OPTIMIZATION: Move heavy loading to IO thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val filterShortAudio = dataStore.filterShortAudio.first()
                    val filterCallRecordings = dataStore.filterCallRecordings.first()
                    musicRepository.rescanDevice(filterShortAudio, filterCallRecordings)
                    val songs = musicRepository.songs.value
                    _songList.value = songs // Correct property
                    recalculatePlayLists()
                    
                    // Reload categories
                    _albums.value = musicRepository.getAllAlbums()
                    _artists.value = musicRepository.getAllArtists()
                    _folders.value = musicRepository.getAllFolders()
                }
                // Reset sync flag
                _hasNewSongs.value = false
                
                // Rescan should validly update the list but NOT reset playback if already running
                preparePlaylistIfReady(force = false)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Restore playback state from DataStore
     */
    /**
     * Restore playback state from DataStore
     * ROBUSTNESS FIX: Waits for song list to load before attempting restore
     */
    private fun restorePlaybackState() {
        viewModelScope.launch {
            try {
                // Check if user already acted or service is playing
                if ((mediaController?.mediaItemCount ?: 0) > 0) {
                     isRestoring = false
                     return@launch
                }

                val state = dataStore.playbackState.first()
                if (state.lastSongUri.isNotEmpty()) {
                    
                    // WAIT for songs to load (up to 5 seconds)
                    var retries = 0
                    while (_songList.value.isEmpty() && retries < 25) {
                         delay(200)
                         retries++
                    }

                    val song = _songList.value.find { it.uri == state.lastSongUri }
                    if (song != null) {
                        _currentSong.value = song
                        android.util.Log.d("MusicViewModel", "âœ“ Restored state: ${song.title} at ${state.lastPosition}ms")
                        
                        // Prepare but don't play
                        mediaController?.let { controller ->
                            val index = _songList.value.indexOfFirst { it.uri == state.lastSongUri }
                            if (index >= 0) {
                                val mediaItems = _songList.value.map { s ->
                                    MediaItem.Builder()
                                        .setUri(s.uri)
                                        .setMediaId(s.id.toString())
                                        .build()
                                }
                                controller.setMediaItems(mediaItems, index, state.lastPosition)
                                controller.prepare()
                                controller.setPlaybackSpeed(state.lastPlaybackSpeed)
                                controller.pause() // Ensure it starts PAUSED
                                
                                playlistPrepared = true
                            }
                        }
                    } else {
                        android.util.Log.w("MusicViewModel", "âŒ Could not restore song: ${state.lastSongUri} (Not found in ${_songList.value.size} songs)")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // UNLOCK: Allow other modifiers now
                isRestoring = false
                // If restore failed or list empty, ensure we at least prepare the list
                if (!playlistPrepared) {
                    preparePlaylistIfReady(false)
                }
            }
        }
    }

    /**
     * Build playlist and prepare if controller is ready
     */
    private fun preparePlaylistIfReady(force: Boolean = false) {
        val controller = mediaController ?: return
        val songs = _songList.value
        if (songs.isEmpty()) return

        // GOLD: If restoration is in progress, DO NOT TOUCH THE PLAYER
        if (isRestoring && !force) return

        // FIX: If controller already has items (synced from service), DO NOT RESET
        if (!force && controller.mediaItemCount > 0) {
            playlistPrepared = true
            return
        }
        
        // If we are restored, we might have set items already. Check if we need to force update.
        if (playlistPrepared && !force) return

        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setUri(song.uri)
                .setMediaId(song.id.toString())
                .build()
        }

        // Only set items if we are NOT currently playing or if forced (e.g., refresh)
        if (!controller.isPlaying || force) {
             // If we have a current song (restored), try to find its index to avoid resetting to 0
             val currentIndex = if (_currentSong.value != null) {
                 songs.indexOfFirst { it.uri == _currentSong.value?.uri }
             } else 0
             
             val startPos = if (_currentSong.value != null) _progress.value else 0L

            if (currentIndex >= 0) {
                 controller.setMediaItems(mediaItems, currentIndex, startPos)
            } else {
                 controller.setMediaItems(mediaItems)
            }
        }
        
        controller.prepare()
        playlistPrepared = true
    }

    /**
     * Play a specific song by URI
     */
    fun playSong(uri: String) {
        if (!isControllerReady || _songList.value.isEmpty()) {
            pendingPlayUri = uri
            return
        }
        playSongInternal(uri)
    }

    private fun playSongInternal(uri: String) {
        val controller = mediaController ?: return
        val songIndex = _songList.value.indexOfFirst { it.uri == uri }
        if (songIndex >= 0) {
            val route = detectAudioRoute()
            viewModelScope.launch {
                val routePosition = dataStore.getRoutePosition(uri, route)
                val startPosition = routeAdjustedStartPosition(routePosition, route)

                val mediaItems = _songList.value.map { song -> 
                    MediaItem.Builder()
                        .setUri(song.uri)
                        .setMediaId(song.id.toString())
                        .build()
                }

                controller.setMediaItems(mediaItems, songIndex, startPosition)
                controller.prepare()
                controller.setPlaybackSpeed(_playbackSpeed.value)

                if (_fadeOnPlayPause.value) {
                    controller.volume = 0f
                    controller.play()
                    fadePlayerVolume(0f, 1f, 600L)
                } else {
                    controller.play()
                }

                playlistPrepared = true
                pendingPlayUri = null
                _currentSong.value = _songList.value[songIndex]

                // Clear A-B loop when new song starts
                clearLoop()
                refreshCurrentSongLoopPresets()

                // LAYER 2: ELEPHANT MEMORY - Save immediately
                dataStore.savePlaybackState(
                    songUri = uri,
                    position = startPosition,
                    playbackSpeed = _playbackSpeed.value,
                    shuffleEnabled = _shuffleEnabled.value,
                    repeatMode = _repeatMode.value
                )
            }
        }
    }

    /**
     * Play song by index
     */
    fun playSongAtIndex(index: Int) {
        if (index >= 0 && index < _songList.value.size) {
            val uri = _songList.value[index].uri
            playSong(uri)
        }
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (_isPlaying.value) {
                if (_fadeOnPlayPause.value) {
                    viewModelScope.launch {
                        fadePlayerVolume(controller.volume, 0f, 350L)
                        controller.pause()
                        controller.volume = 1f
                    }
                } else {
                    controller.pause()
                }
            } else {
                if (_fadeOnPlayPause.value) {
                    controller.volume = 0f
                    controller.play()
                    viewModelScope.launch {
                        fadePlayerVolume(0f, 1f, 500L)
                    }
                } else {
                    controller.play()
                }
            }
            // LAYER 2: ELEPHANT MEMORY - Save state immediately
            viewModelScope.launch {
                _currentSong.value?.let { song ->
                    dataStore.savePlaybackState(
                        songUri = song.uri,
                        position = controller.currentPosition,
                        playbackSpeed = _playbackSpeed.value,
                        shuffleEnabled = _shuffleEnabled.value,
                        repeatMode = _repeatMode.value
                    )
                }
            }
        }
    }

    /**
     * Seek to position (in milliseconds)
     */
    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
        _progress.value = position
        // LAYER 2: ELEPHANT MEMORY - Save position immediately
        viewModelScope.launch {
             dataStore.saveLastPosition(position)
             _currentSong.value?.let { song ->
                 dataStore.saveRoutePosition(song.uri, detectAudioRoute(), position)
             }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val normalized = speed.coerceIn(0.5f, 2.0f)
        _playbackSpeed.value = normalized
        mediaController?.setPlaybackSpeed(normalized)
        viewModelScope.launch {
            _currentSong.value?.let { song ->
                dataStore.savePlaybackState(
                    songUri = song.uri,
                    position = _progress.value,
                    playbackSpeed = normalized,
                    shuffleEnabled = _shuffleEnabled.value,
                    repeatMode = _repeatMode.value
                )
            }
        }
    }

    fun cyclePlaybackSpeed() {
        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        val currentIndex = speeds.indexOfFirst { kotlin.math.abs(it - _playbackSpeed.value) < 0.01f }
        val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % speeds.size else 1
        setPlaybackSpeed(speeds[nextIndex])
    }

    fun toggleStopAfterCurrent() {
        val next = !_stopAfterCurrent.value
        _stopAfterCurrent.value = next
        viewModelScope.launch { dataStore.setStopAfterCurrent(next) }
    }

    fun setStopAfterCurrent(enabled: Boolean) {
        _stopAfterCurrent.value = enabled
        viewModelScope.launch { dataStore.setStopAfterCurrent(enabled) }
    }

    fun stopPlaybackNow(useFade: Boolean = true) {
        val controller = mediaController ?: return
        viewModelScope.launch {
            if (useFade && _fadeOnPlayPause.value && controller.isPlaying) {
                fadePlayerVolume(controller.volume, 0f, 350L)
                controller.pause()
                controller.volume = 1f
            } else {
                controller.pause()
            }
        }
    }

    fun toggleFadeOnPlayPause() {
        val next = !_fadeOnPlayPause.value
        _fadeOnPlayPause.value = next
        viewModelScope.launch { dataStore.setFadeOnPlayPause(next) }
    }

    fun startSleepTimer(minutes: Int, fadeOut: Boolean = true) {
        val totalMs = minutes.coerceAtLeast(1) * 60_000L
        sleepTimerJob?.cancel()
        sleepTimerJob = viewModelScope.launch {
            var remaining = totalMs
            while (remaining > 0) {
                _sleepTimerRemainingMs.value = remaining
                delay(1000L)
                remaining -= 1000L
            }
            _sleepTimerRemainingMs.value = 0L
            mediaController?.let { controller ->
                if (fadeOut) {
                    fadePlayerVolume(controller.volume, 0f, 4000L)
                    controller.pause()
                    controller.volume = 1f
                } else {
                    controller.pause()
                }
            }
            _sleepTimerRemainingMs.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingMs.value = null
    }

    fun toggleStopAfterCurrentSong() {
        val newState = !_stopAfterCurrentSong.value
        _stopAfterCurrentSong.value = newState
        if (newState) {
            // Watch for track change: when it changes, stop playback
            stopAfterCurrentSongJob?.cancel()
            stopAfterCurrentSongJob = viewModelScope.launch {
                val startingSong = _currentSong.value?.uri
                // Poll until the song changes (i.e., next song starts)
                while (true) {
                    delay(500L)
                    val nowSong = _currentSong.value?.uri
                    if (nowSong != startingSong) {
                        // New song just started â€” pause immediately
                        mediaController?.pause()
                        _stopAfterCurrentSong.value = false
                        break
                    }
                }
            }
        } else {
            stopAfterCurrentSongJob?.cancel()
            stopAfterCurrentSongJob = null
        }
    }

    private suspend fun fadePlayerVolume(from: Float, to: Float, durationMs: Long) {
        val controller = mediaController ?: return
        volumeFadeJob?.cancel()
        val fadeJob = viewModelScope.launch {
            val steps = 12
            val stepDelay = (durationMs / steps).coerceAtLeast(20L)
            repeat(steps) { step ->
                val t = (step + 1).toFloat() / steps.toFloat()
                controller.volume = from + (to - from) * t
                delay(stepDelay)
            }
            controller.volume = to
        }
        volumeFadeJob = fadeJob
        fadeJob.join()
    }

    /**
     * Play next song
     */
    fun playNext() {
        val controller = mediaController ?: return
        val wasPlaying = controller.isPlaying
        viewModelScope.launch {
            if (wasPlaying && _fadeOnPlayPause.value) {
                fadePlayerVolume(controller.volume, 0f, 220L)
                controller.seekToNext()
                controller.play()
                controller.volume = 0f
                fadePlayerVolume(0f, 1f, 260L)
            } else {
                controller.seekToNext()
            }
        }
    }

    /**
     * Play previous song
     */
    fun playPrevious() {
        val controller = mediaController ?: return
        val wasPlaying = controller.isPlaying
        viewModelScope.launch {
            if (wasPlaying && _fadeOnPlayPause.value) {
                fadePlayerVolume(controller.volume, 0f, 220L)
                controller.seekToPrevious()
                controller.play()
                controller.volume = 0f
                fadePlayerVolume(0f, 1f, 260L)
            } else {
                controller.seekToPrevious()
            }
        }
    }

    /**
     * Toggle shuffle mode
     */
    fun toggleShuffle() {
        mediaController?.let { controller ->
            val newValue = !controller.shuffleModeEnabled
            controller.shuffleModeEnabled = newValue
            _shuffleEnabled.value = newValue
        }
    }

    /**
     * Cycle repeat mode: OFF -> ONE -> ALL -> OFF
     */
    fun cycleRepeatMode() {
        mediaController?.let { controller ->
            val next = when (controller.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                else -> Player.REPEAT_MODE_OFF
            }
            controller.repeatMode = next
            _repeatMode.value = next
        }
    }

    // ====== A-B Loop Functions ======

    /**
     * Toggle A-B Loop
     * Click 1: Set Point A
     * Click 2: Set Point B (activate loop)
     * Click 3: Clear loop
     */
    fun toggleABLoop() {
        when {
            _loopStart.value == null -> {
                // Set Point A
                _loopStart.value = _progress.value
            }
            _loopEnd.value == null -> {
                // Set Point B and activate
                val currentPos = _progress.value
                if (currentPos > (_loopStart.value ?: 0L)) {
                    _loopEnd.value = currentPos
                    _loopEnabled.value = true
                }
            }
            else -> {
                // Clear loop
                clearLoop()
            }
        }
    }

    /**
     * Clear A-B loop
     */
    fun clearLoop() {
        _loopStart.value = null
        _loopEnd.value = null
        _loopEnabled.value = false
    }

    fun saveCurrentLoopPreset(name: String? = null) {
        val songUri = _currentSong.value?.uri ?: return
        val start = _loopStart.value ?: return
        val end = _loopEnd.value ?: return
        if (end <= start) return
        val presetName = name?.trim().takeUnless { it.isNullOrBlank() }
            ?: "Loop ${_loopPresetsForCurrentSong.value.size + 1}"
        viewModelScope.launch {
            dataStore.saveLoopPreset(songUri, LoopPreset(presetName, start, end))
            refreshCurrentSongLoopPresets()
        }
    }

    fun applyLoopPreset(name: String) {
        val preset = _loopPresetsForCurrentSong.value.firstOrNull { it.name == name } ?: return
        _loopStart.value = preset.startMs
        _loopEnd.value = preset.endMs
        _loopEnabled.value = true
        seekTo(preset.startMs)
    }

    fun deleteLoopPreset(name: String) {
        val songUri = _currentSong.value?.uri ?: return
        viewModelScope.launch {
            dataStore.removeLoopPreset(songUri, name)
            refreshCurrentSongLoopPresets()
        }
    }

    // ====== Category Functions ======

    /**
     * Get songs by folder path
     */
    fun getSongsByFolder(folderPath: String): List<Song> {
        return musicRepository.getSongsByFolder(folderPath)
    }

    /**
     * Get songs by album name
     */
    fun getSongsByAlbum(albumName: String): List<Song> {
        return musicRepository.getSongsByAlbum(albumName)
    }

    /**
     * Get songs by artist name
     */
    fun getSongsByArtist(artistName: String): List<Song> {
        return musicRepository.getSongsByArtist(artistName)
    }

    // ====== Favorites (Database-Backed) ======

    /**
     * Load favorites from database (called in init)
     * Now automatic via Flow
     */
    private fun loadFavorites() {
        // Favorites now come from database via allFavorites Flow
        // No need to manually load
    }

    /**
     * Check if a song is favorited by ID
     * Returns a Flow for reactive updates
     */
    fun isFavoritedById(songId: Long) = favoritesRepository.isFavoritedById(songId)

    /**
     * Check if a song is favorited by URI
     * Returns a Flow for reactive updates
     */
    fun isFavoritedByUri(uri: String) = favoritesRepository.isFavorited(uri)

    /**
     * Toggle favorite status of a song
     * Automatically persists to database
     */
    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val favorite = FavoriteSong(
                id = song.id,
                title = song.title,
                artist = song.artist,
                uri = song.uri,
                albumArtUri = song.albumArtUri
            )
            // Check if already favorited
            val isFav = favoritesRepository.isFavorited(song.uri).first()
            if (isFav) {
                // Remove from favorites
                favoritesRepository.removeFavoriteByUri(song.uri)
            } else {
                // Add to favorites
                favoritesRepository.addFavorite(favorite)
            }
        }
    }

    /**
     * Add a specific song to favorites
     */
    fun addFavorite(song: Song) {
        viewModelScope.launch {
            val favorite = FavoriteSong(
                id = song.id,
                title = song.title,
                artist = song.artist,
                uri = song.uri,
                albumArtUri = song.albumArtUri
            )
            favoritesRepository.addFavorite(favorite)
        }
    }

    /**
     * Remove a song from favorites by URI
     */
    fun removeFavorite(uri: String) {
        viewModelScope.launch {
            favoritesRepository.removeFavoriteByUri(uri)
        }
    }

    /**
     * Start updating progress every second
     */
    private fun startProgressUpdates() {
        progressUpdateJob?.cancel()
        progressUpdateJob = viewModelScope.launch {
            while (true) {
                mediaController?.let {
                    val current = it.currentPosition
                    val total = it.duration
                    
                    if (current >= 0) _progress.value = current
                    if (total > 0) _duration.value = total

                    // Check A-B loop
                    if (_loopEnabled.value) {
                        val end = _loopEnd.value
                        val start = _loopStart.value
                        if (end != null && start != null && current >= end) {
                            it.seekTo(start)
                        }
                    }
                }
                delay(100L) // Update every 100ms for 30fps smooth UI
            }
        }
    }

    /**
     * Stop progress updates
     */
    private fun stopProgressUpdates() {
        progressUpdateJob?.cancel()
    }

    // ====== Settings / Filter ======

    /**
     * Observe filter state
     */
    val isShortAudioHidden: StateFlow<Boolean> = dataStore.filterShortAudio
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Request Audio Session ID from Service
     */
    private fun requestAudioSessionId() {
        val controller = mediaController ?: return
        val command = androidx.media3.session.SessionCommand("ACTION_GET_AUDIO_SESSION_ID", android.os.Bundle.EMPTY)
        
        val future = controller.sendCustomCommand(command, android.os.Bundle.EMPTY)
        future.addListener(
            {
                try {
                    val result = future.get()
                    if (result.resultCode == androidx.media3.session.SessionResult.RESULT_SUCCESS) {
                        val sessionId = result.extras.getInt("AUDIO_SESSION_ID")
                        _audioSessionId.value = sessionId
                        android.util.Log.d("MusicViewModel", "Received Audio Session ID: $sessionId")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    /**
     * Toggle "Smart Filter" (Hide audio < 30s)
     */
    fun toggleShortAudioFilter(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setFilterShortAudio(enabled)
            // FAST UPDATE: Just reload from DB with new filter
            loadLocalSongs()
        }
    }

    /**
     * Observe call recording filter state
     */
    val isCallRecordingHidden: StateFlow<Boolean> = dataStore.filterCallRecordings
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Toggle "Hide Call Recordings"
     */
    fun toggleCallRecordingFilter(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setFilterCallRecordings(enabled)
            // FAST UPDATE: Just reload from DB with new filter
            loadLocalSongs()
        }
    }

    /**
     * Move item in queue (Reorder)
     */
    /**
     * Move item in queue (Reorder)
     */
    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        mediaController?.moveMediaItem(fromIndex, toIndex)
    }

    /**
     * Remove item from queue
     */
    fun removeFromQueue(index: Int) {
        mediaController?.removeMediaItem(index)
    }

    fun playNextFromQueue(songUri: String) {
        val controller = mediaController ?: return
        val fromIndex = (0 until controller.mediaItemCount).firstOrNull {
            controller.getMediaItemAt(it).localConfiguration?.uri.toString() == songUri
        } ?: return
        val targetIndex = (controller.currentMediaItemIndex + 1).coerceAtMost(controller.mediaItemCount - 1)
        controller.moveMediaItem(fromIndex, targetIndex)
    }

    fun playLaterFromQueue(songUri: String) {
        val controller = mediaController ?: return
        val fromIndex = (0 until controller.mediaItemCount).firstOrNull {
            controller.getMediaItemAt(it).localConfiguration?.uri.toString() == songUri
        } ?: return
        val targetIndex = (controller.mediaItemCount - 1).coerceAtLeast(0)
        controller.moveMediaItem(fromIndex, targetIndex)
    }

    fun removeUpcomingDuplicates() {
        val controller = mediaController ?: return
        val currentIndex = controller.currentMediaItemIndex
        val seen = mutableSetOf<String>()
        val toRemove = mutableListOf<Int>()
        for (i in (currentIndex + 1) until controller.mediaItemCount) {
            val uri = controller.getMediaItemAt(i).localConfiguration?.uri.toString()
            if (!seen.add(uri)) {
                toRemove.add(i)
            }
        }
        toRemove.sortedDescending().forEach { controller.removeMediaItem(it) }
    }

    fun autoCleanQueue(removeShortAudio: Boolean = true, removeCallRecordings: Boolean = true) {
        val controller = mediaController ?: return
        val currentIndex = controller.currentMediaItemIndex
        val toRemove = mutableListOf<Int>()
        for (i in (currentIndex + 1) until controller.mediaItemCount) {
            val uri = controller.getMediaItemAt(i).localConfiguration?.uri.toString()
            val song = _songList.value.firstOrNull { it.uri == uri } ?: continue
            val isShort = removeShortAudio && song.duration in 1..29_999
            val path = song.folderPath.lowercase()
            val isCallLike = removeCallRecordings && (
                path.contains("/call/") ||
                    path.contains("/record") ||
                    path.contains("/voice") ||
                    path.contains("whatsapp audio")
                )
            if (isShort || isCallLike) {
                toRemove.add(i)
            }
        }
        toRemove.sortedDescending().forEach { controller.removeMediaItem(it) }
    }

    /**
     * Release MediaController on ViewModel clear
     */
    override fun onCleared() {
        super.onCleared()
        stopProgressUpdates()
        cancelSleepTimer()
        volumeFadeJob?.cancel()
        // Save final state
        viewModelScope.launch {
            _currentSong.value?.let { song ->
                dataStore.savePlaybackState(
                    songUri = song.uri,
                    position = _progress.value,
                    playbackSpeed = _playbackSpeed.value,
                    shuffleEnabled = _shuffleEnabled.value,
                    repeatMode = _repeatMode.value
                )
            }
        }
        mediaController?.release() // Changed from MediaController.releaseFuture
    }
}
