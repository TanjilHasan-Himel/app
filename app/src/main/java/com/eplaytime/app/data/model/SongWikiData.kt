package com.eplaytime.app.data.model

data class SongWikiData(
    val title: String,
    val artist: String,
    val summary: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val composer: String? = null,
    val lyricist: String? = null,
    val producer: String? = null,
    val releaseDate: String? = null,
    val sourceUrl: String? = null
)

