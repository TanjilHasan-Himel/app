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
import com.eplaytime.app.data.database.PlayTimeDatabase
import com.eplaytime.app.data.model.ParsedLyrics
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.data.model.RadioStation
import com.eplaytime.app.data.model.SongWikiData
import com.eplaytime.app.data.network.LrcLibClient
import com.eplaytime.app.data.network.LyricsCandidate
import com.eplaytime.app.playback.playbackQueue
import com.eplaytime.app.data.model.Playlist
import com.eplaytime.app.data.model.PlaylistItem
import com.eplaytime.app.data.model.AppUpdateInfo
import com.eplaytime.app.data.network.AppUpdateManager
import com.eplaytime.app.data.repository.PlaylistRepository
import com.eplaytime.app.data.network.SongWikiClient
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val playlistRepository: PlaylistRepository,
    private val dataStore: PlayTimeDataStore,
    private val appUpdateManager: AppUpdateManager
) : ViewModel() {

    private var cachedMediaSource: List<Song>? = null
    private var cachedMediaItems: List<MediaItem> = emptyList()

    private fun mediaItemsFor(songs: List<Song>): List<MediaItem> {
        if (cachedMediaSource === songs) return cachedMediaItems
        cachedMediaSource = songs
        cachedMediaItems = songs.map { it.toMediaItem() }
        return cachedMediaItems
    }

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
    private var pendingPlayQueue: List<Song>? = null
    private var playlistPrepared = false
    private var isRestoring = true // Lock: Prevent overwrite during restore
    private var sleepTimerJob: Job? = null
    private var volumeFadeJob: Job? = null
    private var stopAfterCurrentSongJob: Job? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // State flows for UI
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _currentRadioStation = MutableStateFlow<RadioStation?>(null)
    val currentRadioStation: StateFlow<RadioStation?> = _currentRadioStation.asStateFlow()
    private val _radioNowPlaying = MutableStateFlow<String?>(null)
    val radioNowPlaying: StateFlow<String?> = _radioNowPlaying.asStateFlow()

    // Lyrics Engine
    private val lyricsDao = PlayTimeDatabase.getDatabase(context).lyricsDao()
    private val lrcLibClient = LrcLibClient(context, lyricsDao)

    private val _currentLyrics = MutableStateFlow<ParsedLyrics?>(null)
    val currentLyrics: StateFlow<ParsedLyrics?> = _currentLyrics.asStateFlow()

    private val _isLyricsLoading = MutableStateFlow(false)
    val isLyricsLoading: StateFlow<Boolean> = _isLyricsLoading.asStateFlow()

    private val _lyricsCandidates = MutableStateFlow<List<LyricsCandidate>>(emptyList())
    val lyricsCandidates: StateFlow<List<LyricsCandidate>> = _lyricsCandidates.asStateFlow()
    private var lyricsLoadJob: Job? = null
    private var lyricsLoadingUri: String? = null

    // Song Wiki Engine
    private val songWikiDao = PlayTimeDatabase.getDatabase(context).songWikiDao()
    private val songWikiClient = SongWikiClient(context, songWikiDao)

    private val _currentSongWiki = MutableStateFlow<SongWikiData?>(null)
    val currentSongWiki: StateFlow<SongWikiData?> = _currentSongWiki.asStateFlow()

    private val _isSongWikiLoading = MutableStateFlow(false)
    val isSongWikiLoading: StateFlow<Boolean> = _isSongWikiLoading.asStateFlow()

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

    private val _playbackQueue = MutableStateFlow<List<Song>>(emptyList())
    val playbackQueue: StateFlow<List<Song>> = _playbackQueue.asStateFlow()

    val upcomingQueue: StateFlow<List<Song>> = combine(_currentSong, _playbackQueue, _songList) { current, queue, allSongs ->
        if (current == null) return@combine emptyList<Song>()
        val sourceList = if (queue.isNotEmpty()) queue else allSongs
        val currentIndex = sourceList.indexOfFirst { it.uri == current.uri }
        if (currentIndex != -1 && currentIndex + 1 < sourceList.size) {
            sourceList.subList(currentIndex + 1, (currentIndex + 7).coerceAtMost(sourceList.size))
        } else if (sourceList.size > 1) {
            sourceList.filter { it.uri != current.uri }.take(6)
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    private var pendingLoopPreset: Pair<String, LoopPreset>? = null

    // Audio Session ID for Visualizer & Real-time FFT Frequency Analysis
    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()
    val visualizerHelper = com.eplaytime.app.util.VisualizerHelper()
    private var isVisualizerUiVisible = false

    // Global in-app HUD notification message
    private val _inAppNotification = MutableStateFlow<String?>(null)
    val inAppNotification: StateFlow<String?> = _inAppNotification.asStateFlow()
    private var inAppNotificationJob: Job? = null

    fun showInAppNotification(message: String) {
        inAppNotificationJob?.cancel()
        _inAppNotification.value = message
        inAppNotificationJob = viewModelScope.launch {
            delay(2400)
            if (_inAppNotification.value == message) {
                _inAppNotification.value = null
            }
        }
    }

    fun dismissInAppNotification() {
        inAppNotificationJob?.cancel()
        _inAppNotification.value = null
    }

    // App Update Engine (GitHub Releases & Fastly CDN unthrottled checks)
    private val _availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableUpdate: StateFlow<AppUpdateInfo?> = _availableUpdate.asStateFlow()

    private val _isCheckingForUpdates = MutableStateFlow(false)
    val isCheckingForUpdates: StateFlow<Boolean> = _isCheckingForUpdates.asStateFlow()

    fun checkForAppUpdates(force: Boolean = false, onComplete: ((AppUpdateInfo?) -> Unit)? = null) {
        viewModelScope.launch {
            _isCheckingForUpdates.value = true
            val result = appUpdateManager.checkForUpdates(force = force)
            _isCheckingForUpdates.value = false
            val update = result.getOrNull()
            if (update != null && update.isUpdateAvailable) {
                _availableUpdate.value = update
            }
            onComplete?.invoke(update)
        }
    }

    fun postponeUpdateReminder() {
        viewModelScope.launch {
            appUpdateManager.postponeReminder()
            _availableUpdate.value = null
        }
    }

    fun dismissUpdateDialog() {
        _availableUpdate.value = null
    }

    fun startUpdateDownload(updateInfo: AppUpdateInfo) {
        appUpdateManager.startDownload(updateInfo)
    }

    fun updateVisualizerState() {
        val sessionId = _audioSessionId.value
        val playing = _isPlaying.value
        if (playing && sessionId > 0 && isVisualizerUiVisible) {
            visualizerHelper.start(context, sessionId)
        } else {
            visualizerHelper.stop()
        }
    }

    fun setVisualizerUiVisible(visible: Boolean) {
        if (isVisualizerUiVisible == visible) return
        isVisualizerUiVisible = visible
        updateVisualizerState()
    }

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

    private val _isShortAudioHidden = MutableStateFlow(false)
    val isShortAudioHidden: StateFlow<Boolean> = _isShortAudioHidden.asStateFlow()

    private val _isCallRecordingHidden = MutableStateFlow(false)
    val isCallRecordingHidden: StateFlow<Boolean> = _isCallRecordingHidden.asStateFlow()

    // Echo Music Scan Filters & Storage Manager
    val minSongDurationSec: StateFlow<Int> = dataStore.minSongDurationSec
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    val folderFilterMode: StateFlow<String> = dataStore.folderFilterMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "EXCLUDE")

    val filteredFolders: StateFlow<Set<String>> = dataStore.filteredFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val lastScanFoundCount: StateFlow<Int> = dataStore.lastScanFoundCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lastScanExcludedCount: StateFlow<Int> = dataStore.lastScanExcludedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setMinSongDurationSec(sec: Int) {
        viewModelScope.launch {
            val safeSeconds = sec.coerceAtLeast(0)
            dataStore.setMinSongDurationSec(safeSeconds)
            dataStore.setFilterShortAudio(safeSeconds > 0)
            _isShortAudioHidden.value = safeSeconds > 0
            applyScanFiltersInMemory(minDurationOverride = safeSeconds)
        }
    }

    fun setFolderFilterMode(mode: String) {
        viewModelScope.launch {
            val safeMode = if (mode.equals("INCLUDE", true)) "INCLUDE" else "EXCLUDE"
            dataStore.setFolderFilterMode(safeMode)
            applyScanFiltersInMemory(folderModeOverride = safeMode)
        }
    }

    fun addFilteredFolder(path: String) {
        viewModelScope.launch {
            dataStore.addFilteredFolder(path)
            applyScanFiltersInMemory(foldersOverride = filteredFolders.value + path)
        }
    }

    fun removeFilteredFolder(path: String) {
        viewModelScope.launch {
            dataStore.removeFilteredFolder(path)
            applyScanFiltersInMemory(foldersOverride = filteredFolders.value - path)
        }
    }

    private fun applyScanFiltersInMemory(
        minDurationOverride: Int? = null,
        callFilterOverride: Boolean? = null,
        folderModeOverride: String? = null,
        foldersOverride: Set<String>? = null
    ) {
        viewModelScope.launch {
            val minSec = minDurationOverride ?: minSongDurationSec.value
            val callFilter = callFilterOverride ?: _isCallRecordingHidden.value
            val mode = folderModeOverride ?: folderFilterMode.value
            val folders = foldersOverride ?: filteredFolders.value

            val songs = withContext(Dispatchers.IO) {
                musicRepository.getAllSongs(
                    filterShortAudio = minSec > 0,
                    filterCallRecordings = callFilter,
                    minDurationSec = minSec,
                    folderFilterMode = mode,
                    filteredFolders = folders
                )
            }
            _songList.value = songs
            recalculatePlayLists()
        }
    }

    private val _loopPresetsForCurrentSong = MutableStateFlow<List<LoopPreset>>(emptyList())
    val loopPresetsForCurrentSong: StateFlow<List<LoopPreset>> = _loopPresetsForCurrentSong.asStateFlow()

    val allLoopPresets: StateFlow<Map<String, List<LoopPreset>>> = dataStore.loopPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

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

    val virtualizerStrength: StateFlow<Int> = dataStore.virtualizerStrength
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 0)

    val eqBandsLevels: StateFlow<String> = dataStore.eqBandsLevels
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), "")

    fun setBassBoostStrength(strength: Int) {
        viewModelScope.launch {
            dataStore.setBassBoostStrength(strength)
            com.eplaytime.app.util.AudioEffectManager.setBassBoostStrength(strength.toShort())
        }
    }

    fun setVirtualizerStrength(strength: Int) {
        viewModelScope.launch {
            dataStore.setVirtualizerStrength(strength)
            com.eplaytime.app.util.AudioEffectManager.setVirtualizerStrength(strength.toShort())
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

    fun applyEqPreset(presetName: String) {
        val preset = com.eplaytime.app.util.AudioEffectManager.PRESETS[presetName] ?: return
        viewModelScope.launch {
            preset.forEachIndexed { index, level ->
                com.eplaytime.app.util.AudioEffectManager.setBandLevel(index.toShort(), level)
            }
            val levelsStr = preset.joinToString(",")
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

        // Check for updates in background (throttled to 24h checks & 48h reminders)
        checkForAppUpdates(force = false)

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
            dataStore.virtualizerStrength.collect { strength ->
                com.eplaytime.app.util.AudioEffectManager.setVirtualizerStrength(strength.toShort())
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
        viewModelScope.launch {
            dataStore.filterShortAudio.collect { _isShortAudioHidden.value = it }
        }
        viewModelScope.launch {
            dataStore.filterCallRecordings.collect { _isCallRecordingHidden.value = it }
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
            .take(15)

        _mostPlayedSongs.value = playCountsCache.entries
            .sortedByDescending { it.value }
            .mapNotNull { (uri, _) -> songsByUri[uri] }
            .take(7)
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
                val extrasSessionId = mediaController?.sessionExtras?.getInt("AUDIO_SESSION_ID") ?: 0
                if (extrasSessionId > 0) {
                    _audioSessionId.value = extrasSessionId
                    com.eplaytime.app.util.AudioEffectManager.initialize(extrasSessionId)
                    updateVisualizerState()
                    android.util.Log.d("MusicViewModel", "Retrieved Audio Session ID from sessionExtras on connect: $extrasSessionId")
                }
                setupPlayerListener()
                refreshQueueFromController()
                preparePlaylistIfReady()
                requestAudioSessionId()
                pendingPlayUri?.let { uri ->
                    playSongInternal(uri, pendingPlayQueue ?: _songList.value)
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
                clearLoop()
                if (_audioSessionId.value <= 0) {
                    requestAudioSessionId()
                }
                
                // Update current song object with robust 3-way matching (URI, MediaID, or Playlist Index)
                val targetUri = mediaItem?.localConfiguration?.uri?.toString()
                    ?: mediaItem?.requestMetadata?.mediaUri?.toString()
                val targetId = mediaItem?.mediaId

                val isRadio = targetId?.startsWith("radio:") == true
                val song = _songList.value.find {
                    (targetUri != null && it.uri == targetUri) || 
                    (targetId != null && (it.id.toString() == targetId || it.uri == targetId))
                } ?: mediaController?.currentMediaItemIndex?.takeUnless { isRadio }?.let { idx ->
                    _songList.value.getOrNull(idx)
                }

                if (song != null) {
                    _currentRadioStation.value = null
                    _radioNowPlaying.value = null
                    _currentSong.value = song
                    loadLyricsForSong(song)
                    if (_duration.value == 0L && song.duration > 0) {
                        _duration.value = song.duration
                    }
                    viewModelScope.launch {
                        dataStore.saveLastSong(song.uri, song.title)
                    }
                    markSongPlayed(song.uri)
                    refreshCurrentSongLoopPresets()
                    pendingLoopPreset?.takeIf { it.first == song.uri }?.let { (_, preset) ->
                        _loopStart.value = preset.startMs
                        _loopEnd.value = preset.endMs
                        _loopEnabled.value = true
                        seekTo(preset.startMs)
                        pendingLoopPreset = null
                    }
                    android.util.Log.d("MusicViewModel", "Track transitioned to: ${song.title} (${song.artist}) via reason=$reason")
                } else if (isRadio) {
                    _currentSong.value = null
                    _currentLyrics.value = null
                    _lyricsCandidates.value = emptyList()
                }
            }

            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                if (_currentRadioStation.value != null) {
                    _radioNowPlaying.value = mediaMetadata.title?.toString()
                        ?.takeUnless { it == _currentRadioStation.value?.name }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_READY) {
                    _duration.value = mediaController?.duration?.takeIf { it > 0 } ?: 0L
                    _isPlaying.value = mediaController?.isPlaying == true
                    if (_audioSessionId.value <= 0) {
                        requestAudioSessionId()
                    }
                    updateVisualizerState()
                }
                if (playbackState == Player.STATE_ENDED) {
                    _isPlaying.value = false
                    stopProgressUpdates()
                    updateVisualizerState()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                updateVisualizerState()
                if (isPlaying) {
                    startProgressUpdates()
                    if (_audioSessionId.value <= 0) {
                        requestAudioSessionId()
                    }
                } else {
                    stopProgressUpdates()
                    // EAGER SAVE: Save state immediately on Pause (Notification/Earbuds)
                    val currentPos = mediaController?.currentPosition ?: 0L
                    val song = _currentSong.value
                    if (song != null) {
                        viewModelScope.launch {
                            dataStore.savePlaybackState(
                                song.uri,
                                currentPos,
                                _playbackSpeed.value,
                                _shuffleEnabled.value,
                                _repeatMode.value
                            )
                        }
                    }
                }
            }

            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                 // EAGER SAVE & PROGRESS SYNC: Save state and sync progress on Seek/Skip from Notification or Lockscreen
                 _progress.value = newPosition.positionMs
                 val song = _currentSong.value
                 val newPos = newPosition.positionMs
                 if (song != null) {
                     viewModelScope.launch {
                         dataStore.savePlaybackState(
                             song.uri,
                             newPos,
                             _playbackSpeed.value,
                             _shuffleEnabled.value,
                             _repeatMode.value
                         )
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
    fun loadLocalSongs(
        filterShortAudio: Boolean? = null,
        filterCallRecordings: Boolean? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val minSec = minSongDurationSec.value
                val callFilter = filterCallRecordings ?: _isCallRecordingHidden.value
                val mode = folderFilterMode.value
                val folders = filteredFolders.value
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val songs = musicRepository.getAllSongs(
                        filterShortAudio = minSec > 0,
                        filterCallRecordings = callFilter,
                        minDurationSec = minSec,
                        folderFilterMode = mode,
                        filteredFolders = folders
                    )
                    _songList.value = songs
                    recalculatePlayLists()

                    // Load categories
                    _albums.value = musicRepository.getAllAlbums()
                    _artists.value = musicRepository.getAllArtists()
                    _folders.value = musicRepository.getAllFolders()
                }

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
                val minSec = minSongDurationSec.value
                val filterCallRecordings = _isCallRecordingHidden.value
                val mode = folderFilterMode.value
                val folders = filteredFolders.value

                // OPTIMIZATION: Move heavy loading to IO thread
                val summary = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val scanResult = musicRepository.rescanDevice(
                        filterShortAudio = minSec > 0,
                        filterCallRecordings = filterCallRecordings,
                        minDurationSec = minSec,
                        folderFilterMode = mode,
                        filteredFolders = folders
                    )
                    dataStore.saveLastScanCounts(scanResult.totalFound, scanResult.excludedCount)

                    val songs = musicRepository.songs.value
                    _songList.value = songs
                    recalculatePlayLists()
                    
                    // Reload categories
                    _albums.value = musicRepository.getAllAlbums()
                    _artists.value = musicRepository.getAllArtists()
                    _folders.value = musicRepository.getAllFolders()

                    scanResult
                }
                // Reset sync flag
                _hasNewSongs.value = false
                
                // Rescan should validly update the list but NOT reset playback if already running
                preparePlaylistIfReady(force = false)
                showInAppNotification("Scanned ${summary.totalFound} songs (${summary.excludedCount} filtered)")
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
    private fun Song.toMediaItem(): MediaItem {
        val metadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(albumArtUri?.let { android.net.Uri.parse(it) })
            .build()
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(id.toString())
            .setMediaMetadata(metadata)
            .build()
    }

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
                        loadLyricsForSong(song)
                        android.util.Log.d("MusicViewModel", "âœ“ Restored state: ${song.title} at ${state.lastPosition}ms")
                        
                        // Prepare but don't play
                        mediaController?.let { controller ->
                            val index = _songList.value.indexOfFirst { it.uri == state.lastSongUri }
                            if (index >= 0) {
                                val mediaItems = mediaItemsFor(_songList.value)
                                controller.setMediaItems(mediaItems, index, state.lastPosition)
                                _playbackQueue.value = _songList.value





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

        val mediaItems = mediaItemsFor(songs)





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
            _playbackQueue.value = songs
        }
        
        controller.prepare()
        playlistPrepared = true
    }

    /**
     * Play a specific song by URI
     */
    fun playSong(uri: String) {
        playSongInQueue(_songList.value, uri)
    }

    fun playSongInQueue(songs: List<Song>, uri: String) {
        val queue = playbackQueue(songs)
        if (queue.none { it.uri == uri }) return
        if (!isControllerReady || _songList.value.isEmpty()) {
            pendingPlayUri = uri
            pendingPlayQueue = queue
            return
        }
        playSongInternal(uri, queue)
    }

    fun playRadioStation(station: RadioStation) {
        val controller = mediaController
        if (controller == null || !isControllerReady) {
            showInAppNotification("Playback service is still starting. Try again.")
            return
        }
        val extras = android.os.Bundle().apply {
            putString("radio_uuid", station.uuid)
            putString("fallback_url", station.fallbackUrl)
        }
        val metadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(station.name)
            .setArtist(listOf(station.country, station.language).filter(String::isNotBlank).joinToString(" • "))
            .setArtworkUri(station.favicon.takeIf(String::isNotBlank)?.let(android.net.Uri::parse))
            .setExtras(extras)
            .build()
        val item = MediaItem.Builder()
            .setMediaId("radio:${station.uuid}")
            .setUri(station.streamUrl)
            .apply { if (station.isHls) setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8) }
            .setMediaMetadata(metadata)
            .build()

        _currentRadioStation.value = station
        _radioNowPlaying.value = null
        _currentSong.value = null
        _currentLyrics.value = null
        _lyricsCandidates.value = emptyList()
        _playbackQueue.value = emptyList()
        clearLoop()
        controller.shuffleModeEnabled = false
        controller.repeatMode = Player.REPEAT_MODE_OFF
        controller.setMediaItem(item)
        controller.prepare()
        controller.play()
        playlistPrepared = false
    }

    private fun playSongInternal(uri: String, songs: List<Song>) {
        val controller = mediaController ?: return
        val songIndex = songs.indexOfFirst { it.uri == uri }
        if (songIndex >= 0) {
            val route = detectAudioRoute()
            viewModelScope.launch {
                val routePosition = dataStore.getRoutePosition(uri, route)
                val startPosition = routeAdjustedStartPosition(routePosition, route)

                val mediaItems = mediaItemsFor(songs)

                controller.setMediaItems(mediaItems, songIndex, startPosition)
                _playbackQueue.value = songs
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
                pendingPlayQueue = null
                _currentSong.value = songs[songIndex]
                loadLyricsForSong(songs[songIndex])

                // Clear A-B loop when new song starts
                clearLoop()
                refreshCurrentSongLoopPresets()
                requestAudioSessionId()

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

    fun playQueueSong(uri: String) {
        val controller = mediaController ?: return
        val index = (0 until controller.mediaItemCount).firstOrNull {
            controller.getMediaItemAt(it).localConfiguration?.uri.toString() == uri
        } ?: return
        controller.seekToDefaultPosition(index)
        controller.play()
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

    /**
     * Load synchronized lyrics for the specified song from local cache, .lrc file, or LRCLIB
     */
    fun loadLyricsForSong(song: Song?, forceRefresh: Boolean = false) {
        if (!forceRefresh && song != null && lyricsLoadingUri == song.uri && lyricsLoadJob?.isActive == true) return
        lyricsLoadJob?.cancel()
        if (song == null) {
            _currentLyrics.value = null
            _lyricsCandidates.value = emptyList()
            _isLyricsLoading.value = false
            return
        }
        lyricsLoadingUri = song.uri
        lyricsLoadJob = viewModelScope.launch {
            _isLyricsLoading.value = true
            try {
                if (forceRefresh) lrcLibClient.clearCachedLyrics(song)
                val lookup = lrcLibClient.getLyricsForSong(song, ignoreCache = forceRefresh)
                if (_currentSong.value?.uri == song.uri) {
                    _currentLyrics.value = lookup.lyrics
                    _lyricsCandidates.value = lookup.candidates
                    lookup.retryAfterSeconds?.let {
                        showInAppNotification("Lyrics provider is busy. Try again in ${it}s")
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (_currentSong.value?.uri == song.uri) {
                    _currentLyrics.value = null
                    _lyricsCandidates.value = emptyList()
                }
            } finally {
                if (_currentSong.value?.uri == song.uri) _isLyricsLoading.value = false
                if (lyricsLoadingUri == song.uri) lyricsLoadingUri = null
            }
        }
    }

    fun retryLyricsForSong(song: Song?) = loadLyricsForSong(song, forceRefresh = true)

    fun selectLyricsCandidate(song: Song, candidate: LyricsCandidate) {
        lyricsLoadJob?.cancel()
        lyricsLoadJob = viewModelScope.launch {
            _isLyricsLoading.value = true
            try {
                _currentLyrics.value = lrcLibClient.selectCandidate(song, candidate)
                _lyricsCandidates.value = emptyList()
            } finally {
                _isLyricsLoading.value = false
            }
        }
    }

    fun dismissLyricsCandidates() {
        _lyricsCandidates.value = emptyList()
    }

    /**
     * Load Song Wiki background story and creative credits (Wikipedia + MusicBrainz)
     */
    fun loadSongWiki(song: Song?) {
        if (song == null) {
            _currentSongWiki.value = null
            return
        }
        viewModelScope.launch {
            _isSongWikiLoading.value = true
            try {
                val wiki = songWikiClient.getWikiForSong(song)
                _currentSongWiki.value = wiki
            } catch (e: Exception) {
                _currentSongWiki.value = null
            } finally {
                _isSongWikiLoading.value = false
            }
        }
    }

    /**
     * Update metadata tags and cover artwork for a track in Song Garage
     */
    fun updateSongTags(songId: Long, newTitle: String, newArtist: String, newAlbum: String, newArtUri: String? = null) {
        viewModelScope.launch {
            val updatedList = _songList.value.map { song ->
                if (song.id == songId) {
                    song.copy(
                        title = newTitle.ifBlank { song.title },
                        artist = newArtist.ifBlank { song.artist },
                        album = newAlbum.ifBlank { song.album },
                        albumArtUri = newArtUri ?: song.albumArtUri
                    )
                } else song
            }
            _songList.value = updatedList

            if (_currentSong.value?.id == songId) {
                _currentSong.value = updatedList.firstOrNull { it.id == songId }
            }
        }
    }

    fun updateCurrentSongAlbumArt(newArtUri: String) {
        val song = _currentSong.value ?: return
        updateSongTags(
            songId = song.id,
            newTitle = song.title,
            newArtist = song.artist,
            newAlbum = song.album,
            newArtUri = newArtUri
        )
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
                // Set Point B and activate seamlessly (auto-ordering start and end)
                val currentPos = _progress.value
                val start = _loopStart.value ?: 0L
                if (kotlin.math.abs(currentPos - start) >= 500L) {
                    val pA = kotlin.math.min(start, currentPos)
                    val pB = kotlin.math.max(start, currentPos)
                    _loopStart.value = pA
                    _loopEnd.value = pB
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

    fun playLoopPreset(songUri: String, preset: LoopPreset) {
        val currentUri = _currentSong.value?.uri
        if (currentUri != songUri) {
            pendingLoopPreset = songUri to preset
            playSong(songUri)
            return
        }
        _loopStart.value = preset.startMs
        _loopEnd.value = preset.endMs
        _loopEnabled.value = true
        seekTo(preset.startMs)
    }

    fun deleteLoopPreset(name: String, songUri: String? = null) {
        val targetUri = songUri ?: _currentSong.value?.uri ?: return
        viewModelScope.launch {
            dataStore.removeLoopPreset(targetUri, name)
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
            var lastUiUpdateMs = 0L
            while (isActive) {
                mediaController?.let {
                    val current = it.currentPosition
                    val total = it.duration

                    val now = android.os.SystemClock.uptimeMillis()
                    if (now - lastUiUpdateMs >= 250L) {
                        if (current >= 0) _progress.value = current
                        if (total > 0) _duration.value = total
                        lastUiUpdateMs = now
                    }

                    // Check A-B loop
                    if (_loopEnabled.value) {
                        val end = _loopEnd.value
                        val start = _loopStart.value
                        if (end != null && start != null && current >= end) {
                            it.seekTo(start)
                        }
                    }
                }
                delay(if (_loopEnabled.value) 20L else 250L)
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
     * Request Audio Session ID from Service
     */
    fun requestAudioSessionId() {
        val controller = mediaController ?: return

        // 1. Proactively read from session extras if present
        val extrasId = controller.sessionExtras.getInt("AUDIO_SESSION_ID")
        if (extrasId > 0) {
            _audioSessionId.value = extrasId
            com.eplaytime.app.util.AudioEffectManager.initialize(extrasId)
            updateVisualizerState()
            android.util.Log.d("MusicViewModel", "Audio Session ID resolved from sessionExtras: $extrasId")
        }

        // 2. Query service directly via SessionCommand
        val command = androidx.media3.session.SessionCommand("ACTION_GET_AUDIO_SESSION_ID", android.os.Bundle.EMPTY)
        
        val future = controller.sendCustomCommand(command, android.os.Bundle.EMPTY)
        future.addListener(
            {
                try {
                    val result = future.get()
                    if (result.resultCode == androidx.media3.session.SessionResult.RESULT_SUCCESS) {
                        val sessionId = result.extras.getInt("AUDIO_SESSION_ID")
                        if (sessionId > 0) {
                            _audioSessionId.value = sessionId
                            com.eplaytime.app.util.AudioEffectManager.initialize(sessionId)
                            updateVisualizerState()
                            android.util.Log.d("MusicViewModel", "Received Audio Session ID from command: $sessionId")
                        }
                    } else {
                        android.util.Log.w("MusicViewModel", "requestAudioSessionId command returned resultCode: ${result.resultCode}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MusicViewModel", "Error requesting Audio Session ID", e)
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    /**
     * Toggle "Smart Filter" (Hide audio < 30s)
     */
    fun toggleShortAudioFilter(enabled: Boolean) {
        _isShortAudioHidden.value = enabled
        viewModelScope.launch {
            dataStore.setFilterShortAudio(enabled)
        }
        loadLocalSongs(filterShortAudio = enabled)
    }

    /**
     * Observe call recording filter state
     */


    /**
     * Toggle "Hide Call Recordings"
     */
    fun toggleCallRecordingFilter(enabled: Boolean) {
        _isCallRecordingHidden.value = enabled
        viewModelScope.launch {
            dataStore.setFilterCallRecordings(enabled)
        }
        applyScanFiltersInMemory(callFilterOverride = enabled)
    }

    /**
     * Move item in queue (Reorder)
     */
    /**
     * Move item in queue (Reorder)
     */
    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        mediaController?.moveMediaItem(fromIndex, toIndex)
        refreshQueueFromController()
    }

    /**
     * Remove item from queue
     */
    fun removeFromQueue(index: Int) {
        mediaController?.removeMediaItem(index)
        refreshQueueFromController()
    }

    fun playNextFromQueue(songUri: String) {
        val controller = mediaController ?: return
        val fromIndex = (0 until controller.mediaItemCount).firstOrNull {
            controller.getMediaItemAt(it).localConfiguration?.uri.toString() == songUri
        } ?: return
        val targetIndex = (controller.currentMediaItemIndex + 1).coerceAtMost(controller.mediaItemCount - 1)
        controller.moveMediaItem(fromIndex, targetIndex)
        refreshQueueFromController()
    }

    fun playLaterFromQueue(songUri: String) {
        val controller = mediaController ?: return
        val fromIndex = (0 until controller.mediaItemCount).firstOrNull {
            controller.getMediaItemAt(it).localConfiguration?.uri.toString() == songUri
        } ?: return
        val targetIndex = (controller.mediaItemCount - 1).coerceAtLeast(0)
        controller.moveMediaItem(fromIndex, targetIndex)
        refreshQueueFromController()
    }

    private fun refreshQueueFromController() {
        val controller = mediaController ?: return
        val songsByUri = _songList.value.associateBy { it.uri }
        _playbackQueue.value = (0 until controller.mediaItemCount).mapNotNull { index ->
            controller.getMediaItemAt(index).localConfiguration?.uri?.toString()?.let(songsByUri::get)
        }
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

    // ==========================================
    // Phase L2: Persistent Playlists & M3U Engine
    // ==========================================

    val allPlaylists: StateFlow<List<Playlist>> =
        playlistRepository.allPlaylists.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun createPlaylist(name: String, description: String = "", onCreated: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = playlistRepository.createPlaylist(name, description)
            showInAppNotification("Created playlist '$name'")
            onCreated?.invoke(id)
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            playlistRepository.renamePlaylist(playlistId, newName)
            showInAppNotification("Playlist renamed")
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
            showInAppNotification("Playlist deleted")
        }
    }

    fun addSongsToPlaylist(playlistId: Long, songs: List<Song>, onAdded: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val count = playlistRepository.addSongsToPlaylist(playlistId, songs)
            showInAppNotification("Added $count songs to playlist")
            onAdded?.invoke(count)
        }
    }

    fun removePlaylistItem(playlistId: Long, itemId: Long) {
        viewModelScope.launch {
            playlistRepository.removePlaylistItem(playlistId, itemId)
            showInAppNotification("Song removed from playlist")
        }
    }

    fun reorderPlaylist(playlistId: Long, items: List<PlaylistItem>) {
        viewModelScope.launch {
            playlistRepository.reorderPlaylist(playlistId, items)
        }
    }

    fun getPlaylistItems(playlistId: Long): kotlinx.coroutines.flow.Flow<List<PlaylistItem>> {
        return playlistRepository.getPlaylistItems(playlistId)
    }

    fun playPlaylist(
        playlistId: Long,
        items: List<PlaylistItem>,
        startSongUri: String? = null,
        shuffle: Boolean = false
    ) {
        if (items.isEmpty()) {
            showInAppNotification("Playlist is empty")
            return
        }
        val libraryMap = _songList.value.associateBy { it.uri }
        val songs = items.map { it.toSong(libraryMap) }
        val targetQueue = if (shuffle) songs.shuffled() else songs
        val targetUri = startSongUri ?: targetQueue.firstOrNull()?.uri ?: return
        playSongInQueue(targetQueue, targetUri)
    }

    private fun PlaylistItem.toSong(libraryMap: Map<String, Song>): Song {
        return libraryMap[songUri] ?: Song(
            id = if (songId > 0) songId else (songUri.hashCode().toLong() and 0x7FFFFFFF),
            title = title,
            artist = artist,
            album = album,
            uri = songUri,
            duration = duration,
            albumArtUri = albumArtUri
        )
    }

    fun exportPlaylistToM3u(
        playlistId: Long,
        outputStream: java.io.OutputStream,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = playlistRepository.exportPlaylistToM3u(playlistId)
                outputStream.use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                    stream.flush()
                }
                withContext(Dispatchers.Main) {
                    showInAppNotification("Playlist exported successfully")
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val msg = e.localizedMessage ?: "Failed to export playlist"
                    showInAppNotification(msg)
                    onError(msg)
                }
            }
        }
    }

    fun importPlaylistFromM3u(
        name: String,
        inputStream: java.io.InputStream,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val resolvedName = name.ifBlank { "Imported Playlist" }
                val playlistId = playlistRepository.importM3u(
                    name = resolvedName,
                    m3uContent = content,
                    knownSongs = _songList.value
                )
                withContext(Dispatchers.Main) {
                    showInAppNotification("Imported playlist '$resolvedName'")
                    onSuccess(resolvedName)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val msg = e.localizedMessage ?: "Failed to import playlist"
                    showInAppNotification(msg)
                    onError(msg)
                }
            }
        }
    }

    /**
     * Release MediaController on ViewModel clear
     */
    override fun onCleared() {
        super.onCleared()
        visualizerHelper.stop()
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
