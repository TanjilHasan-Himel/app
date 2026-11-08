package com.eplaytime.app.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.eplaytime.app.data.model.Song
import java.io.File
import java.text.DecimalFormat

data class AudioTechSpecs(
    val format: String,
    val bitrateKbps: String,
    val sampleRateHz: String,
    val channels: String,
    val fileSizeFormatted: String,
    val filePath: String,
    val durationFormatted: String
)

object AudioInspectorHelper {

    fun inspect(context: Context, song: Song): AudioTechSpecs {
        var format = "Audio Track"
        var bitrate = "Unknown"
        var sampleRate = "44,100 Hz"
        var channels = "Stereo (2 ch)"
        var fileSize = "Unknown"

        try {
            val retriever = MediaMetadataRetriever()
            if (song.folderPath.isNotBlank() && File(song.folderPath).exists()) {
                retriever.setDataSource(song.folderPath)
                val file = File(song.folderPath)
                fileSize = formatFileSize(file.length())
                format = file.extension.uppercase().ifBlank { "MP3" }
            } else {
                retriever.setDataSource(context, Uri.parse(song.uri))
            }

            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            if (!mime.isNullOrBlank()) {
                format = when {
                    mime.contains("flac", ignoreCase = true) -> "FLAC (Lossless)"
                    mime.contains("wav", ignoreCase = true) -> "WAV (Uncompressed)"
                    mime.contains("mp4", ignoreCase = true) || mime.contains("m4a", ignoreCase = true) || mime.contains("aac", ignoreCase = true) -> "AAC / M4A"
                    mime.contains("ogg", ignoreCase = true) || mime.contains("opus", ignoreCase = true) -> "OGG / Opus"
                    else -> "MPEG Audio (MP3)"
                }
            }

            val br = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
            if (br != null && br > 0) {
                bitrate = "${br / 1000} kbps"
            }

            val sr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()
            if (sr != null && sr > 0) {
                sampleRate = "${DecimalFormat("#,###").format(sr)} Hz"
            }

            retriever.release()
        } catch (_: Exception) {}

        val durationFormatted = formatTime(song.duration)

        return AudioTechSpecs(
            format = format,
            bitrateKbps = bitrate,
            sampleRateHz = sampleRate,
            channels = channels,
            fileSizeFormatted = fileSize,
            filePath = song.folderPath.ifBlank { song.uri },
            durationFormatted = durationFormatted
        )
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "Unknown"
        val mb = bytes / (1024.0 * 1024.0)
        return "%.2f MB".format(mb)
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%02d:%02d".format(min, sec)
    }
}

