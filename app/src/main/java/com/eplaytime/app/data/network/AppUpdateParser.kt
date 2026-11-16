package com.eplaytime.app.data.network

import android.util.Log
import com.eplaytime.app.data.model.AppUpdateInfo
import org.json.JSONObject
import java.net.URI

/**
 * Pure parsing, semantic version comparison, and URL validation engine for Audia updates.
 * Independent of Android Context for deterministic, zero-mock testing.
 */
object AppUpdateParser {

    private const val TAG = "AppUpdateParser"
    const val PERMANENT_DOWNLOAD_URL =
        "https://github.com/TanjilHasan-Himel/app/releases/latest/download/Audia-latest.apk"

    private val TRUSTED_DOWNLOAD_HOSTS = setOf(
        "github.com",
        "raw.githubusercontent.com",
        "objects.githubusercontent.com",
        "api.github.com"
    )

    /**
     * Validates that the provided update URL strictly uses HTTPS and originates from a trusted GitHub host.
     */
    fun isTrustedUpdateUrl(url: String): Boolean {
        if (url.isBlank()) return false
        return try {
            val uri = URI(url.trim())
            if (!uri.scheme.equals("https", ignoreCase = true)) return false
            val host = uri.host?.lowercase() ?: return false
            TRUSTED_DOWNLOAD_HOSTS.contains(host) || host.endsWith(".githubusercontent.com")
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Sanitize destination file name preventing directory traversal.
     */
    fun sanitizeVersionFilename(versionName: String): String {
        val truncated = versionName.takeWhile { it != '\u0000' && it != ';' && it != '&' && it != '|' }
        val noPath = truncated.replace("..", "").replace("/", "").replace("\\", "")
        val safe = noPath.filter { it.isLetterOrDigit() || it == '.' || it == '-' || it == '_' }
            .trim()
            .trim('.')
            .take(32)
        return if (safe.isBlank()) "latest" else safe
    }

    fun parseVersionJson(
        jsonString: String,
        currentVersionCode: Int,
        currentVersionName: String
    ): AppUpdateInfo? {
        if (jsonString.isBlank()) return null
        return try {
            val json = JSONObject(jsonString)
            val remoteVersionCode = json.optInt("versionCode", 0)
            val remoteVersionName = json.optString("versionName", "").ifBlank { "v$remoteVersionCode" }
            val releaseTitle = json.optString("releaseTitle", "New Audia Version Available")
            val releaseNotes = json.optString("releaseNotes", "")
            val rawDownloadUrl = json.optString("downloadUrl", PERMANENT_DOWNLOAD_URL)
            val downloadUrl = if (isTrustedUpdateUrl(rawDownloadUrl)) rawDownloadUrl else PERMANENT_DOWNLOAD_URL
            val releaseUrl = json.optString("releaseUrl", "")

            val isAvailable = remoteVersionCode > currentVersionCode ||
                    (remoteVersionCode == 0 && isVersionHigher(remoteVersionName, currentVersionName))

            AppUpdateInfo(
                latestVersionName = remoteVersionName,
                latestVersionCode = remoteVersionCode,
                releaseTitle = releaseTitle,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                releasePageUrl = releaseUrl,
                isUpdateAvailable = isAvailable
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing version.json", e)
            null
        }
    }

    fun parseGitHubReleaseJson(
        jsonString: String,
        currentVersionName: String
    ): AppUpdateInfo? {
        if (jsonString.isBlank()) return null
        return try {
            val json = JSONObject(jsonString)
            val tagName = json.optString("tag_name", "")
            val releaseTitle = json.optString("name", tagName)
            val releaseNotes = json.optString("body", "")
            val htmlUrl = json.optString("html_url", "")

            var downloadUrl = PERMANENT_DOWNLOAD_URL
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        val candidateUrl = asset.optString("browser_download_url", "")
                        if (isTrustedUpdateUrl(candidateUrl)) {
                            downloadUrl = candidateUrl
                            break
                        }
                    }
                }
            }

            val cleanTag = tagName.trim().removePrefix("v").removePrefix("V").trim()
            val isAvailable = isVersionHigher(cleanTag, currentVersionName)

            AppUpdateInfo(
                latestVersionName = tagName,
                latestVersionCode = 0,
                releaseTitle = releaseTitle,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                releasePageUrl = htmlUrl,
                isUpdateAvailable = isAvailable
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing GitHub release JSON", e)
            null
        }
    }

    fun isVersionHigher(remote: String, local: String): Boolean {
        val cleanRemote = remote.trim().removePrefix("v").removePrefix("V").trim()
        val cleanLocal = local.trim().removePrefix("v").removePrefix("V").trim()

        if (cleanRemote.isBlank() || cleanLocal.isBlank()) return false
        if (!cleanRemote.any { it.isDigit() } || !cleanLocal.any { it.isDigit() }) return false

        // Extract numeric segments before any prerelease/hotfix suffixes (-beta, -rc1, -hotfix, +build)
        val remoteParts = cleanRemote.split(".").map { segment ->
            segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }
        val localParts = cleanLocal.split(".").map { segment ->
            segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }

        // If both failed to yield numeric parts, they cannot be meaningfully compared
        if (remoteParts.all { it == 0 } && localParts.all { it == 0 } && cleanRemote != cleanLocal) {
            return false
        }

        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
