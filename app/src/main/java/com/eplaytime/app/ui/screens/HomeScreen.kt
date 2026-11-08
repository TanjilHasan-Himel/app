package com.eplaytime.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.layout.ContentScale
import androidx.palette.graphics.Palette
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.ui.components.AudioEffectsSheet
import com.eplaytime.app.ui.components.HeroGlassPlate
import com.eplaytime.app.ui.components.HomeMenuBottomSheet
import com.eplaytime.app.ui.components.MiniPlayer
import com.eplaytime.app.ui.components.ScanFiltersBottomSheet
import com.eplaytime.app.ui.components.SettingsDialog
import com.eplaytime.app.ui.components.SongList
import com.eplaytime.app.ui.screens.home.*
import com.eplaytime.app.ui.theme.*
import com.eplaytime.app.ui.viewmodel.MusicViewModel
import java.util.Calendar
import java.util.Locale

/**
 * HomeScreen - Modular, Adaptive Music Hub
 * Universal support for compact, standard, and foldable devices.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    
    val requiredPermissions = remember {
        val list = mutableListOf(audioPermission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val isGranted = results[audioPermission] ?: false
        hasPermission = isGranted
        if (isGranted) {
            viewModel.rescanDevice()
        }
    }
    
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.loadLocalSongs()
        }
    }
    
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
                    onClick = { permissionLauncher.launch(requiredPermissions) },
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
    val audioSessionId by viewModel.audioSessionId.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showScanFiltersSheet by remember { mutableStateOf(false) }
    var showHomeMenuSheet by remember { mutableStateOf(false) }
    var showAudioEffectsSheet by remember { mutableStateOf(false) }
    val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsState()

    var searchQuery by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var dynamicAccentColor by remember { mutableStateOf(SoftGold) }
    var dynamicBgColor by remember { mutableStateOf(Color.Black) }

    LaunchedEffect(currentSong?.albumArtUri) {
        val albumArt = currentSong?.albumArtUri
        if (!albumArt.isNullOrBlank()) {
            val request = ImageRequest.Builder(context)
                .data(albumArt)
                .allowHardware(false)
                .build()

            val result = withContext(Dispatchers.IO) {
                context.imageLoader.execute(request)
            }

            if (result is SuccessResult) {
                val bitmap = result.drawable.toBitmap()
                val palette = Palette.from(bitmap).generate()

                palette.vibrantSwatch?.rgb?.let { dynamicAccentColor = Color(it) }
                    ?: palette.dominantSwatch?.rgb?.let { dynamicAccentColor = Color(it) }

                palette.darkVibrantSwatch?.rgb?.let { dynamicBgColor = Color(it) }
                    ?: palette.darkMutedSwatch?.rgb?.let { dynamicBgColor = Color(it) }
            }
        } else {
            dynamicAccentColor = SoftGold
            dynamicBgColor = Color.Black
        }
    }

    val animatedAccentColor by animateColorAsState(
        targetValue = dynamicAccentColor,
        animationSpec = tween(1200),
        label = "homeAccentColor"
    )
    val animatedBgColor by animateColorAsState(
        targetValue = dynamicBgColor,
        animationSpec = tween(1200),
        label = "homeBgColor"
    )

    val bgAmbientModel = remember(currentSong?.albumArtUri) {
        val artUri = currentSong?.albumArtUri
        if (!artUri.isNullOrBlank()) {
            ImageRequest.Builder(context)
                .data(artUri)
                .size(64, 64)
                .crossfade(true)
                .build()
        } else null
    }

    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }

    BackHandler(enabled = isSearchExpanded) {
        isSearchExpanded = false
        searchQuery = TextFieldValue("")
    }

    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    val tabs = remember { listOf("Tracks", "Folders", "Albums", "Artists", "Favorites") }

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
            4 -> songList.filter { song -> allFavorites.any { it.uri == song.uri } }
            else -> songList
        }
        if (query.isEmpty()) baseList
        else baseList.filter { song ->
            song.title.lowercase(Locale.US).contains(query) ||
                song.artist.lowercase(Locale.US).contains(query) ||
                song.album.lowercase(Locale.US).contains(query)
        }
    }

    val filteredFolders = remember(folders, searchQuery.text) {
        val query = searchQuery.text.trim().lowercase(Locale.US)
        if (query.isEmpty()) folders
        else folders.filter { it.name.lowercase(Locale.US).contains(query) }
    }

    val filteredAlbums = remember(albums, searchQuery.text) {
        val query = searchQuery.text.trim().lowercase(Locale.US)
        if (query.isEmpty()) albums
        else albums.filter {
            it.name.lowercase(Locale.US).contains(query) ||
            it.artist.lowercase(Locale.US).contains(query)
        }
    }

    val filteredArtists = remember(artists, searchQuery.text) {
        val query = searchQuery.text.trim().lowercase(Locale.US)
        if (query.isEmpty()) artists
        else artists.filter { it.name.lowercase(Locale.US).contains(query) }
    }

    // --- Cinematic Cold Start Logic ---
    var isColdStart by rememberSaveable { mutableStateOf(true) }
    
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {}
    }

    val transition = updateTransition(targetState = isColdStart, label = "cold_start")

    val plateOffsetY by transition.animateDp(
        transitionSpec = { tween(durationMillis = 800, easing = LinearOutSlowInEasing) },
        label = "plate_exit"
    ) { state ->
        if (state) 0.dp else (-1000).dp
    }

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

    if (hasSeenOnboarding == null) {
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (isPlaying || currentSong != null) {
                Column {
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
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsBottomHeight(WindowInsets.navigationBars)
                    )
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenWidth = maxWidth
            val cardWidth = remember(screenWidth) {
                when {
                    screenWidth < 360.dp -> (screenWidth * 0.42f).coerceIn(120.dp, 140.dp)
                    screenWidth >= 600.dp -> 180.dp
                    else -> 150.dp
                }
            }

            // --- Atmospheric Ambient Gradient & Artwork Glow (YouTube Music style) ---
            if (bgAmbientModel != null) {
                AsyncImage(
                    model = bgAmbientModel,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(520.dp)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.0f to Color.Black,
                                    0.60f to Color.Black,
                                    1.0f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                        .blur(56.dp)
                        .alpha(0.38f),
                    contentScale = ContentScale.Crop
                )
            }

            val glowColor = if (bgAmbientModel != null) animatedAccentColor else BrandGlowColor
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to glowColor.copy(alpha = 0.42f),
                                0.18f to glowColor.copy(alpha = 0.22f),
                                0.38f to animatedBgColor.copy(alpha = 0.28f),
                                0.65f to BlackBackground.copy(alpha = 0.85f),
                                0.90f to BlackBackground,
                                1.0f to BlackBackground
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .nestedScroll(nestedScrollConnection)
                    .graphicsLayer { 
                        translationY = contentOffsetY.toPx() 
                        alpha = contentAlpha
                    }
            ) {
                // Header & Search
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    AnimatedVisibility(
                        visible = !isSearchExpanded,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        HomeHeader(
                            onSchedulerClick = onNavigateToScheduler,
                            onAboutClick = onNavigateToAbout,
                            onSettingsClick = { showSettingsDialog = true },
                            onRefreshClick = { showScanFiltersSheet = true },
                            isLoading = isLoading,
                            hasNewSongs = hasNewSongs,
                            userName = userName,
                            onSearchClick = { isSearchExpanded = true },
                            onMenuClick = { showHomeMenuSheet = true },
                            searchIconOffset = searchIconOffset,
                            refreshIconOffset = refreshIconOffset,
                            otherIconsOffset = otherIconsOffset
                        )
                    }

                    AnimatedVisibility(
                        visible = isSearchExpanded,
                        enter = fadeIn() + slideInHorizontally(),
                        exit = fadeOut() + slideOutHorizontally()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp), 
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                isSearchExpanded = false
                                searchQuery = TextFieldValue("")
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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

                if (showSettingsDialog) {
                    SettingsDialog(
                        showDialog = showSettingsDialog,
                        onDismiss = { showSettingsDialog = false },
                        currentFilterState = isShortAudioHidden,
                        onToggleFilter = { viewModel.toggleShortAudioFilter(it) },
                        currentCallFilterState = isCallRecordingHidden,
                        onToggleCallFilter = { viewModel.toggleCallRecordingFilter(it) },
                        onOpenScanFilters = { showScanFiltersSheet = true },
                        onTermsClick = onNavigateToAbout,
                        onAboutClick = onNavigateToAbout
                    )
                }

                if (showScanFiltersSheet) {
                    ScanFiltersBottomSheet(
                        onDismiss = { showScanFiltersSheet = false },
                        viewModel = viewModel
                    )
                }

                if (showHomeMenuSheet) {
                    HomeMenuBottomSheet(
                        hasNewSongs = hasNewSongs,
                        onScanClick = { showScanFiltersSheet = true },
                        onSchedulerClick = onNavigateToScheduler,
                        onEffectsClick = {
                            viewModel.requestAudioSessionId()
                            showAudioEffectsSheet = true
                        },
                        onSettingsClick = { showSettingsDialog = true },
                        onAboutClick = onNavigateToAbout,
                        onDismiss = { showHomeMenuSheet = false }
                    )
                }

                if (showAudioEffectsSheet) {
                    AudioEffectsSheet(
                        audioSessionId = audioSessionId,
                        viewModel = viewModel,
                        onDismiss = { showAudioEffectsSheet = false }
                    )
                }

                // Category Tabs
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
                
                val tracksListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                val foldersListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                val albumsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                val artistsListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                val favoritesListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = selectedTabIndex,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200)) togetherWith
                            fadeOut(animationSpec = tween(200))
                        },
                        label = "tab_content"
                    ) { tabIndex ->
                        when (tabIndex) {
                            0 -> {
                                if (isLoading) LoadingState()
                                else if (songList.isEmpty()) EmptyState()
                                else if (searchQuery.text.isNotBlank()) {
                                    SongList(
                                        songs = filteredSongs,
                                        currentSongId = currentSong?.id,
                                        onSongClick = { song ->
                                            viewModel.playSong(song.uri)
                                            onNavigateToPlayer()
                                        },
                                        state = tracksListState
                                    )
                                } else {
                                    SpotifyTracksFeed(
                                        allSongs = songList,
                                        recentAddedSongs = recentAddedSongs,
                                        recentlyPlayedSongs = recentPlayedSongs,
                                        mostPlayedSongs = mostPlayedSongs,
                                        currentSongId = currentSong?.id,
                                        onSongClick = { song ->
                                            viewModel.playSong(song.uri)
                                            if (isSearchExpanded) {
                                                onNavigateToPlayer()
                                            }
                                        },
                                        state = tracksListState,
                                        cardWidth = cardWidth
                                    )
                                }
                            }
                            1 -> {
                                if (filteredFolders.isEmpty()) EmptyState(if (searchQuery.text.isNotBlank()) "No matching folders" else "No folders found")
                                else FolderList(
                                    folders = filteredFolders,
                                    onFolderClick = { folder -> onNavigateToFolder(folder.path) },
                                    state = foldersListState
                                )
                            }
                            2 -> {
                                if (filteredAlbums.isEmpty()) EmptyState(if (searchQuery.text.isNotBlank()) "No matching albums" else "No albums found")
                                else AlbumList(
                                    albums = filteredAlbums,
                                    onAlbumClick = { album ->
                                        val songs = viewModel.getSongsByAlbum(album.name)
                                        songs.firstOrNull()?.let { 
                                            viewModel.playSong(it.uri)
                                            if (isSearchExpanded || searchQuery.text.isNotBlank()) {
                                                onNavigateToPlayer()
                                            }
                                        }
                                    },
                                    state = albumsListState
                                )
                            }
                            3 -> {
                                if (filteredArtists.isEmpty()) EmptyState(if (searchQuery.text.isNotBlank()) "No matching artists" else "No artists found")
                                else ArtistList(
                                    artists = filteredArtists,
                                    onArtistClick = { artist ->
                                        val songs = viewModel.getSongsByArtist(artist.name)
                                        songs.firstOrNull()?.let { 
                                            viewModel.playSong(it.uri)
                                            if (isSearchExpanded || searchQuery.text.isNotBlank()) {
                                                onNavigateToPlayer()
                                            }
                                        }
                                    },
                                    state = artistsListState
                                )
                            }
                            4 -> {
                                if (filteredSongs.isEmpty()) EmptyState("No favorites yet")
                                else SongList(
                                    songs = filteredSongs,
                                    currentSongId = currentSong?.id,
                                    onSongClick = { song -> 
                                        viewModel.playSong(song.uri)
                                        if (isSearchExpanded || searchQuery.text.isNotBlank()) {
                                            onNavigateToPlayer()
                                        }
                                    },
                                    state = favoritesListState
                                )
                            }
                        }
                    }
                }
            }
                
            if (isColdStart || plateOffsetY > (-1000).dp) {
                HeroGlassPlate(
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
