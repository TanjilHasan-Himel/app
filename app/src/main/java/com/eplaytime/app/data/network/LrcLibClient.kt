package com.eplaytime.app.data.network

import android.content.Context
import android.net.Uri
import android.util.Log
import com.eplaytime.app.data.database.LyricsDao
import com.eplaytime.app.data.database.LyricsEntity
import com.eplaytime.app.data.model.ParsedLyrics
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.util.LrcParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class LrcLibClient(
    private val context: Context,
    private val lyricsDao: LyricsDao
) {
    companion object {
        private const val TAG = "LrcLibClient"
        private const val BASE_URL = "https://lrclib.net/api/get"
        private const val USER_AGENT = "AudiaPlayer/9.0.0 (https://github.com/TanjilHasan-Himel)"
    }

    suspend fun getLyricsForSong(song: Song): ParsedLyrics? = withContext(Dispatchers.IO) {
        val songUri = song.uri.toString()

        // 1. Check for Local .LRC file adjacent to audio file
        val localLrc = loadLocalLrcFile(song)
        if (localLrc != null) {
            Log.d(TAG, "Loaded lyrics from local .lrc file for: ${song.title}")
            return@withContext LrcParser.parse(localLrc)
        }

        // 2. Check Room Database Cache
        try {
            val cached = lyricsDao.getLyrics(songUri) ?: lyricsDao.getLyricsByMetadata(cleanQuery(song.title), cleanQuery(song.artist))
            if (cached != null) {
                val lrcText = cached.syncedLyrics ?: cached.plainLyrics
                if (!lrcText.isNullOrBlank()) {
                    Log.d(TAG, "Loaded lyrics from Room cache for: ${song.title}")
                    return@withContext LrcParser.parse(lrcText)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Database cache read error: ${e.message}")
        }

        // 3. Query LRCLIB Open API
        try {
            val cleanTitle = cleanQuery(song.title)
            val cleanArtist = cleanQuery(song.artist)
            val durationSec = (song.duration / 1000L).coerceAtLeast(1L)

            val queryUrl = buildString {
                append(BASE_URL)
                append("?track_name=").append(URLEncoder.encode(cleanTitle, "UTF-8"))
                if (cleanArtist.isNotBlank() && cleanArtist != "<unknown>") {
                    append("&artist_name=").append(URLEncoder.encode(cleanArtist, "UTF-8"))
                }
                append("&duration=").append(durationSec)
            }

            Log.d(TAG, "Fetching lyrics from LRCLIB: $queryUrl")
            val connection = (URL(queryUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                connectTimeout = 4000
                readTimeout = 4000
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val jsonString = reader.readText()
                reader.close()

                val json = JSONObject(jsonString)
                val syncedLyrics = json.optString("syncedLyrics").takeIf { it.isNotBlank() && it != "null" }
                val plainLyrics = json.optString("plainLyrics").takeIf { it.isNotBlank() && it != "null" }

                val chosenText = syncedLyrics ?: plainLyrics
                if (!chosenText.isNullOrBlank()) {
                    // Cache in Room
                    try {
                        lyricsDao.insertLyrics(
                            LyricsEntity(
                                songUri = songUri,
                                title = cleanTitle,
                                artist = cleanArtist,
                                syncedLyrics = syncedLyrics,
                                plainLyrics = plainLyrics,
                                source = "LRCLIB"
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to cache lyrics in DB: ${e.message}")
                    }

                    return@withContext LrcParser.parse(chosenText)
                }
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                Log.d(TAG, "Exact match not found on LRCLIB, falling back to search for: $cleanTitle")
                val searchUrl = "https://lrclib.net/api/search?q=${URLEncoder.encode(cleanTitle, "UTF-8")}"
                val searchConn = (URL(searchUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", USER_AGENT)
                    connectTimeout = 4000
                    readTimeout = 4000
                }
                if (searchConn.responseCode == HttpURLConnection.HTTP_OK) {
                    val searchReader = BufferedReader(InputStreamReader(searchConn.inputStream))
                    val searchJson = searchReader.readText()
                    searchReader.close()

                    val array = org.json.JSONArray(searchJson)
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        val syncedLyrics = item.optString("syncedLyrics").takeIf { it.isNotBlank() && it != "null" }
                        val plainLyrics = item.optString("plainLyrics").takeIf { it.isNotBlank() && it != "null" }
                        val chosenText = syncedLyrics ?: plainLyrics
                        if (!chosenText.isNullOrBlank()) {
                            try {
                                lyricsDao.insertLyrics(
                                    LyricsEntity(
                                        songUri = songUri,
                                        title = cleanTitle,
                                        artist = cleanArtist,
                                        syncedLyrics = syncedLyrics,
                                        plainLyrics = plainLyrics,
                                        source = "LRCLIB_SEARCH"
                                    )
                                )
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to cache lyrics in DB: ${e.message}")
                            }
                            return@withContext LrcParser.parse(chosenText)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "LRCLIB request failed (offline or network error): ${e.message}")
        }

        null
    }

    private fun loadLocalLrcFile(song: Song): String? {
        return try {
            val path = song.folderPath
            if (path.isNotBlank()) {
                val songFile = File(path)
                val parentDir = songFile.parentFile
                if (parentDir != null && parentDir.exists()) {
                    val nameWithoutExt = songFile.nameWithoutExtension
                    val lrcFile = File(parentDir, "$nameWithoutExt.lrc")
                    if (lrcFile.exists() && lrcFile.canRead()) {
                        return lrcFile.readText()
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanQuery(text: String): String {
        return text
            .replace(Regex("(?i)\\s*\\(from\\s+.*\\)"), "")
            .replace(Regex("(?i)\\s*\\(official.*\\)"), "")
            .replace(Regex("(?i)\\s*\\[official.*\\]"), "")
            .replace(Regex("(?i)\\s*\\(feat\\..*\\)"), "")
            .replace(Regex("(?i)\\s*\\[.*?\\]"), "")
            .trim()
    }
}

