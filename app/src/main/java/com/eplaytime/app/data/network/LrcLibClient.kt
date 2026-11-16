package com.eplaytime.app.data.network

import android.content.Context
import android.util.Log
import com.eplaytime.app.data.database.LyricsDao
import com.eplaytime.app.data.database.LyricsEntity
import com.eplaytime.app.data.model.ParsedLyrics
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.util.LrcParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.coroutineContext
import kotlin.math.min
import kotlin.random.Random

class LrcLibClient(
    private val context: Context,
    private val lyricsDao: LyricsDao
) {
    companion object {
        private const val TAG = "LrcLibClient"
        private const val BASE_URL = "https://lrclib.net/api"
        private const val USER_AGENT = "AudiaPlayer/9.0 (https://github.com/TanjilHasan-Himel)"
    }

    data class Lookup(
        val lyrics: ParsedLyrics?,
        val candidates: List<LyricsCandidate> = emptyList(),
        val retryAfterSeconds: Long? = null
    )

    suspend fun getLyricsForSong(song: Song, ignoreCache: Boolean = false): Lookup =
        withContext(Dispatchers.IO) {
            loadLocalLrcFile(song)?.let { return@withContext Lookup(LrcParser.parse(it)) }
            if (!ignoreCache) cachedLyrics(song.uri)?.let { return@withContext Lookup(it) }

            val exact = request(buildExactUrl(song))
            if (exact.code == HttpURLConnection.HTTP_OK) {
                val candidate = parseCandidate(JSONObject(exact.body.orEmpty()))
                if (candidate?.hasLyrics == true) {
                    return@withContext Lookup(cacheAndParse(song, candidate, "LRCLIB_EXACT"))
                }
            }
            if (exact.retryAfterSeconds != null) {
                return@withContext Lookup(null, retryAfterSeconds = exact.retryAfterSeconds)
            }
            if (exact.code != HttpURLConnection.HTTP_NOT_FOUND) return@withContext Lookup(null)

            delay(250L)
            val search = request(buildSearchUrl(song))
            if (search.retryAfterSeconds != null) {
                return@withContext Lookup(null, retryAfterSeconds = search.retryAfterSeconds)
            }
            if (search.code != HttpURLConnection.HTTP_OK) return@withContext Lookup(null)

            val candidates = parseCandidates(search.body.orEmpty(), song)
            val best = candidates.firstOrNull()
            if (best != null && best.score >= LyricsCandidateScorer.AUTO_ACCEPT_SCORE) {
                Lookup(cacheAndParse(song, best, "LRCLIB_SEARCH"))
            } else {
                Lookup(null, candidates)
            }
        }

    suspend fun selectCandidate(song: Song, candidate: LyricsCandidate): ParsedLyrics? =
        withContext(Dispatchers.IO) {
            if (!candidate.hasLyrics) null else cacheAndParse(song, candidate, "LRCLIB_USER_SELECTED")
        }

    suspend fun clearCachedLyrics(song: Song) = withContext(Dispatchers.IO) {
        lyricsDao.deleteLyrics(song.uri)
    }

    private suspend fun cachedLyrics(songUri: String): ParsedLyrics? = try {
        lyricsDao.getLyrics(songUri)?.let { cached ->
            (cached.syncedLyrics ?: cached.plainLyrics)
                ?.takeIf(String::isNotBlank)
                ?.let(LrcParser::parse)
        }
    } catch (error: Exception) {
        Log.w(TAG, "Lyrics cache read failed", error)
        null
    }

    private suspend fun cacheAndParse(
        song: Song,
        candidate: LyricsCandidate,
        source: String
    ): ParsedLyrics? {
        val text = candidate.syncedLyrics ?: candidate.plainLyrics ?: return null
        try {
            lyricsDao.insertLyrics(
                LyricsEntity(
                    songUri = song.uri,
                    title = cleanQuery(song.title),
                    artist = cleanQuery(song.artist),
                    syncedLyrics = candidate.syncedLyrics,
                    plainLyrics = candidate.plainLyrics,
                    source = source
                )
            )
        } catch (error: Exception) {
            Log.w(TAG, "Lyrics cache write failed", error)
        }
        return LrcParser.parse(text)
    }

    private fun parseCandidates(json: String, song: Song): List<LyricsCandidate> {
        val array = JSONArray(json)
        val raw = buildList {
            for (index in 0 until array.length()) parseCandidate(array.optJSONObject(index))?.let(::add)
        }
        return LyricsCandidateScorer.rank(
            candidates = raw,
            title = cleanQuery(song.title),
            artist = cleanQuery(song.artist),
            album = cleanQuery(song.album),
            durationSeconds = song.duration / 1000L
        )
    }

    private fun parseCandidate(json: JSONObject?): LyricsCandidate? = json?.let {
        LyricsCandidate(
            id = it.optLong("id", -1L),
            title = it.optString("trackName", it.optString("name")),
            artist = it.optString("artistName"),
            album = it.optString("albumName"),
            durationSeconds = it.optLong("duration", 0L),
            syncedLyrics = it.nullableString("syncedLyrics"),
            plainLyrics = it.nullableString("plainLyrics")
        )
    }

    private fun JSONObject.nullableString(name: String): String? =
        optString(name).takeIf { it.isNotBlank() && it != "null" }

    private fun buildExactUrl(song: Song): String = buildString {
        append("$BASE_URL/get?track_name=${encode(cleanQuery(song.title))}")
        append("&artist_name=${encode(cleanQuery(song.artist).ifBlank { "Unknown Artist" })}")
        cleanQuery(song.album).takeIf(String::isNotBlank)?.let { append("&album_name=${encode(it)}") }
        (song.duration / 1000L).takeIf { it in 1..3600 }?.let { append("&duration=$it") }
    }

    private fun buildSearchUrl(song: Song): String = buildString {
        append("$BASE_URL/search?track_name=${encode(cleanQuery(song.title))}")
        cleanQuery(song.artist).takeUnless(::isUnknownArtist)?.let { append("&artist_name=${encode(it)}") }
        cleanQuery(song.album).takeIf(String::isNotBlank)?.let { append("&album_name=${encode(it)}") }
    }

    private suspend fun request(url: String): HttpResult {
        var attempt = 0
        while (true) {
            coroutineContext.ensureActive()
            val result = execute(url)
            if (result.code !in listOf(429, 503) || attempt == 1) return result
            val retrySeconds = result.retryAfterSeconds ?: 1L
            val jitterBound = min(retrySeconds * 300L, 30_000L).coerceAtLeast(1L)
            delay(retrySeconds * 1000L + Random.nextLong(jitterBound))
            attempt++
        }
    }

    private fun execute(url: String): HttpResult {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            connectTimeout = 5_000
            readTimeout = 7_000
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            HttpResult(
                code = code,
                body = stream?.bufferedReader()?.use { it.readText() },
                retryAfterSeconds = connection.getHeaderField("Retry-After")?.toLongOrNull()
            )
        } catch (error: Exception) {
            Log.w(TAG, "LRCLIB request failed: ${error.message}")
            HttpResult(-1)
        } finally {
            connection.disconnect()
        }
    }

    private fun loadLocalLrcFile(song: Song): String? = try {
        val audioFile = File(song.folderPath)
        audioFile.parentFile
            ?.takeIf(File::exists)
            ?.let { File(it, "${audioFile.nameWithoutExtension}.lrc") }
            ?.takeIf { it.exists() && it.canRead() }
            ?.readText()
    } catch (_: Exception) {
        null
    }

    private fun cleanQuery(text: String): String = text
        .replace(Regex("(?i)\\s*\\(from\\s+.*\\)"), "")
        .replace(Regex("(?i)\\s*[\\[(](official|feat\\.).*?[\\])]"), "")
        .replace(Regex("(?i)\\s*\\[.*?]"), "")
        .trim()

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
    private fun isUnknownArtist(value: String): Boolean = value.isBlank() ||
        value.equals("<unknown>", true) || value.equals("unknown artist", true)

    private data class HttpResult(
        val code: Int,
        val body: String? = null,
        val retryAfterSeconds: Long? = null
    )
}
