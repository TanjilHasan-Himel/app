package com.eplaytime.app

import com.eplaytime.app.data.network.AppUpdateParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    // =========================================================================
    // 1. Semantic Version Comparison Boundary Tests
    // =========================================================================

    @Test
    fun isVersionHigher_correctlyComparesStandardSemanticVersions() {
        assertTrue(AppUpdateParser.isVersionHigher("9.1.2", "9.1.1"))
        assertTrue(AppUpdateParser.isVersionHigher("9.2.0", "9.1.1"))
        assertTrue(AppUpdateParser.isVersionHigher("10.0.0", "9.1.1"))

        assertFalse(AppUpdateParser.isVersionHigher("9.1.1", "9.1.1"))
        assertFalse(AppUpdateParser.isVersionHigher("9.1.0", "9.1.1"))
        assertFalse(AppUpdateParser.isVersionHigher("8.9.9", "9.1.1"))
    }

    @Test
    fun isVersionHigher_handlesUpperAndLowerVPrefixAndWhitespace() {
        assertTrue(AppUpdateParser.isVersionHigher("v9.1.2", "9.1.1"))
        assertTrue(AppUpdateParser.isVersionHigher("9.1.2", "v9.1.1"))
        assertTrue(AppUpdateParser.isVersionHigher("v9.1.2", "v9.1.1"))
        assertTrue(AppUpdateParser.isVersionHigher("  v9.1.2  ", "  9.1.1  "))
        assertTrue(AppUpdateParser.isVersionHigher("V9.1.2", "9.1.1"))

        assertFalse(AppUpdateParser.isVersionHigher("v9.1.1", "9.1.1"))
        assertFalse(AppUpdateParser.isVersionHigher("V9.1.1", "9.1.1"))
    }

    @Test
    fun isVersionHigher_handlesMultiDigitAndDoubleDigitSegments() {
        // Critical: Lexicographical comparison would fail ("9.10" < "9.9")
        assertTrue(AppUpdateParser.isVersionHigher("9.10.0", "9.9.0"))
        assertTrue(AppUpdateParser.isVersionHigher("9.2.11", "9.2.2"))
        assertTrue(AppUpdateParser.isVersionHigher("10.0.0", "9.99.99"))
        assertTrue(AppUpdateParser.isVersionHigher("100.0.0", "99.99.99"))

        assertFalse(AppUpdateParser.isVersionHigher("9.9.0", "9.10.0"))
        assertFalse(AppUpdateParser.isVersionHigher("9.2.2", "9.2.11"))
    }

    @Test
    fun isVersionHigher_handlesUnequalSegmentLengthsAndPatchVersions() {
        // 4 segments vs 3 segments
        assertTrue(AppUpdateParser.isVersionHigher("9.1.1.1", "9.1.1"))
        assertFalse(AppUpdateParser.isVersionHigher("9.1.1", "9.1.1.1"))

        // 2 segments vs 3 segments
        assertTrue(AppUpdateParser.isVersionHigher("9.2", "9.1.9"))
        assertFalse(AppUpdateParser.isVersionHigher("9.1", "9.1.1"))

        // Trailing zeros equivalence
        assertFalse(AppUpdateParser.isVersionHigher("9.1.0", "9.1"))
        assertFalse(AppUpdateParser.isVersionHigher("9.1", "9.1.0"))
        assertTrue(AppUpdateParser.isVersionHigher("9.1.0.1", "9.1.0"))
    }

    @Test
    fun isVersionHigher_handlesPrereleaseAndSuffixes() {
        assertTrue(AppUpdateParser.isVersionHigher("9.2.0-rc1", "9.1.0"))
        assertTrue(AppUpdateParser.isVersionHigher("9.2.0-beta.2", "9.1.9"))
        assertTrue(AppUpdateParser.isVersionHigher("9.1.2-hotfix", "9.1.1"))

        assertFalse(AppUpdateParser.isVersionHigher("", "9.1.1"))
        assertFalse(AppUpdateParser.isVersionHigher("invalid", "9.1.1"))
        assertFalse(AppUpdateParser.isVersionHigher("9.1.1", "invalid"))
        assertFalse(AppUpdateParser.isVersionHigher("", ""))
    }

    // =========================================================================
    // 2. Security URL & Filename Validation Tests
    // =========================================================================

    @Test
    fun isTrustedUpdateUrl_validatesHttpsAndGithubHosts() {
        assertTrue(AppUpdateParser.isTrustedUpdateUrl("https://github.com/TanjilHasan-Himel/app/releases/download/v9.1.1/Audia-v9.1.1.apk"))
        assertTrue(AppUpdateParser.isTrustedUpdateUrl("https://raw.githubusercontent.com/TanjilHasan-Himel/app/main/version.json"))
        assertTrue(AppUpdateParser.isTrustedUpdateUrl("https://objects.githubusercontent.com/github-production-release-asset-2e65be/Audia.apk"))
        assertTrue(AppUpdateParser.isTrustedUpdateUrl("https://api.github.com/repos/TanjilHasan-Himel/app/releases/latest"))

        // Reject cleartext HTTP
        assertFalse(AppUpdateParser.isTrustedUpdateUrl("http://github.com/app/release.apk"))
        // Reject non-GitHub untrusted hosts
        assertFalse(AppUpdateParser.isTrustedUpdateUrl("https://malicious-mirror.org/Audia.apk"))
        assertFalse(AppUpdateParser.isTrustedUpdateUrl("https://evil.github.com.attacker.com/Audia.apk"))
        // Reject invalid schemes
        assertFalse(AppUpdateParser.isTrustedUpdateUrl("content://media/external/file"))
        assertFalse(AppUpdateParser.isTrustedUpdateUrl("file:///sdcard/Audia.apk"))
        assertFalse(AppUpdateParser.isTrustedUpdateUrl(""))
    }

    @Test
    fun sanitizeVersionFilename_stripsDangerousCharacters() {
        assertEquals("9.1.1", AppUpdateParser.sanitizeVersionFilename("9.1.1"))
        assertEquals("v9.1.1-beta_2", AppUpdateParser.sanitizeVersionFilename("v9.1.1-beta_2"))
        assertEquals("9.1.1", AppUpdateParser.sanitizeVersionFilename("../../../9.1.1"))
        assertEquals("9.1.1", AppUpdateParser.sanitizeVersionFilename("9.1.1\u0000; rm -rf /"))
        assertEquals("latest", AppUpdateParser.sanitizeVersionFilename("   "))
    }

    // =========================================================================
    // 3. version.json Parsing & Missing Field Tests
    // =========================================================================

    @Test
    fun parseVersionJson_happyPath_parsesAllFieldsCorrectly() {
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
        assertEquals("https://github.com/TanjilHasan-Himel/app/releases/tag/v9.2.0", info?.releasePageUrl)
        assertTrue(info?.isUpdateAvailable == true)
    }

    @Test
    fun parseVersionJson_malformedJson_returnsNullSafely() {
        assertNull(AppUpdateParser.parseVersionJson("", currentVersionCode = 91, currentVersionName = "9.1.0"))
        assertNull(AppUpdateParser.parseVersionJson("not valid json", currentVersionCode = 91, currentVersionName = "9.1.0"))
        assertNull(AppUpdateParser.parseVersionJson("{ unclosed json: 123", currentVersionCode = 91, currentVersionName = "9.1.0"))
        assertNull(AppUpdateParser.parseVersionJson("[]", currentVersionCode = 91, currentVersionName = "9.1.0"))
    }

    @Test
    fun parseVersionJson_missingOptionalFields_usesSafeDefaults() {
        val json = "{}"

        val info = AppUpdateParser.parseVersionJson(json, currentVersionCode = 91, currentVersionName = "9.1.0")
        assertNotNull(info)
        assertEquals("v0", info?.latestVersionName)
        assertEquals(0, info?.latestVersionCode)
        assertEquals("New Audia Version Available", info?.releaseTitle)
        assertEquals("", info?.releaseNotes)
        assertEquals(AppUpdateParser.PERMANENT_DOWNLOAD_URL, info?.downloadUrl)
        assertEquals("", info?.releasePageUrl)
        assertFalse(info?.isUpdateAvailable == true)
    }

    @Test
    fun parseVersionJson_untrustedUrl_fallsBackToPermanentUrl() {
        val json = """
            {
              "versionCode": 99,
              "versionName": "9.2.0",
              "downloadUrl": "http://insecure-attacker.com/malicious.apk"
            }
        """.trimIndent()

        val info = AppUpdateParser.parseVersionJson(json, currentVersionCode = 91, currentVersionName = "9.1.0")
        assertNotNull(info)
        // Untrusted HTTP URL must be sanitized back to PERMANENT_DOWNLOAD_URL
        assertEquals(AppUpdateParser.PERMANENT_DOWNLOAD_URL, info?.downloadUrl)
        assertTrue(info?.isUpdateAvailable == true)
    }

    // =========================================================================
    // 4. GitHub Releases API Parsing Tests
    // =========================================================================

    @Test
    fun parseGitHubReleaseJson_happyPath_parsesCorrectly() {
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
        assertEquals("https://github.com/TanjilHasan-Himel/app/releases/tag/v9.9.0", info?.releasePageUrl)
        assertTrue(info?.isUpdateAvailable == true)
    }

    @Test
    fun parseGitHubReleaseJson_malformedJson_returnsNullSafely() {
        assertNull(AppUpdateParser.parseGitHubReleaseJson("", currentVersionName = "9.1.0"))
        assertNull(AppUpdateParser.parseGitHubReleaseJson("invalid", currentVersionName = "9.1.0"))
        assertNull(AppUpdateParser.parseGitHubReleaseJson("{ unclosed: true", currentVersionName = "9.1.0"))
    }

    @Test
    fun parseGitHubReleaseJson_nonApkAssets_fallsBackToPermanentUrl() {
        val jsonNonApkAssets = """
            {
              "tag_name": "v9.5.0",
              "name": "Audia v9.5.0",
              "assets": [
                {
                  "name": "source-code.tar.gz",
                  "browser_download_url": "https://github.com/source.tar.gz"
                }
              ]
            }
        """.trimIndent()

        val info = AppUpdateParser.parseGitHubReleaseJson(jsonNonApkAssets, currentVersionName = "9.1.0")
        assertNotNull(info)
        assertEquals(AppUpdateParser.PERMANENT_DOWNLOAD_URL, info?.downloadUrl)
    }
}
