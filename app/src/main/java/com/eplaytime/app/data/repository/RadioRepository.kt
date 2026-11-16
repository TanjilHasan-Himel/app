package com.eplaytime.app.data.repository

import android.content.Context
import com.eplaytime.app.data.model.RadioSearchField
import com.eplaytime.app.data.model.RadioStation
import com.eplaytime.app.data.network.RadioBrowserClient
import org.json.JSONArray
import org.json.JSONObject

class RadioRepository(context: Context) {
    private val client = RadioBrowserClient()
    private val preferences = context.getSharedPreferences("audia_radio", Context.MODE_PRIVATE)

    suspend fun popular(): List<RadioStation> = client.popular()
    suspend fun search(query: String, field: RadioSearchField): List<RadioStation> = client.search(query, field)
    suspend fun registerClick(uuid: String) = client.registerClick(uuid)

    fun favorites(): List<RadioStation> = decode(preferences.getString("favorites", null))
    fun recents(): List<RadioStation> = decode(preferences.getString("recents", null))

    fun toggleFavorite(station: RadioStation): List<RadioStation> {
        val updated = favorites().toMutableList().apply {
            val existing = indexOfFirst { it.uuid == station.uuid }
            if (existing >= 0) removeAt(existing) else add(0, station)
        }
        preferences.edit().putString("favorites", encode(updated)).apply()
        return updated
    }

    fun addRecent(station: RadioStation): List<RadioStation> {
        val updated = (listOf(station) + recents().filterNot { it.uuid == station.uuid }).take(30)
        preferences.edit().putString("recents", encode(updated)).apply()
        return updated
    }

    private fun encode(stations: List<RadioStation>): String = JSONArray().apply {
        stations.forEach { station ->
            put(JSONObject().apply {
                put("uuid", station.uuid)
                put("name", station.name)
                put("streamUrl", station.streamUrl)
                put("fallbackUrl", station.fallbackUrl)
                put("favicon", station.favicon)
                put("countryCode", station.countryCode)
                put("country", station.country)
                put("language", station.language)
                put("tags", station.tags)
                put("codec", station.codec)
                put("bitrate", station.bitrate)
                put("hls", station.isHls)
                put("working", station.isWorking)
            })
        }
    }.toString()

    private fun decode(raw: String?): List<RadioStation> = try {
        val array = JSONArray(raw ?: "[]")
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    RadioStation(
                        uuid = item.getString("uuid"),
                        name = item.getString("name"),
                        streamUrl = item.getString("streamUrl"),
                        fallbackUrl = item.optString("fallbackUrl"),
                        favicon = item.optString("favicon"),
                        countryCode = item.optString("countryCode"),
                        country = item.optString("country"),
                        language = item.optString("language"),
                        tags = item.optString("tags"),
                        codec = item.optString("codec"),
                        bitrate = item.optInt("bitrate"),
                        isHls = item.optBoolean("hls"),
                        isWorking = item.optBoolean("working", true)
                    )
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}
