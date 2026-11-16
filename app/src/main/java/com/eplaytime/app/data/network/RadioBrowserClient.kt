package com.eplaytime.app.data.network

import com.eplaytime.app.data.model.RadioSearchField
import com.eplaytime.app.data.model.RadioStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.URLEncoder

class RadioBrowserClient {
    companion object {
        private const val USER_AGENT = "AudiaPlayer/9.0"
        private const val DNS_HOST = "all.api.radio-browser.info"
        private const val FALLBACK_HOST = "de1.api.radio-browser.info"
    }

    suspend fun popular(limit: Int = 100): List<RadioStation> = requestStations(
        "/json/stations/topclick/$limit?hidebroken=true"
    )

    suspend fun search(
        query: String,
        field: RadioSearchField,
        limit: Int = 100
    ): List<RadioStation> {
        val value = URLEncoder.encode(query.trim(), "UTF-8")
        return requestStations(
            "/json/stations/search?${field.apiName}=$value&hidebroken=true&order=clickcount&reverse=true&limit=$limit"
        )
    }

    suspend fun registerClick(stationUuid: String) = withContext(Dispatchers.IO) {
        request("/json/url/${URLEncoder.encode(stationUuid, "UTF-8")}")
        Unit
    }

    private suspend fun requestStations(path: String): List<RadioStation> = withContext(Dispatchers.IO) {
        val body = request(path) ?: return@withContext emptyList()
        parseStations(body)
    }

    internal fun parseStations(body: String): List<RadioStation> {
        val array = JSONArray(body)
        return buildList {
            for (index in 0 until array.length()) {
                val json = array.optJSONObject(index) ?: continue
                val station = json.toStation()
                if (station.uuid.isNotBlank() && station.name.isNotBlank() && station.streamUrl.isNotBlank()) {
                    add(station)
                }
            }
        }.distinctBy(RadioStation::uuid)
    }

    private fun request(path: String): String? {
        for (host in mirrors()) {
            val connection = (URL("https://$host$path").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connectTimeout = 4_000
                readTimeout = 7_000
            }
            try {
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    return connection.inputStream.bufferedReader().use { it.readText() }
                }
            } catch (_: Exception) {
                // Try the next mirror.
            } finally {
                connection.disconnect()
            }
        }
        return null
    }

    private fun mirrors(): List<String> = try {
        InetAddress.getAllByName(DNS_HOST)
            .map { it.canonicalHostName }
            .filter { it.endsWith(".api.radio-browser.info") }
            .distinct()
            .shuffled()
            .ifEmpty { listOf(FALLBACK_HOST) }
    } catch (_: Exception) {
        listOf(FALLBACK_HOST)
    }

    private fun JSONObject.toStation(): RadioStation {
        val resolved = optString("url_resolved")
        val original = optString("url")
        return RadioStation(
            uuid = optString("stationuuid"),
            name = optString("name").trim(),
            streamUrl = resolved.ifBlank { original },
            fallbackUrl = original.takeUnless { it == resolved }.orEmpty(),
            favicon = optString("favicon"),
            countryCode = optString("countrycode"),
            country = optString("country"),
            language = optString("language"),
            tags = optString("tags"),
            codec = optString("codec"),
            bitrate = optInt("bitrate"),
            isHls = optInt("hls") == 1,
            isWorking = optInt("lastcheckok") == 1
        )
    }
}
