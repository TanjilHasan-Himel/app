package com.eplaytime.app.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.eplaytime.app.R
import com.eplaytime.app.data.model.RadioSearchField
import com.eplaytime.app.data.model.RadioStation
import com.eplaytime.app.ui.theme.*
import com.eplaytime.app.ui.viewmodel.MusicViewModel
import com.eplaytime.app.ui.viewmodel.RadioViewModel
import com.eplaytime.app.util.FirebaseDiagnostics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioScreen(
    onNavigateBack: () -> Unit,
    musicViewModel: MusicViewModel,
    radioViewModel: RadioViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        FirebaseDiagnostics.recordScreen(context, "RadioScreen")
    }
    val stations by radioViewModel.stations.collectAsState()
    val favorites by radioViewModel.favorites.collectAsState()
    val recents by radioViewModel.recents.collectAsState()
    val isLoading by radioViewModel.isLoading.collectAsState()
    val error by radioViewModel.error.collectAsState()
    val currentStation by musicViewModel.currentRadioStation.collectAsState()
    val nowPlaying by musicViewModel.radioNowPlaying.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()

    var query by remember { mutableStateOf("") }
    var field by remember { mutableStateOf(RadioSearchField.STATION) }
    var section by remember { mutableIntStateOf(0) }
    var pendingMeteredStation by remember { mutableStateOf<RadioStation?>(null) }
    val radioPrefs = remember { context.getSharedPreferences("audia_radio", Context.MODE_PRIVATE) }

    fun start(station: RadioStation) {
        val metered = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
            .isActiveNetworkMetered
        if (metered && !radioPrefs.getBoolean("allow_metered", false)) {
            pendingMeteredStation = station
        } else {
            radioViewModel.recordPlay(station)
            musicViewModel.playRadioStation(station)
        }
    }

    pendingMeteredStation?.let { station ->
        AlertDialog(
            onDismissRequest = { pendingMeteredStation = null },
            title = { Text("Use mobile data?") },
            text = { Text("Live radio can use continuous data. You can allow it once or remember this choice.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingMeteredStation = null
                    radioViewModel.recordPlay(station)
                    musicViewModel.playRadioStation(station)
                }) { Text("Play once") }
            },
            dismissButton = {
                TextButton(onClick = {
                    radioPrefs.edit().putBoolean("allow_metered", true).apply()
                    pendingMeteredStation = null
                    radioViewModel.recordPlay(station)
                    musicViewModel.playRadioStation(station)
                }) { Text("Always allow") }
            }
        )
    }

    Scaffold(
        containerColor = BlackBackground,
        topBar = {
            TopAppBar(
                title = { Text("Live Radio", fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BlackBackground)
            )
        },
        bottomBar = {
            currentStation?.let { station ->
                Surface(color = Color(0xFF17171D), tonalElevation = 6.dp) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StationLogo(station, 48)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(station.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(nowPlaying ?: "LIVE • ${station.codec} ${station.bitrate.takeIf { it > 0 }?.let { "$it kbps" }.orEmpty()}", color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                        }
                        IconButton(onClick = musicViewModel::togglePlayPause, modifier = Modifier.size(48.dp)) {
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (isPlaying) "Pause radio" else "Play radio")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Search ${field.label.lowercase()}") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = { section = 0; radioViewModel.search(query, field) }
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Search
                    )
                )
                IconButton(onClick = { section = 0; radioViewModel.search(query, field) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Search radio")
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RadioSearchField.entries.forEach { option ->
                    FilterChip(
                        selected = field == option,
                        onClick = { field = option },
                        label = { Text(option.label, fontSize = 11.sp) }
                    )
                }
            }
            Text(
                text = when (field) {
                    RadioSearchField.STATION -> "Searches station names — e.g. BBC, Dhaka FM"
                    RadioSearchField.GENRE -> "Searches station tags — e.g. rock, jazz, Quran"
                    RadioSearchField.COUNTRY -> "Uses a 2-letter country code — e.g. BD, IN, US"
                    RadioSearchField.LANGUAGE -> "Searches broadcast languages — e.g. Bangla, English"
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                color = TextSecondary,
                fontSize = 12.sp
            )
            TabRow(selectedTabIndex = section, containerColor = Color.Transparent) {
                listOf("Discover", "Favorites", "Recent").forEachIndexed { index, label ->
                    Tab(selected = section == index, onClick = { section = index }, text = { Text(label) })
                }
            }

            val visible = when (section) { 1 -> favorites; 2 -> recents; else -> stations }
            when {
                isLoading && section == 0 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SoftGold)
                }
                error != null && section == 0 -> RadioMessage(error!!, radioViewModel::loadPopular)
                visible.isEmpty() -> RadioMessage(if (section == 1) "No favorite stations yet" else "No recent stations yet")
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
                    items(visible, key = RadioStation::uuid) { station ->
                        StationRow(
                            station = station,
                            selected = currentStation?.uuid == station.uuid,
                            favorite = favorites.any { it.uuid == station.uuid },
                            onPlay = { start(station) },
                            onFavorite = { radioViewModel.toggleFavorite(station) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StationRow(
    station: RadioStation,
    selected: Boolean,
    favorite: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onPlay).background(if (selected) SoftGold.copy(alpha = 0.08f) else Color.Transparent).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StationLogo(station, 52)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(station.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listOf(station.countryCode, station.language, station.codec, station.bitrate.takeIf { it > 0 }?.let { "$it kbps" }).filterNotNull().filter(String::isNotBlank).joinToString(" • "), color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (station.isWorking) Icon(Icons.Default.CheckCircle, contentDescription = "Recently verified", tint = Color(0xFF55C98D), modifier = Modifier.size(16.dp))
        IconButton(onClick = onFavorite, modifier = Modifier.size(48.dp)) {
            Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = if (favorite) "Remove favorite" else "Add favorite", tint = if (favorite) SoftGold else TextSecondary)
        }
    }
}

@Composable
private fun StationLogo(station: RadioStation, size: Int) {
    AsyncImage(
        model = station.favicon.ifBlank { R.drawable.audia_player_logo },
        contentDescription = null,
        modifier = Modifier.size(size.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun RadioMessage(message: String, retry: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = TextSecondary)
            retry?.let { TextButton(onClick = it) { Text("Retry") } }
        }
    }
}
