package com.eplaytime.app.data.model

/**
 * Metadata representing a published release of Audia Player.
 */
data class AppUpdateInfo(
    val latestVersionName: String,
    val latestVersionCode: Int,
    val releaseTitle: String = "",
    val releaseNotes: String = "",
    val downloadUrl: String,
    val releasePageUrl: String = "",
    val isUpdateAvailable: Boolean = false
)
