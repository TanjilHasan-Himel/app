package com.eplaytime.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.eplaytime.app.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.data.repository.Album
import com.eplaytime.app.data.repository.Artist
import com.eplaytime.app.data.repository.MusicFolder
import com.eplaytime.app.ui.components.MiniPlayer
import com.eplaytime.app.ui.components.SongList
import com.eplaytime.app.ui.screens.OnboardingScreen
import com.eplaytime.app.ui.theme.*
import com.eplaytime.app.ui.viewmodel.MusicViewModel
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * HomeScreen - Flagship UI with Scrollable Tabs and Glass Header
 * Features:
 * - True OLED Black background
 * - Outfit font throughout
 * - ScrollableTabRow for categories
 * - Glass header with greeting
 * - Scan/Refresh button
 */
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi

private val BrandGlowColor = Color(0xFFFF5A5F)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onNavigateToScheduler: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToAbout: () -> Unit = {},
    onNavigateToQueue: () -> Unit = {},
    onNavigateToFolder: (String) -> Unit = {}
) {
    val context = LocalContext.current
    
    // Permission handling
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            viewModel.rescanDevice()
        }
    }
    
    // Only load songs when we already have permission; do not auto-request permission.
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.loadLocalSongs()
        }
    }
    
    // If no permission, show permission request UI
    if (!hasPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BlackBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(72.dp)
                )
                Text(
                    text = "Permission Required",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Audia Player needs access to your music files to play them.",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { permissionLauncher.launch(audioPermission) },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftGold)
                ) {
                    Text(
                        text = "Grant Permission",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                }
            }
        }
        return
    }
    
    val songList by viewModel.songList.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val allFavorites by viewModel.allFavorites.collectAsState(initial = emptyList())
    val recentPlayedSongs by viewModel.recentPlayedSongs.collectAsState()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsState()
    val isShortAudioHidden by viewModel.isShortAudioHidden.collectAsState()
    val isCallRecordingHidden by viewModel.isCallRecordingHidden.collectAsState()
    val hasNewSongs by viewModel.hasNewSongs.collectAsState()
    val userName by viewModel.userName.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsState()

    var searchQuery by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-focus logic
    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }

    // Handle Back Press to close search
    BackHandler(enabled = isSearchExpanded) {
        isSearchExpanded = false
        searchQuery = TextFieldValue("")
    }
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }

    val tabs = listOf("Tracks", "Folders", "Albums", "Artists", "Favorites")

    val recentAddedSongs = remember(songList) {
        val visibleSongs = songList.filterNot { song -> isSongFromHiddenFolder(song) }
        val withDate = visibleSongs.filter { it.dateAdded > 0L }
        if (withDate.isNotEmpty()) {
            withDate.sortedByDescending { it.dateAdded }
        } else {
            visibleSongs
        }
    }

    val filteredSongs = remember(
        songList,
        searchQuery.text,
        selectedTabIndex,
        allFavorites
    ) {
        val query = searchQuery.text.trim().lowercase(Locale.US)
        val baseList = when (selectedTabIndex) {
            4 -> songList.filter { song -> allFavorites.any { it.uri == song.uri } } // Favorites from DB
            else -> songList
        }
        if (query.isEmpty()) baseList
        else baseList.filter { song ->
            song.title.lowercase(Locale.US).contains(query) ||
                song.artist.lowercase(Locale.US).contains(query) ||
                song.album.lowercase(Locale.US).contains(query)
        }
    }

    // --- Cinematic Cold Start Logic ---
    var isColdStart by rememberSaveable { mutableStateOf(true) }
    
    // Feature: Swipe Down to Restore Hero Plate
    // Logic: If list is at top and user swipes down, bring back the Cold Start plate
    val nestedScrollConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): Offset {
                // If we have available 'y' scroll (meaning list is at top/bottom boundary)
                // and it's positive (pulling down)
                if (available.y > 0 && source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.Drag) {
                    // Threshold to trigger (e.g. 100px)
                    if (available.y > 50f) { // Sensitive trigger
                         isColdStart = true
                         return available // Consume it so we don't trigger other refreshers if any
                    }
                }
                return super.onPostScroll(consumed, available, source)
            }
        }
    }

    val transition = updateTransition(targetState = isColdStart, label = "cold_start")

    // Animation A: Hero Plate Slide UP (Exit)
    val plateOffsetY by transition.animateDp(
        transitionSpec = { tween(durationMillis = 800, easing = LinearOutSlowInEasing) },
        label = "plate_exit"
    ) { state ->
        if (state) 0.dp else (-1000).dp // Move off screen
    }

    // Animation B: Header Icons Slide DOWN (Entry)
    val iconEnterSpec = tween<Dp>(durationMillis = 600, easing = FastOutSlowInEasing)
    
    val searchIconOffset by transition.animateDp(
        transitionSpec = { if (targetState) snap() else iconEnterSpec },
        label = "icon_search"
    ) { state -> if (state) (-100).dp else 0.dp }

    val refreshIconOffset by transition.animateDp(
        transitionSpec = { if (targetState) snap() else tween(durationMillis = 600, delayMillis = 100, easing = FastOutSlowInEasing) },
        label = "icon_refresh"
    ) { state -> if (state) (-100).dp else 0.dp }

    val otherIconsOffset by transition.animateDp(
        transitionSpec = { if (targetState) snap() else tween(durationMillis = 600, delayMillis = 200, easing = FastOutSlowInEasing) },
        label = "icon_others"
    ) { state -> if (state) (-100).dp else 0.dp }

    // Animation C: Main Content Slide UP (Entry)
    val contentOffsetY by transition.animateDp(
        transitionSpec = { tween(durationMillis = 800, delayMillis = 100, easing = EaseOutCubic) },
        label = "content_enter"
    ) { state ->
        if (state) 150.dp else 0.dp
    }
    
    val contentAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 800, delayMillis = 100) },
        label = "content_alpha"
    ) { state ->
        if (state) 0f else 1f
    }
    
    // Dynamic Greeting Logic (Lifted for Hero Plate access)
    val greeting = remember(userName) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeGreeting = when (hour) {
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
        if (userName != null) "$timeGreeting, $userName!" else "$timeGreeting!"
    }


    // --- Cinematic Cold Start Logic ---



    // Animation B: Header Icons Slide DOWN (Entry)


    // Animation C: Main Content Slide UP (Entry)



    if (hasSeenOnboarding == null) {
        // Wait for DataStore to load
        return
    }

    if (hasSeenOnboarding == false) {
        OnboardingScreen(
            onComplete = { name ->
                viewModel.setUserName(name)
                viewModel.setHasSeenOnboarding(true)
            }
        )
        return
    }

    Scaffold(
        containerColor = BlackBackground,
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            // MiniPlayer with safe area - OUTSIDE content to prevent recreation on tab change
            Column {
                if (isPlaying || currentSong != null) {
                    currentSong?.let { song ->
                        MiniPlayer(
                            song = song,
                            isPlaying = isPlaying,
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            onSongClick = onNavigateToPlayer,
                            onNextClick = { viewModel.playNext() },
                            onPreviousClick = { viewModel.playPrevious() },
                            viewModel = viewModel
                        )
                    }
                }
                // Navigation bar spacer
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                        .background(BlackBackground)
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .graphicsLayer { 
                    translationY = contentOffsetY.toPx() 
                    alpha = contentAlpha
                }
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            // Collapsible Header & Search
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                // COLLAPSED STATE: Header with Icons
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isSearchExpanded,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    GlassHeader(
                        onSchedulerClick = onNavigateToScheduler,
                        onAboutClick = onNavigateToAbout,
                        onSettingsClick = { showSettingsDialog = true },
                        onRefreshClick = { viewModel.rescanDevice() },
                        isLoading = isLoading,
                        hasNewSongs = hasNewSongs,
                        userName = userName,
                        onSearchClick = { isSearchExpanded = true },
                        searchIconOffset = searchIconOffset,
                        refreshIconOffset = refreshIconOffset,
                        otherIconsOffset = otherIconsOffset
                    )
                }

                // EXPANDED STATE: Full Search Bar
                androidx.compose.animation.AnimatedVisibility(
                    visible = isSearchExpanded,
                    enter = fadeIn() + slideInHorizontally(),
                    exit = fadeOut() + slideOutHorizontally()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            isSearchExpanded = false
                            searchQuery = TextFieldValue("")
                            // viewModel.updateSearchQuery("") // If needed, but local state drives filter currently or we need to check usage
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = SoftGold
                            )
                        }
                        
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            placeholder = {
                                Text(
                                    "Search songs, artists...",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Light,
                                    color = TextSecondary.copy(alpha = 0.7f)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.text.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = TextFieldValue("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = TextSecondary
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SoftGold,
                                unfocusedBorderColor = GlassBorder,
                                cursorColor = SoftGold,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = BlackSurface,
                                unfocusedContainerColor = BlackSurface
                            )
                        )
                    }
                }
            }

            // ... (Search Bar code) ...

            // Settings Dialog
            if (showSettingsDialog) {
                com.eplaytime.app.ui.components.SettingsDialog(
                    showDialog = showSettingsDialog,
                    onDismiss = { showSettingsDialog = false },
                    currentFilterState = isShortAudioHidden,
                    onToggleFilter = { viewModel.toggleShortAudioFilter(it) },
                    currentCallFilterState = isCallRecordingHidden,
                    onToggleCallFilter = { viewModel.toggleCallRecordingFilter(it) },
                    onTermsClick = onNavigateToAbout, // Navigate to About (where Terms are located)
                    onAboutClick = onNavigateToAbout
                )
            }
            
            
            // Onboarding completely replaced WelcomeDialog.



            // Scrollable Tab Row (CRITICAL: Must scroll horizontally)
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = SoftGold,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedTabIndex])
                                .height(3.dp)
                                .background(SoftGold, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        )
                    }
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontFamily = OutfitFontFamily,
                                fontWeight = if (selectedTabIndex == index) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp,
                                color = if (selectedTabIndex == index) SoftGold else TextSecondary,
                                maxLines = 1
                            )
                        }
                    )
                }
            }
            
            // Hoist list states to preserve scroll position
            val tracksListState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
                androidx.compose.foundation.lazy.LazyListState()
            }
            val foldersListState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
                androidx.compose.foundation.lazy.LazyListState()
            }
            val albumsListState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
                androidx.compose.foundation.lazy.LazyListState()
            }
            val artistsListState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
                androidx.compose.foundation.lazy.LazyListState()
            }
            val favoritesListState = rememberSaveable(saver = androidx.compose.foundation.lazy.LazyListState.Saver) {
                androidx.compose.foundation.lazy.LazyListState()
            }

            // Content based on selected tab - using AnimatedContent instead of Crossfade
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = selectedTabIndex,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) with
                        fadeOut(animationSpec = tween(200))
                    },
                    label = "tab_content"
                ) { tabIndex ->
                    when (tabIndex) {
                        0 -> {
                            // Tracks
                            if (isLoading) LoadingState()
                            else if (songList.isEmpty()) EmptyState()
                            else if (searchQuery.text.isNotBlank()) {
                                SongList(
                                    songs = filteredSongs,
                                    currentSongId = currentSong?.id,
                                    onSongClick = { song -> viewModel.playSong(song.uri) },
                                    state = tracksListState
                                )
                            } else {
                                SpotifyTracksFeed(
                                    allSongs = songList,
                                    recentAddedSongs = recentAddedSongs,
                                    recentlyPlayedSongs = recentPlayedSongs,
                                    mostPlayedSongs = mostPlayedSongs,
                                    currentSongId = currentSong?.id,
                                    onSongClick = { song -> viewModel.playSong(song.uri) },
                                    state = tracksListState
                                )
                            }
                        }
                        1 -> {
                            // Folders
                            if (folders.isEmpty()) EmptyState("No folders found")
                            else FolderList(
                                folders = folders,
                                onFolderClick = { folder ->
                                    onNavigateToFolder(folder.path)
                                },
                                state = foldersListState
                            )
                        }
                        2 -> {
                            // Albums
                            if (albums.isEmpty()) EmptyState("No albums found")
                            else AlbumList(
                                albums = albums,
                                onAlbumClick = { album ->
                                    val songs = viewModel.getSongsByAlbum(album.name)
                                    songs.firstOrNull()?.let { viewModel.playSong(it.uri) }
                                },
                                state = albumsListState
                            )
                        }
                        3 -> {
                            // Artists
                            if (artists.isEmpty()) EmptyState("No artists found")
                            else ArtistList(
                                artists = artists,
                                onArtistClick = { artist ->
                                    val songs = viewModel.getSongsByArtist(artist.name)
                                    songs.firstOrNull()?.let { viewModel.playSong(it.uri) }
                                },
                                state = artistsListState
                            )
                        }
                        4 -> {
                            // Favorites
                            if (filteredSongs.isEmpty()) EmptyState("No favorites yet")
                            else SongList(
                                songs = filteredSongs,
                                currentSongId = currentSong?.id,
                                onSongClick = { song -> viewModel.playSong(song.uri) },
                                state = favoritesListState
                            )
                        }
                    }
                }

                val activeListState = when (selectedTabIndex) {
                    0 -> tracksListState
                    1 -> foldersListState
                    2 -> albumsListState
                    3 -> artistsListState
                    else -> favoritesListState
                }
                val activeTotalItems = when (selectedTabIndex) {
                    0 -> {
                        if (searchQuery.text.isNotBlank()) {
                            filteredSongs.size
                        } else {
                            val rails = listOf(recentPlayedSongs, mostPlayedSongs, recentAddedSongs).count { it.isNotEmpty() }
                            allFavorites.size + songList.size + (rails * 2) + 1
                        }
                    }
                    1 -> folders.size
                    2 -> albums.size
                    3 -> artists.size
                    else -> filteredSongs.size
                }

                SideScrollIndicator(
                    listState = activeListState,
                    totalItems = activeTotalItems,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(top = 12.dp, bottom = 108.dp, end = 4.dp)
                        .fillMaxHeight()
                )
            }
        } // End of outer Column (372)
            
            // --- Cinematic Hero Plate Overlay ---
            // Only show if animating or active to optimizations
            if (isColdStart || plateOffsetY > (-1000).dp) {
                com.eplaytime.app.ui.components.HeroGlassPlate(
                    userName = userName,
                    greeting = greeting,
                    onDismiss = { isColdStart = false },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = plateOffsetY)
                )
            }
        }
    }
}

private fun isSongFromHiddenFolder(song: Song): Boolean {
    val bucket = song.bucketDisplayName.trim()
    return bucket.startsWith(".") || containsHiddenSegment(song.folderPath)
}

private fun containsHiddenSegment(path: String): Boolean {
    if (path.isBlank()) return false
    val normalized = path.replace('\\', '/')
    return normalized
        .split('/')
        .any { segment -> segment.isNotBlank() && segment.startsWith(".") }
}

@Composable
private fun SpotifyTracksFeed(
    allSongs: List<Song>,
    recentAddedSongs: List<Song>,
    recentlyPlayedSongs: List<Song>,
    mostPlayedSongs: List<Song>,
    currentSongId: Long?,
    onSongClick: (Song) -> Unit,
    state: androidx.compose.foundation.lazy.LazyListState
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        if (recentlyPlayedSongs.isNotEmpty()) {
            item {
                HomeSectionTitle("Recently Played")
            }
            item {
                SongHorizontalRail(songs = recentlyPlayedSongs.take(12), currentSongId = currentSongId, onSongClick = onSongClick)
            }
        }

        if (mostPlayedSongs.isNotEmpty()) {
            item {
                HomeSectionTitle("Most Played")
            }
            item {
                SongHorizontalRail(songs = mostPlayedSongs.take(12), currentSongId = currentSongId, onSongClick = onSongClick)
            }
        }

        if (recentAddedSongs.isNotEmpty()) {
            item {
                HomeSectionTitle("Recently Added")
            }
            item {
                SongHorizontalRail(songs = recentAddedSongs.take(12), currentSongId = currentSongId, onSongClick = onSongClick)
            }
        }

        item {
            HomeSectionTitle("All Tracks")
        }

        items(items = allSongs, key = { it.uri }, contentType = { "track_row" }) { song ->
            SongRowItem(song = song, isCurrent = song.id == currentSongId, onClick = { onSongClick(song) })
        }
    }
}

@Composable
private fun HomeSectionTitle(title: String) {
    Text(
        text = title,
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = TextPrimary
    )
}

@Composable
private fun SongHorizontalRail(
    songs: List<Song>,
    currentSongId: Long?,
    onSongClick: (Song) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(items = songs, key = { it.uri }, contentType = { "rail_song" }) { song ->
            SongRailCard(song = song, isCurrent = song.id == currentSongId, onClick = { onSongClick(song) })
        }
    }
}

@Composable
private fun SongRailCard(
    song: Song,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) BrandGlowColor.copy(alpha = 0.18f) else BlackSurface)
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        AsyncImage(
            model = if (!song.albumArtUri.isNullOrBlank()) song.albumArtUri else song.uri,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(10.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = TextPrimary
        )
        Text(
            text = song.artist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun SongRowItem(
    song: Song,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) BrandGlowColor.copy(alpha = 0.14f) else BlackSurface)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = if (!song.albumArtUri.isNullOrBlank()) song.albumArtUri else song.uri,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(10.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = TextPrimary
            )
            Text(
                text = song.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun SideScrollIndicator(
    listState: androidx.compose.foundation.lazy.LazyListState,
    totalItems: Int,
    modifier: Modifier = Modifier
) {
    val scrollInfo by remember {
        derivedStateOf {
            val layoutTotal = listState.layoutInfo.totalItemsCount
            val effectiveTotal = if (layoutTotal > 0) layoutTotal else totalItems
            val visible = listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
            val maxScroll = (effectiveTotal - visible).coerceAtLeast(1)
            val fraction = (listState.firstVisibleItemIndex.toFloat() / maxScroll.toFloat()).coerceIn(0f, 1f)
            Triple(effectiveTotal, visible, fraction)
        }
    }
    val (effectiveTotalItems, visibleCount, scrollFraction) = scrollInfo
    val maxScrollable = (effectiveTotalItems - visibleCount).coerceAtLeast(1)
    if (effectiveTotalItems <= 0) return
    if (visibleCount >= effectiveTotalItems) return
    val thumbFraction = (visibleCount.toFloat() / effectiveTotalItems.toFloat()).coerceIn(0.08f, 0.45f)
    val scope = rememberCoroutineScope()
    var indicatorHeightPx by remember { mutableFloatStateOf(1f) }

    fun jumpToFraction(fraction: Float) {
        val safeFraction = fraction.coerceIn(0f, 1f)
        val targetIndex = (safeFraction * maxScrollable.toFloat()).toInt().coerceIn(0, maxScrollable)
        scope.launch {
            listState.scrollToItem(targetIndex)
        }
    }

    Box(
        modifier = modifier
            .width(24.dp)
            .onSizeChanged { indicatorHeightPx = it.height.toFloat().coerceAtLeast(1f) }
            .pointerInput(maxScrollable, indicatorHeightPx) {
                detectTapGestures { offset ->
                    jumpToFraction(offset.y / indicatorHeightPx)
                }
            }
            .pointerInput(maxScrollable, indicatorHeightPx) {
                detectDragGestures(
                    onDragStart = { offset ->
                        jumpToFraction(offset.y / indicatorHeightPx)
                    },
                    onDrag = { change, _ ->
                        jumpToFraction(change.position.y / indicatorHeightPx)
                    }
                )
            }
            .drawBehind {
                val trackWidth = size.width * 0.22f
                val x = (size.width - trackWidth) / 2f
                drawRoundRect(
                    color = TextSecondary.copy(alpha = 0.28f),
                    topLeft = androidx.compose.ui.geometry.Offset(x, 0f),
                    size = androidx.compose.ui.geometry.Size(trackWidth, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackWidth, trackWidth)
                )

                val thumbHeight = size.height * thumbFraction
                val thumbY = (size.height - thumbHeight) * scrollFraction
                drawRoundRect(
                    color = SoftGold.copy(alpha = if (listState.isScrollInProgress) 0.9f else 0.65f),
                    topLeft = androidx.compose.ui.geometry.Offset(x, thumbY),
                    size = androidx.compose.ui.geometry.Size(trackWidth, thumbHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackWidth, trackWidth)
                )
            }
    )
}

@Composable
private fun GlassHeader(
    onSchedulerClick: () -> Unit,
    onAboutClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRefreshClick: () -> Unit,
    isLoading: Boolean,
    hasNewSongs: Boolean,
    userName: String?,
    onSearchClick: () -> Unit,
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
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(shimmerTranslate, shimmerTranslate)
    )

    // Pulse animation for New Songs (Refresh Button)
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (hasNewSongs) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Rotation animation for refresh icon
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "rotation"
    )
    
    // Dynamic Greeting Logic (Local to Header for display)
    val greeting = remember(userName) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeGreeting = when (hour) {
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
        if (userName != null) "$timeGreeting, $userName!" else "$timeGreeting!"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BlackSurface.copy(alpha = 0.5f)) // Semi-transparent base
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        BrandGlowColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset.Unspecified,
                    radius = 800f
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // LEFT: Name Plate (Logo + Text)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Rounded Square Logo with Shimmer Border
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .border(2.dp, shimmerBrush, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAboutClick() }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.audia_player_logo),
                        contentDescription = "Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Greeting & Name
                Column {
                    Text(
                        text = greeting.split(",")[0], // Just the "Good Morning" part if comma exists
                        fontFamily = GaladaFontFamily, // Rich Elegant Font
                        fontWeight = FontWeight.Normal,
                        fontSize = 20.sp, // Larger for cursive font visibility
                        color = TextSecondary
                    )
                    Text(
                        text = userName ?: "Audia User",
                        fontFamily = GaladaFontFamily, // Rich Elegant Font
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp, // Prominent
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // RIGHT: Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Search
                IconButton(
                    onClick = onSearchClick, 
                    modifier = Modifier.size(32.dp).offset(y = searchIconOffset)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SoftGold,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 2. Refresh (with Pulse/Rotation)
                IconButton(
                    onClick = {
                        if (!isRefreshing && !isLoading) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isRefreshing = true
                            onRefreshClick()
                            scope.launch {
                                kotlinx.coroutines.delay(1500)
                                isRefreshing = false
                            }
                        }
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .scale(if (hasNewSongs) pulseScale else 1f)
                        .offset(y = refreshIconOffset)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan",
                        tint = if (hasNewSongs) Color.Red else if (isRefreshing || isLoading) SoftGold else TextSecondary,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer { rotationZ = if (isRefreshing || isLoading) rotation else 0f }
                    )
                }

                // 3. Scheduler
                IconButton(
                    onClick = onSchedulerClick, 
                    modifier = Modifier.size(32.dp).offset(y = otherIconsOffset)
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = "Scheduler",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 4. Settings
                IconButton(
                    onClick = onSettingsClick, 
                    modifier = Modifier.size(32.dp).offset(y = otherIconsOffset)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}



@Composable
private fun FolderList(
    folders: List<MusicFolder>,
    onFolderClick: (MusicFolder) -> Unit,
    state: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = folders, key = { it.path }, contentType = { "folder" }) { folder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BlackSurface)
                    .clickable { onFolderClick(folder) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = SoftGold,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.name,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "${folder.songCount} songs",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumList(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit,
    state: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = albums, key = { it.id }, contentType = { "album" }) { album ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BlackSurface)
                    .clickable { onAlbumClick(album) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = album.albumArtUri ?: R.drawable.audia_player_logo,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BlackCard),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.name,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${album.artist} • ${album.songCount} songs",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistList(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit,
    state: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = artists, key = { it.id }, contentType = { "artist" }) { artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BlackSurface)
                    .clickable { onArtistClick(artist) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SoftGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = artist.name.first().uppercase(),
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = SoftGold
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artist.name,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "${artist.songCount} tracks",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = SoftGold,
                strokeWidth = 3.dp
            )
            Text(
                text = "Scanning music library...",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = 16.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun EmptyState(message: String = "No music found") {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicOff,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = message,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = TextPrimary
            )
            Text(
                text = "Add music to your device\nand tap refresh to scan",
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Light,
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, remainingSeconds)
}
