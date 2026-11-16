package com.eplaytime.app.data.model

data class RadioStation(
    val uuid: String,
    val name: String,
    val streamUrl: String,
    val fallbackUrl: String,
    val favicon: String,
    val countryCode: String,
    val country: String,
    val language: String,
    val tags: String,
    val codec: String,
    val bitrate: Int,
    val isHls: Boolean,
    val isWorking: Boolean
)

enum class RadioSearchField(val apiName: String, val label: String) {
    STATION("name", "Station"),
    GENRE("tag", "Genre"),
    COUNTRY("countrycode", "Country"),
    LANGUAGE("language", "Language")
}
