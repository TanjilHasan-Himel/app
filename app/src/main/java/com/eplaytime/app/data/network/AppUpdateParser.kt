package com.eplaytime.app.data.network

import android.util.Log
import com.eplaytime.app.data.model.AppUpdateInfo
import org.json.JSONObject

/**
 * Pure parsing and semantic version comparison engine for Audia updates.
 * Independent of Android Context for deterministic, zero-mock testing.
 */
object AppUpdateParser {

    private const val TAG = "AppUpdateParser"
    const val PERMANENT_DOWNLOAD_URL =
        "https://github.com/TanjilHasan-Himel/app/releases/latest/download/Audia-latest.apk"

    fun parseVersionJson(
        jsonString: String,
        currentVersionCode: Int,
        currentVersionName: String
    ): AppUpdateInfo? {
        return try {
            val json = JSONObject(jsonString)
            val remoteVersionCode = json.optInt("versionCode", 0)
            val remoteVersionName = json.optString("versionName", "").ifBlank { "v$remoteVersionCode" }
            val releaseTitle = json.optString("releaseTitle", "New Audia Version Available")
            val releaseNotes = json.optString("releaseNotes", "")
            val downloadUrl = json.optString("downloadUrl", PERMANENT_DOWNLOAD_URL)
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
                        downloadUrl = asset.optString("browser_download_url", downloadUrl)
                        break
                    }
                }
            }

            val cleanTag = tagName.removePrefix("v").trim()
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
        val cleanRemote = remote.removePrefix("v").trim()
        val cleanLocal = local.removePrefix("v").trim()
        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = cleanLocal.split(".").mapNotNull { it.toIntOrNull() }

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
