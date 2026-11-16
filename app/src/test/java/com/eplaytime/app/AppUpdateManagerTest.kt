package com.eplaytime.app

import com.eplaytime.app.data.network.AppUpdateParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun isVersionHigher_correctlyComparesSemanticVersions() {
        assertTrue(AppUpdateParser.isVersionHigher("9.1.2", "9.1.1"))
        assertTrue(AppUpdateParser.isVersionHigher("9.2.0", "9.1.1"))
        assertTrue(AppUpdateParser.isVersionHigher("10.0.0", "9.1.1"))
        assertTrue(AppUpdateParser.isVersionHigher("v9.1.2", "9.1.1"))
        assertTrue(AppUpdateParser.isVersionHigher("9.1.1.1", "9.1.1"))

        assertFalse(AppUpdateParser.isVersionHigher("9.1.1", "9.1.1"))
        assertFalse(AppUpdateParser.isVersionHigher("9.1.0", "9.1.1"))
        assertFalse(AppUpdateParser.isVersionHigher("8.9.9", "9.1.1"))
        assertFalse(AppUpdateParser.isVersionHigher("v9.1.1", "9.1.1"))
    }

    @Test
    fun parseVersionJson_parsesCorrectly() {
        val json = """
            {
              "versionCode": 99,
              "versionName": "9.2.0",
              "releaseTitle": "Audia 9.2.0 Major Update",
              "releaseNotes": "• New equalizer presets\n• Performance enhancements",
              "downloadUrl": "https://github.com/TanjilHasan-Himel/app/releases/download/v9.2.0/Audia-v9.2.0.apk",
              "releaseUrl": "https://github.com/TanjilHasan-Himel/app/releases/tag/v9.2.0"
            }
        """.trimIndent()

        val info = AppUpdateParser.parseVersionJson(json, currentVersionCode = 91, currentVersionName = "9.1.0")
        assertNotNull(info)
        assertEquals("9.2.0", info?.latestVersionName)
        assertEquals(99, info?.latestVersionCode)
        assertEquals("Audia 9.2.0 Major Update", info?.releaseTitle)
        assertTrue(info?.releaseNotes?.contains("New equalizer") == true)
        assertEquals("https://github.com/TanjilHasan-Himel/app/releases/download/v9.2.0/Audia-v9.2.0.apk", info?.downloadUrl)
        assertTrue(info?.isUpdateAvailable == true)
    }

    @Test
    fun parseGitHubReleaseJson_parsesCorrectly() {
        val json = """
            {
              "tag_name": "v9.9.0",
              "name": "Audia v9.9.0 Final",
              "body": "Changelog:\n- Feature 1\n- Feature 2",
              "html_url": "https://github.com/TanjilHasan-Himel/app/releases/tag/v9.9.0",
              "assets": [
                {
                  "name": "Audia-v9.9.0.apk",
                  "browser_download_url": "https://github.com/TanjilHasan-Himel/app/releases/download/v9.9.0/Audia-v9.9.0.apk"
                }
              ]
            }
        """.trimIndent()

        val info = AppUpdateParser.parseGitHubReleaseJson(json, currentVersionName = "9.1.0")
        assertNotNull(info)
        assertEquals("v9.9.0", info?.latestVersionName)
        assertEquals("Audia v9.9.0 Final", info?.releaseTitle)
        assertEquals("https://github.com/TanjilHasan-Himel/app/releases/download/v9.9.0/Audia-v9.9.0.apk", info?.downloadUrl)
        assertTrue(info?.isUpdateAvailable == true)
    }
}
