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

    private fun sanitizeLine(text: String): String =
        text.replace("\r", "").replace("\n", " ").trim()

    /**
     * Checks if a path or URI attempts to access internal app private directories or path traversal.
     */
    fun isSafePathOrUri(uriOrPath: String): Boolean {
        val lower = uriOrPath.lowercase()
        if (lower.contains("/data/data/") || lower.startsWith("file:///data/") || lower.startsWith("/data/")) {
            return false
        }
        if (lower.contains("../") || lower.contains("..\\")) {
            return false
        }
        return true
    }

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
                    // Support both integer and decimal durations (e.g. 203.45)
                    currentDuration = durationStr.toDoubleOrNull()?.toLong() ?: -1L
                    val metadata = info.substring(commaIndex + 1).trim()
                    if (metadata.contains(" - ")) {
                        val parts = metadata.split(" - ", limit = 2)
                        currentArtist = sanitizeLine(parts[0])
                        currentTitle = sanitizeLine(parts[1])
                    } else {
                        currentTitle = sanitizeLine(metadata)
                        currentArtist = ""
                    }
                }
            } else if (!line.startsWith("#")) {
                // Security check: Ignore prohibited private app paths or directory traversal
                if (!isSafePathOrUri(line)) {
                    continue
                }

                // Audio path or content URI
                val entryTitle = currentTitle.ifBlank {
                    line.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.')
                }
                entries.add(
                    M3uEntry(
                        title = sanitizeLine(entryTitle),
                        artist = sanitizeLine(currentArtist),
                        durationSeconds = currentDuration,
                        uriOrPath = sanitizeLine(line)
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
     * Generates a valid Extended M3U playlist file content string with strict CRLF sanitization.
     */
    fun generateM3u(playlistName: String, items: List<PlaylistItem>): String {
        val sb = StringBuilder()
        sb.appendLine("#EXTM3U")
        sb.appendLine("#PLAYLIST:${sanitizeLine(playlistName)}")
        sb.appendLine()

        for (item in items) {
            val durationSec = (item.duration / 1000).coerceAtLeast(-1)
            val title = sanitizeLine(item.title)
            val artist = sanitizeLine(item.artist)
            val artistTitle = if (artist.isNotBlank()) "$artist - $title" else title
            sb.appendLine("#EXTINF:$durationSec,$artistTitle")
            sb.appendLine(sanitizeLine(item.songUri))
        }
        return sb.toString()
    }
}
