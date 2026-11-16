package com.eplaytime.app.data.repository

import com.eplaytime.app.data.model.Song

/** Pure production filter rules, shared by the repository and unit tests. */
object MusicScanFilter {
    fun apply(
        songs: List<Song>,
        minDurationSec: Int,
        hideCallsAndVoiceNotes: Boolean,
        folderMode: String,
        folders: Set<String>
    ): List<Song> {
        val minDurationMs = minDurationSec.coerceAtLeast(0) * 1_000L
        val normalizedFolders = folders.mapNotNull(::normalizeFolder)

        return songs.filter { song ->
            val longEnough = minDurationMs == 0L || song.duration >= minDurationMs
            val allowedType = !hideCallsAndVoiceNotes || !isCallOrVoiceNote(song)
            val matchesFolder = normalizedFolders.any { folder -> pathIsInside(song.folderPath, folder) }
            val allowedFolder = when {
                normalizedFolders.isEmpty() -> true
                folderMode.equals("INCLUDE", ignoreCase = true) -> matchesFolder
                else -> !matchesFolder
            }
            longEnough && allowedType && allowedFolder
        }
    }

    fun isCallOrVoiceNote(song: Song): Boolean {
        val path = normalizePath(song.folderPath)
        val bucket = song.bucketDisplayName.lowercase()
        val title = song.title.lowercase()

        val callRecording = listOf(
            "/call/", "/call recordings/", "/callrecordings/", "/recordings/call/",
            "/recordings/call recordings/", "/miui/sound_recorder/call_rec/"
        ).any(path::contains) || bucket.contains("call record") ||
            title.startsWith("call_") || title.startsWith("call recording")

        val voiceNote = path.contains("/whatsapp voice notes/") ||
            path.contains("/voice recorder/") || path.contains("/sound recorder/") ||
            bucket.contains("voice note") || bucket.contains("voice recorder") ||
            title.startsWith("ptt-") || title.startsWith("voice_") ||
            title.startsWith("voice ") || title.startsWith("recording_")

        // WhatsApp Audio can contain real songs, so its folder alone is not a voice-note signal.
        return callRecording || voiceNote
    }

    private fun normalizeFolder(value: String): String? = normalizePath(value)
        .trimEnd('/')
        .takeIf { it.isNotBlank() }

    private fun normalizePath(value: String): String = value.lowercase().replace('\\', '/')

    private fun pathIsInside(songPath: String, folder: String): Boolean {
        val normalizedSong = normalizePath(songPath).trimEnd('/')
        return normalizedSong == folder || normalizedSong.startsWith("$folder/")
    }
}
