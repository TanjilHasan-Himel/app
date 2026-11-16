package com.eplaytime.app.util

import com.eplaytime.app.data.model.PlaylistItem

/**
 * M3uHelper - Standardized M3U / M3U8 Playlist Parser and Generator
 * Complies with Extended M3U standards for importing and exporting playlists.
 */
object M3uHelper {

    data class M3uEntry(
        val title: String,
        val artist: String = "",
        val durationSeconds: Long = -1,
        val uriOrPath: String
    )

    /**
     * Parses an M3U or M3U8 string into structured M3uEntry objects.
     */
    fun parseM3u(content: String): List<M3uEntry> {
        val lines = content.lines()
        val entries = mutableListOf<M3uEntry>()
        var currentDuration: Long = -1
        var currentTitle = ""
        var currentArtist = ""

        for (rawLine in lines) {
            val line = rawLine.trim().removePrefix("\uFEFF") // Remove BOM if present
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                val info = line.substring(8).trim()
                val commaIndex = info.indexOf(',')
                if (commaIndex != -1) {
                    val durationStr = info.substring(0, commaIndex).trim()
                    currentDuration = durationStr.toLongOrNull() ?: -1L
                    val metadata = info.substring(commaIndex + 1).trim()
                    if (metadata.contains(" - ")) {
                        val parts = metadata.split(" - ", limit = 2)
                        currentArtist = parts[0].trim()
                        currentTitle = parts[1].trim()
                    } else {
                        currentTitle = metadata
                        currentArtist = ""
                    }
                }
            } else if (!line.startsWith("#")) {
                // Audio path or content URI
                val entryTitle = currentTitle.ifBlank {
                    line.substringAfterLast('/').substringBeforeLast('.')
                }
                entries.add(
                    M3uEntry(
                        title = entryTitle,
                        artist = currentArtist,
                        durationSeconds = currentDuration,
                        uriOrPath = line
                    )
                )
                // Reset temporary fields
                currentDuration = -1
                currentTitle = ""
                currentArtist = ""
            }
        }
        return entries
    }

    /**
     * Generates a valid Extended M3U playlist file content string.
     */
    fun generateM3u(playlistName: String, items: List<PlaylistItem>): String {
        val sb = StringBuilder()
        sb.appendLine("#EXTM3U")
        sb.appendLine("#PLAYLIST:$playlistName")
        sb.appendLine()

        for (item in items) {
            val durationSec = (item.duration / 1000).coerceAtLeast(-1)
            val artistTitle = if (item.artist.isNotBlank()) "${item.artist} - ${item.title}" else item.title
            sb.appendLine("#EXTINF:$durationSec,$artistTitle")
            sb.appendLine(item.songUri)
        }
        return sb.toString()
    }
}
