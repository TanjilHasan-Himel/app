package com.eplaytime.app.data.model

data class LyricLine(
    val timestampMs: Long,
    val content: String
)

data class ParsedLyrics(
    val lines: List<LyricLine>,
    val isSynced: Boolean,
    val plainText: String? = null
)

