package com.eplaytime.app.util

import com.eplaytime.app.data.model.LyricLine
import com.eplaytime.app.data.model.ParsedLyrics
import java.util.regex.Pattern

object LrcParser {
    // Regex matching timestamp patterns like [01:23.45] or [01:23.456]
    private val TIME_TAG_PATTERN = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?\\]")

    /**
     * Parses a raw LRC string into a structured ParsedLyrics object.
     */
    fun parse(lrcContent: String?): ParsedLyrics {
        if (lrcContent.isNullOrBlank()) {
            return ParsedLyrics(emptyList(), isSynced = false, plainText = null)
        }

        val lines = lrcContent.lines()
        val parsedLines = mutableListOf<LyricLine>()
        var foundTimeTag = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // Skip metadata tags like [ar:Artist], [ti:Title], [al:Album]
            if (trimmed.matches(Regex("^\\[[a-zA-Z]+:.*?\\]$"))) continue

            val matcher = TIME_TAG_PATTERN.matcher(trimmed)
            val lineTimestamps = mutableListOf<Long>()
            var lastMatchEnd = 0

            while (matcher.find()) {
                foundTimeTag = true
                val minutes = matcher.group(1)?.toLongOrNull() ?: 0L
                val seconds = matcher.group(2)?.toLongOrNull() ?: 0L
                val millisString = matcher.group(3)
                val millis = when (millisString?.length) {
                    1 -> (millisString.toLongOrNull() ?: 0L) * 100L
                    2 -> (millisString.toLongOrNull() ?: 0L) * 10L
                    3 -> millisString.toLongOrNull() ?: 0L
                    else -> 0L
                }
                val totalMs = (minutes * 60L + seconds) * 1000L + millis
                lineTimestamps.add(totalMs)
                lastMatchEnd = matcher.end()
            }

            if (lineTimestamps.isNotEmpty()) {
                val content = trimmed.substring(lastMatchEnd).trim()
                for (timestamp in lineTimestamps) {
                    parsedLines.add(LyricLine(timestampMs = timestamp, content = content))
                }
            }
        }

        parsedLines.sortBy { it.timestampMs }

        return if (foundTimeTag && parsedLines.isNotEmpty()) {
            ParsedLyrics(lines = parsedLines, isSynced = true, plainText = lrcContent)
        } else {
            // Unsynced plain text
            val plainLyricLines = lines
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("[") }
                .mapIndexed { index, text -> LyricLine(timestampMs = index * 3000L, content = text) }
            ParsedLyrics(lines = plainLyricLines, isSynced = false, plainText = lrcContent)
        }
    }

    /**
     * Finds the index of the currently active lyric line for a given playback position in milliseconds.
     * Returns -1 if the song is currently in an instrumental prelude before the first lyric starts.
     */
    fun findActiveLyricIndex(lines: List<LyricLine>, currentPositionMs: Long): Int {
        if (lines.isEmpty()) return -1
        if (currentPositionMs < lines.first().timestampMs) return -1

        var low = 0
        var high = lines.size - 1
        var candidate = -1

        while (low <= high) {
            val mid = (low + high) ushr 1
            val midTime = lines[mid].timestampMs
            if (midTime <= currentPositionMs) {
                candidate = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return candidate
    }
}

