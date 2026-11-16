package com.eplaytime.app.data.network

import kotlin.math.abs

data class LyricsCandidate(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Long,
    val syncedLyrics: String?,
    val plainLyrics: String?,
    val score: Double = 0.0
) {
    val hasLyrics: Boolean get() = !syncedLyrics.isNullOrBlank() || !plainLyrics.isNullOrBlank()
}

internal object LyricsCandidateScorer {
    const val AUTO_ACCEPT_SCORE = 0.78

    fun rank(
        candidates: List<LyricsCandidate>,
        title: String,
        artist: String,
        album: String,
        durationSeconds: Long
    ): List<LyricsCandidate> = candidates
        .asSequence()
        .filter(LyricsCandidate::hasLyrics)
        .map { candidate ->
            candidate.copy(
                score = score(candidate, title, artist, album, durationSeconds)
            )
        }
        .filter { candidate ->
            similarity(candidate.title, title) >= 0.70 &&
                (isUnknown(artist) || similarity(candidate.artist, artist) >= 0.45) &&
                (durationSeconds <= 0L || candidate.durationSeconds <= 0L ||
                    abs(candidate.durationSeconds - durationSeconds) <= 15L)
        }
        .sortedByDescending(LyricsCandidate::score)
        .toList()

    private fun score(
        candidate: LyricsCandidate,
        title: String,
        artist: String,
        album: String,
        durationSeconds: Long
    ): Double {
        val titleScore = similarity(candidate.title, title)
        val artistScore = if (isUnknown(artist)) 1.0 else similarity(candidate.artist, artist)
        val albumScore = if (album.isBlank()) 1.0 else similarity(candidate.album, album)
        val durationScore = when {
            durationSeconds <= 0L || candidate.durationSeconds <= 0L -> 0.5
            else -> (1.0 - abs(candidate.durationSeconds - durationSeconds) / 15.0).coerceIn(0.0, 1.0)
        }
        return titleScore * 0.52 + artistScore * 0.28 + durationScore * 0.15 + albumScore * 0.05
    }

    internal fun similarity(left: String, right: String): Double {
        val a = tokens(left)
        val b = tokens(right)
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a == b) return 1.0
        return a.intersect(b).size.toDouble() / a.union(b).size.toDouble()
    }

    private fun tokens(value: String): Set<String> = value
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
        .toSet()

    private fun isUnknown(value: String): Boolean =
        value.isBlank() || value.equals("<unknown>", ignoreCase = true) ||
            value.equals("unknown artist", ignoreCase = true)
}
