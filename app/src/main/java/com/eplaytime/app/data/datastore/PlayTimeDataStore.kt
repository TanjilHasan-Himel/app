package com.eplaytime.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension for DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "playtime_settings")

/**
 * Playback state to remember
 */
data class PlaybackState(
    val lastSongUri: String = "",
    val lastPosition: Long = 0L,
    val lastPlaybackSpeed: Float = 1.0f,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = 0,
    val lastVolume: Float = 0.5f
)

data class LoopPreset(
    val name: String,
    val startMs: Long,
    val endMs: Long
)

/**
 * PlayTimeDataStore - Persistent storage for app settings and playback state
 */
@Singleton
class PlayTimeDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val LAST_SONG_URI = stringPreferencesKey("last_song_uri")
        val LAST_POSITION = longPreferencesKey("last_position")
        val LAST_PLAYBACK_SPEED = floatPreferencesKey("last_playback_speed")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE = intPreferencesKey("repeat_mode")
        val LAST_VOLUME = floatPreferencesKey("last_volume")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val FILTER_SHORT_AUDIO = booleanPreferencesKey("filter_short_audio") // Hide clips < 30s
        val FILTER_CALL_RECORDINGS = booleanPreferencesKey("filter_call_recordings") // Hide call/voice recordings
        val USER_NAME = stringPreferencesKey("user_name")
        val LAST_OPENED_TIME = longPreferencesKey("last_opened_time")
        val LAST_NOTIFICATION_TIME = longPreferencesKey("last_notification_time")
        val STOP_AFTER_CURRENT = booleanPreferencesKey("stop_after_current")
        val FADE_ON_PLAY_PAUSE = booleanPreferencesKey("fade_on_play_pause")
        val LOOP_PRESETS = stringPreferencesKey("loop_presets")
        val RECENT_PLAYED_URIS = stringPreferencesKey("recent_played_uris")
        val PLAY_COUNTS = stringPreferencesKey("play_counts")
        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
        val BASS_BOOST_STRENGTH = intPreferencesKey("bass_boost_strength")
        val EQ_BANDS_LEVELS = stringPreferencesKey("eq_bands_levels")
    }

    private fun routePositionKey(songUri: String, route: String): Preferences.Key<Long> {
        val safeRoute = route.lowercase().replace("[^a-z0-9_]".toRegex(), "_")
        val keyName = "route_pos_${safeRoute}_${songUri.hashCode()}"
        return longPreferencesKey(keyName)
    }

    private fun parseLoopPresets(raw: String): Map<String, List<LoopPreset>> {
        if (raw.isBlank()) return emptyMap()
        val grouped = mutableMapOf<String, MutableList<LoopPreset>>()
        raw.split(";;")
            .filter { it.isNotBlank() }
            .forEach { row ->
                val parts = row.split("|~|")
                if (parts.size == 4) {
                    val uri = parts[0]
                    val name = parts[1]
                    val start = parts[2].toLongOrNull()
                    val end = parts[3].toLongOrNull()
                    if (start != null && end != null && end > start) {
                        grouped.getOrPut(uri) { mutableListOf() }.add(
                            LoopPreset(name = name, startMs = start, endMs = end)
                        )
                    }
                }
            }
        return grouped
    }

    private fun encodeLoopPresets(presets: Map<String, List<LoopPreset>>): String {
        return presets.entries
            .flatMap { (uri, list) ->
                list.map { preset ->
                    listOf(uri, preset.name, preset.startMs.toString(), preset.endMs.toString())
                        .joinToString("|~|")
                }
            }
            .joinToString(";;")
    }

    private fun parseRecentUris(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.split(";;").filter { it.isNotBlank() }
    }

    private fun encodeRecentUris(list: List<String>): String {
        return list.joinToString(";;")
    }

    private fun parsePlayCounts(raw: String): Map<String, Int> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(";;")
            .mapNotNull { row ->
                val parts = row.split("|~|")
                if (parts.size != 2) return@mapNotNull null
                val count = parts[1].toIntOrNull() ?: return@mapNotNull null
                parts[0] to count
            }
            .toMap()
    }

    private fun encodePlayCounts(map: Map<String, Int>): String {
        return map.entries.joinToString(";;") { (uri, count) ->
            "$uri|~|$count"
        }
    }

    /**
     * Get playback state as Flow
     */
    val playbackState: Flow<PlaybackState> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            PlaybackState(
                lastSongUri = prefs[Keys.LAST_SONG_URI] ?: "",
                lastPosition = prefs[Keys.LAST_POSITION] ?: 0L,
                lastPlaybackSpeed = prefs[Keys.LAST_PLAYBACK_SPEED] ?: 1.0f,
                shuffleEnabled = prefs[Keys.SHUFFLE_ENABLED] ?: false,
                repeatMode = prefs[Keys.REPEAT_MODE] ?: 0,
                lastVolume = prefs[Keys.LAST_VOLUME] ?: 0.5f
            )
        }

    /**
     * Save playback state
     */
    suspend fun savePlaybackState(
        songUri: String,
        position: Long,
        playbackSpeed: Float = 1.0f,
        shuffleEnabled: Boolean = false,
        repeatMode: Int = 0
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SONG_URI] = songUri
            prefs[Keys.LAST_POSITION] = position
            prefs[Keys.LAST_PLAYBACK_SPEED] = playbackSpeed
            prefs[Keys.SHUFFLE_ENABLED] = shuffleEnabled
            prefs[Keys.REPEAT_MODE] = repeatMode
        }
    }

    /**
     * Save last song URI only
     */
    suspend fun saveLastSong(songUri: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SONG_URI] = songUri
        }
    }

    /**
     * Save last position only
     */
    suspend fun saveLastPosition(position: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_POSITION] = position
        }
    }

    /**
     * Clear playback state
     */
    suspend fun clearPlaybackState() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.LAST_SONG_URI)
            prefs.remove(Keys.LAST_POSITION)
        }
    }

    /**
     * Get theme mode
     */
    val themeMode: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.THEME_MODE] ?: "dark" }

    /**
     * Save theme mode
     */
    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode
        }
    }

    /**
     * Get filter short audio setting
     */
    val filterShortAudio: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.FILTER_SHORT_AUDIO] ?: false }

    /**
     * Save filter short audio setting
     */
    suspend fun setFilterShortAudio(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FILTER_SHORT_AUDIO] = enabled
        }
    }

    /**
     * Get filter call recordings setting
     */
    val filterCallRecordings: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.FILTER_CALL_RECORDINGS] ?: false }

    /**
     * Save filter call recordings setting
     */
    suspend fun setFilterCallRecordings(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FILTER_CALL_RECORDINGS] = enabled
        }
    }

    /**
     * Get user name
     */
    val userName: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.USER_NAME] }

    /**
     * Save user name
     */
    suspend fun setUserName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = name
        }
    }

    /**
     * Get has seen onboarding
     */
    val hasSeenOnboarding: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.HAS_SEEN_ONBOARDING] ?: false }

    /**
     * Save has seen onboarding
     */
    suspend fun setHasSeenOnboarding(seen: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAS_SEEN_ONBOARDING] = seen
        }
    }

    /**
     * Get last opened time
     */
    val lastOpenedTime: Flow<Long> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.LAST_OPENED_TIME] ?: 0L }

    /**
     * Save last opened time
     */
    suspend fun setLastOpenedTime(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_OPENED_TIME] = timestamp
        }
    }

    /**
     * Get last notification time
     */
    val lastNotificationTime: Flow<Long> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.LAST_NOTIFICATION_TIME] ?: 0L }

    /**
     * Save last notification time
     */
    suspend fun setLastNotificationTime(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_NOTIFICATION_TIME] = timestamp
        }
    }

    val stopAfterCurrent: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.STOP_AFTER_CURRENT] ?: false }

    suspend fun setStopAfterCurrent(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.STOP_AFTER_CURRENT] = enabled
        }
    }

    val fadeOnPlayPause: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.FADE_ON_PLAY_PAUSE] ?: true }

    suspend fun setFadeOnPlayPause(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FADE_ON_PLAY_PAUSE] = enabled
        }
    }

    val loopPresets: Flow<Map<String, List<LoopPreset>>> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            parseLoopPresets(prefs[Keys.LOOP_PRESETS] ?: "")
        }

    suspend fun saveLoopPreset(songUri: String, preset: LoopPreset) {
        context.dataStore.edit { prefs ->
            val current = parseLoopPresets(prefs[Keys.LOOP_PRESETS] ?: "")
            val updated = current.toMutableMap()
            val list = updated[songUri].orEmpty().toMutableList()
            val deduped = list.filterNot { it.name == preset.name }.toMutableList()
            deduped.add(preset)
            updated[songUri] = deduped.sortedBy { it.startMs }
            prefs[Keys.LOOP_PRESETS] = encodeLoopPresets(updated)
        }
    }

    suspend fun removeLoopPreset(songUri: String, name: String) {
        context.dataStore.edit { prefs ->
            val current = parseLoopPresets(prefs[Keys.LOOP_PRESETS] ?: "")
            val updated = current.toMutableMap()
            val list = updated[songUri].orEmpty().filterNot { it.name == name }
            if (list.isEmpty()) {
                updated.remove(songUri)
            } else {
                updated[songUri] = list
            }
            prefs[Keys.LOOP_PRESETS] = encodeLoopPresets(updated)
        }
    }

    suspend fun saveRoutePosition(songUri: String, route: String, positionMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[routePositionKey(songUri, route)] = positionMs
        }
    }

    suspend fun getRoutePosition(songUri: String, route: String): Long {
        val prefs = context.dataStore.data.firstOrNull() ?: emptyPreferences()
        return prefs[routePositionKey(songUri, route)] ?: 0L
    }

    val recentPlayedUris: Flow<List<String>> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> parseRecentUris(prefs[Keys.RECENT_PLAYED_URIS] ?: "") }

    suspend fun addRecentPlayedSong(songUri: String, maxItems: Int = 60) {
        context.dataStore.edit { prefs ->
            val current = parseRecentUris(prefs[Keys.RECENT_PLAYED_URIS] ?: "")
            val updated = listOf(songUri) + current.filterNot { it == songUri }
            prefs[Keys.RECENT_PLAYED_URIS] = encodeRecentUris(updated.take(maxItems))
        }
    }

    val playCounts: Flow<Map<String, Int>> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> parsePlayCounts(prefs[Keys.PLAY_COUNTS] ?: "") }

    suspend fun incrementPlayCount(songUri: String) {
        context.dataStore.edit { prefs ->
            val counts = parsePlayCounts(prefs[Keys.PLAY_COUNTS] ?: "").toMutableMap()
            counts[songUri] = (counts[songUri] ?: 0) + 1
            prefs[Keys.PLAY_COUNTS] = encodePlayCounts(counts)
        }
    }

    val bassBoostStrength: Flow<Int> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.BASS_BOOST_STRENGTH] ?: 0 }

    suspend fun setBassBoostStrength(strength: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BASS_BOOST_STRENGTH] = strength
        }
    }

    val eqBandsLevels: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[Keys.EQ_BANDS_LEVELS] ?: "" }

    suspend fun setEqBandsLevels(levels: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EQ_BANDS_LEVELS] = levels
        }
    }
}
