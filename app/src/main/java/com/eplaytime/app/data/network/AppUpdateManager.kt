package com.eplaytime.app.data.network

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.eplaytime.app.BuildConfig
import com.eplaytime.app.data.datastore.PlayTimeDataStore
import com.eplaytime.app.data.model.AppUpdateInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: PlayTimeDataStore
) {

    companion object {
        private const val TAG = "AppUpdateManager"
        private const val USER_AGENT = "AudiaPlayer/${BuildConfig.VERSION_NAME} (Android)"
        
        // Fastly CDN URL (Zero API rate limiting, instant response worldwide)
        private const val RAW_VERSION_URL =
            "https://raw.githubusercontent.com/TanjilHasan-Himel/app/main/version.json"
            
        // Official GitHub Releases fallback
        private const val GITHUB_RELEASES_API =
            "https://api.github.com/repos/TanjilHasan-Himel/app/releases/latest"

        // Permanent direct download link for the latest APK
        const val PERMANENT_DOWNLOAD_URL = AppUpdateParser.PERMANENT_DOWNLOAD_URL

        // Check throttle: 24 hours (1 day)
        const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L

        // Reminder throttle: 48 hours (2 days) if dismissed
        const val REMINDER_INTERVAL_MS = 48 * 60 * 60 * 1000L
    }

    /**
     * Checks whether an update is available.
     * @param force If true, bypasses the 24-hour check interval and 48-hour reminder interval (e.g. from Settings).
     * @return Result containing [AppUpdateInfo] or null if up to date or throttled.
     */
    suspend fun checkForUpdates(force: Boolean = false): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()

            if (!force) {
                val lastReminder = dataStore.getLastUpdateReminderTime()
                if (now - lastReminder < REMINDER_INTERVAL_MS) {
                    Log.d(TAG, "Update reminder suppressed: dismissed less than 48 hours ago")
                    return@withContext Result.success(null)
                }

                val lastCheck = dataStore.getLastUpdateCheckTime()
                if (now - lastCheck < CHECK_INTERVAL_MS) {
                    Log.d(TAG, "Update check skipped: checked less than 24 hours ago")
                    return@withContext Result.success(null)
                }
            }

            // 1. Try Fastly CDN (version.json) first for speed and zero rate limits
            var fetchSucceeded = false
            var updateInfo: AppUpdateInfo? = null

            val rawResult = fetchFromRawVersionJson()
            if (rawResult != null) {
                fetchSucceeded = true
                updateInfo = rawResult
            } else {
                // 2. Fallback to GitHub Releases API if version.json wasn't accessible
                val (success, gitHubResult) = fetchFromGitHubReleasesApi()
                if (success) {
                    fetchSucceeded = true
                    updateInfo = gitHubResult
                }
            }

            if (!fetchSucceeded) {
                // If offline or network unavailable, do NOT stamp lastUpdateCheckTime
                Log.w(TAG, "Update check failed: unable to reach update endpoints")
                return@withContext Result.failure(IOException("Update endpoints unreachable"))
            }

            // Record check timestamp only on verified network success
            dataStore.setLastUpdateCheckTime(now)

            if (updateInfo != null && updateInfo.isUpdateAvailable) {
                Log.i(TAG, "New Audia update found: ${updateInfo.latestVersionName} (Code: ${updateInfo.latestVersionCode})")
                Result.success(updateInfo)
            } else {
                Log.d(TAG, "Audia is up to date: local build is ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates", e)
            Result.failure(e)
        }
    }

    /**
     * Marks the current update as postponed. The user will not be reminded again for 48 hours.
     */
    suspend fun postponeReminder() {
        dataStore.setLastUpdateReminderTime(System.currentTimeMillis())
        Log.d(TAG, "Update reminder postponed for 48 hours")
    }

    /**
     * Initiates downloading of the update APK via Android DownloadManager with notifications.
     */
    fun startDownload(updateInfo: AppUpdateInfo) {
        val rawDownloadUrl = updateInfo.downloadUrl.ifBlank { PERMANENT_DOWNLOAD_URL }
        val downloadUrl = if (AppUpdateParser.isTrustedUpdateUrl(rawDownloadUrl)) {
            rawDownloadUrl
        } else {
            Log.w(TAG, "Untrusted download URL detected ($rawDownloadUrl), reverting to permanent URL")
            PERMANENT_DOWNLOAD_URL
        }

        val safeFileName = "Audia-${AppUpdateParser.sanitizeVersionFilename(updateInfo.latestVersionName)}.apk"

        try {
            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Audia ${updateInfo.latestVersionName}")
                setDescription("Downloading Audia update...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    safeFileName
                )
                setMimeType("application/vnd.android.package-archive")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (manager != null) {
                manager.enqueue(request)
                Toast.makeText(context, "Downloading Audia ${updateInfo.latestVersionName}...", Toast.LENGTH_SHORT).show()
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "DownloadManager failed, falling back to browser intent", e)
        }

        // Fallback: Open in external browser
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open download link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchFromRawVersionJson(): AppUpdateInfo? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(RAW_VERSION_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                AppUpdateParser.parseVersionJson(response, BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.d(TAG, "Raw version.json fetch skipped: ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun fetchFromGitHubReleasesApi(): Pair<Boolean, AppUpdateInfo?> {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(GITHUB_RELEASES_API)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val parsed = AppUpdateParser.parseGitHubReleaseJson(response, BuildConfig.VERSION_NAME)
                Pair(true, parsed)
            } else {
                Pair(false, null)
            }
        } catch (e: Exception) {
            Log.d(TAG, "GitHub Releases API fetch skipped: ${e.message}")
            Pair(false, null)
        } finally {
            connection?.disconnect()
        }
    }
}
