package com.eplaytime.app.data.network

import android.content.Context
import android.util.Log
import com.eplaytime.app.data.database.SongWikiDao
import com.eplaytime.app.data.database.SongWikiEntity
import com.eplaytime.app.data.model.Song
import com.eplaytime.app.data.model.SongWikiData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class SongWikiClient(
    private val context: Context,
    private val wikiDao: SongWikiDao
) {
    companion object {
        private const val TAG = "SongWikiClient"
        private const val USER_AGENT = "AudiaPlayer/1.0 (taanjilhasan@gmail.com)"
        private const val CONNECT_TIMEOUT_MS = 3500
        private const val READ_TIMEOUT_MS = 3500
    }

    suspend fun getWikiForSong(song: Song): SongWikiData? = withContext(Dispatchers.IO) {
        val cleanTitle = cleanSongTitle(song.title)
        val cleanArtist = cleanArtistName(song.artist)
        val key = "${cleanTitle.lowercase()}_${cleanArtist.lowercase()}"

        // 1. Check local Room cache
        try {
            val cached = wikiDao.getWikiByKey(key)
            if (cached != null) {
                Log.d(TAG, "Loaded SongWiki from Room cache for: ${song.title}")
                return@withContext SongWikiData(
                    title = cached.title,
                    artist = cached.artist,
                    summary = cached.summary,
                    description = cached.description,
                    thumbnailUrl = cached.thumbnailUrl,
                    composer = cached.composer,
                    lyricist = cached.lyricist,
                    producer = cached.producer,
                    releaseDate = cached.releaseDate,
                    sourceUrl = cached.sourceUrl
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cache read failed: ${e.message}")
        }

        // 2 & 3. Query Wikipedia and MusicBrainz concurrently in parallel
        val (wikiSummary, credits) = coroutineScope {
            val wikiDeferred = async {
                var summary = fetchWikipediaSummary(cleanTitle, cleanArtist)
                if (summary == null && cleanArtist.isNotBlank() && cleanArtist != "<unknown>") {
                    // Fallback: search for artist page
                    summary = fetchWikipediaSummary(cleanArtist, "")
                }
                summary
            }
            val creditsDeferred = async {
                fetchMusicBrainzCredits(cleanTitle, cleanArtist)
            }
            Pair(wikiDeferred.await(), creditsDeferred.await())
        }

        if (wikiSummary == null && credits == null) {
            return@withContext null
        }

        val finalTitle = wikiSummary?.title ?: cleanTitle
        val finalArtist = cleanArtist
        val finalSummary = wikiSummary?.summary
            ?: "Track performed by $cleanArtist. Released with high-fidelity digital master audio."
        val finalDesc = wikiSummary?.description ?: credits?.releaseDate?.let { "Released in $it" }
        val finalThumbnail = wikiSummary?.thumbnailUrl
        val finalComposer = credits?.composer
        val finalLyricist = credits?.lyricist
        val finalProducer = credits?.producer
        val finalRelease = credits?.releaseDate ?: wikiSummary?.releaseDate
        val finalSource = wikiSummary?.sourceUrl ?: "https://en.wikipedia.org/wiki/${URLEncoder.encode(cleanTitle, "UTF-8")}"

        val wikiData = SongWikiData(
            title = finalTitle,
            artist = finalArtist,
            summary = finalSummary,
            description = finalDesc,
            thumbnailUrl = finalThumbnail,
            composer = finalComposer,
            lyricist = finalLyricist,
            producer = finalProducer,
            releaseDate = finalRelease,
            sourceUrl = finalSource
        )

        // 4. Cache in Room DB
        try {
            wikiDao.insertWiki(
                SongWikiEntity(
                    songKey = key,
                    title = finalTitle,
                    artist = finalArtist,
                    summary = finalSummary,
                    description = finalDesc,
                    thumbnailUrl = finalThumbnail,
                    composer = finalComposer,
                    lyricist = finalLyricist,
                    producer = finalProducer,
                    releaseDate = finalRelease,
                    sourceUrl = finalSource
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Cache insert failed: ${e.message}")
        }

        wikiData
    }

    private fun fetchWikipediaSummary(title: String, artist: String): SongWikiData? {
        val query = if (artist.isNotBlank() && artist != "<unknown>") "$title ($artist song)" else title
        val encoded = URLEncoder.encode(query.replace(' ', '_'), "UTF-8")
        val directUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/$encoded"

        val directResult = fetchJson(directUrl)
        if (directResult != null && !directResult.has("title") == false && directResult.optString("type") != "https://mediawiki.org/wiki/HyperSwitch/errors/not_found") {
            val extract = directResult.optString("extract")
            if (extract.isNotBlank()) {
                val thumb = directResult.optJSONObject("thumbnail")?.optString("source")
                val desc = directResult.optString("description")
                val pageUrl = directResult.optJSONObject("content_urls")
                    ?.optJSONObject("desktop")?.optString("page")
                return SongWikiData(
                    title = directResult.optString("title", title),
                    artist = artist,
                    summary = extract,
                    description = desc.takeIf { it.isNotBlank() },
                    thumbnailUrl = thumb,
                    sourceUrl = pageUrl
                )
            }
        }

        // Search fallback
        val searchQuery = if (artist.isNotBlank() && artist != "<unknown>") "$title $artist song" else title
        val searchUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=" +
            URLEncoder.encode(searchQuery, "UTF-8") + "&format=json&utf8=1&srlimit=1"

        val searchResult = fetchJson(searchUrl)
        val searchArray = searchResult?.optJSONObject("query")?.optJSONArray("search")
        if (searchArray != null && searchArray.length() > 0) {
            val firstHit = searchArray.getJSONObject(0)
            val hitTitle = firstHit.optString("title")
            if (hitTitle.isNotBlank()) {
                val hitEncoded = URLEncoder.encode(hitTitle.replace(' ', '_'), "UTF-8")
                val pageResult = fetchJson("https://en.wikipedia.org/api/rest_v1/page/summary/$hitEncoded")
                if (pageResult != null) {
                    val extract = pageResult.optString("extract")
                    if (extract.isNotBlank()) {
                        val thumb = pageResult.optJSONObject("thumbnail")?.optString("source")
                        val desc = pageResult.optString("description")
                        val pageUrl = pageResult.optJSONObject("content_urls")
                            ?.optJSONObject("desktop")?.optString("page")
                        return SongWikiData(
                            title = hitTitle,
                            artist = artist,
                            summary = extract,
                            description = desc.takeIf { it.isNotBlank() },
                            thumbnailUrl = thumb,
                            sourceUrl = pageUrl
                        )
                    }
                }
            }
        }
        return null
    }

    private data class MbCredits(
        val composer: String?,
        val lyricist: String?,
        val producer: String?,
        val releaseDate: String?
    )

    private fun fetchMusicBrainzCredits(title: String, artist: String): MbCredits? {
        return try {
            val queryStr = "recording:\"$title\"" + if (artist.isNotBlank() && artist != "<unknown>") " AND artist:\"$artist\"" else ""
            val url = "https://musicbrainz.org/ws/2/recording/?query=" +
                URLEncoder.encode(queryStr, "UTF-8") + "&fmt=json&limit=1"

            val json = fetchJson(url)
            val recordings = json?.optJSONArray("recordings")
            if (recordings != null && recordings.length() > 0) {
                val rec = recordings.getJSONObject(0)
                val firstReleaseDate = rec.optString("first-release-date").takeIf { it.isNotBlank() }

                var composer: String? = null
                var lyricist: String? = null
                var producer: String? = null

                val artistCredit = rec.optJSONArray("artist-credit")
                if (artistCredit != null && artistCredit.length() > 0) {
                    val primaryArtist = artistCredit.getJSONObject(0).optString("name")
                    if (composer == null) composer = primaryArtist
                }

                MbCredits(
                    composer = composer,
                    lyricist = lyricist,
                    producer = producer,
                    releaseDate = firstReleaseDate
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchJson(urlString: String): JSONObject? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
                val content = reader.use { it.readText() }
                JSONObject(content)
            } else null
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun cleanSongTitle(raw: String): String {
        return raw.replace(Regex("(?i)\\[.*?\\]|\\(.*?remix.*?\\)|\\(.*?official.*?\\)|\\(.*?video.*?\\)|\\(.*?audio.*?\\)"), "")
            .replace(Regex("^\\s*\\d+([.\\-_\\s]+)"), "")
            .replace(Regex("[_\\-]+"), " ")
            .trim()
    }

    private fun cleanArtistName(raw: String): String {
        return raw.replace(Regex("(?i)\\[.*?\\]|feat\\..*|ft\\..*"), "")
            .replace(Regex("[_\\-]+"), " ")
            .trim()
    }
}

