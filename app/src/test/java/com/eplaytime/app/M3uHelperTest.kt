package com.eplaytime.app

import com.eplaytime.app.data.model.PlaylistItem
import com.eplaytime.app.util.M3uHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uHelperTest {

    // =========================================================================
    // 1. Standard Parsing & Line Ending Tests
    // =========================================================================

    @Test
    fun parseM3u_standardExtendedFormat_returnsParsedEntries() {
        val content = """
            #EXTM3U
            #PLAYLIST:Workout Hits
            
            #EXTINF:203,Dua Lipa - Levitating
            content://media/external/audio/media/101
            
            #EXTINF:195,The Weeknd - Blinding Lights
            /storage/emulated/0/Music/blinding_lights.mp3
        """.trimIndent()

        val entries = M3uHelper.parseM3u(content)

        assertEquals(2, entries.size)
        assertEquals("Levitating", entries[0].title)
        assertEquals("Dua Lipa", entries[0].artist)
        assertEquals(203L, entries[0].durationSeconds)
        assertEquals("content://media/external/audio/media/101", entries[0].uriOrPath)

        assertEquals("Blinding Lights", entries[1].title)
        assertEquals("The Weeknd", entries[1].artist)
        assertEquals(195L, entries[1].durationSeconds)
        assertEquals("/storage/emulated/0/Music/blinding_lights.mp3", entries[1].uriOrPath)
    }

    @Test
    fun parseM3u_handlesWindowsCrlfAndMixedLineEndings() {
        val contentWithCrlf = "#EXTM3U\r\n#EXTINF:120,Artist - CRLF Song\r\n/music/crlf.mp3\r\n#EXTINF:150,Artist - LF Song\n/music/lf.mp3\n"

        val entries = M3uHelper.parseM3u(contentWithCrlf)

        assertEquals(2, entries.size)
        assertEquals("CRLF Song", entries[0].title)
        assertEquals("/music/crlf.mp3", entries[0].uriOrPath)
        assertEquals("LF Song", entries[1].title)
        assertEquals("/music/lf.mp3", entries[1].uriOrPath)
    }

    // =========================================================================
    // 2. Empty, Header-Only & Whitespace Tests
    // =========================================================================

    @Test
    fun parseM3u_emptyOrWhitespaceContent_returnsEmptyList() {
        assertTrue(M3uHelper.parseM3u("").isEmpty())
        assertTrue(M3uHelper.parseM3u("   \n\t  \r\n   ").isEmpty())
    }

    @Test
    fun parseM3u_headerOnlyWithoutEntries_returnsEmptyList() {
        val content = """
            #EXTM3U
            #PLAYLIST:Empty Playlist
            #EXT-X-VERSION:3
        """.trimIndent()

        val entries = M3uHelper.parseM3u(content)
        assertTrue(entries.isEmpty())
    }

    // =========================================================================
    // 3. UTF-8 BOM, Unicode, Multilingual & Emojis
    // =========================================================================

    @Test
    fun parseM3u_handlesUtf8BomAtFileStart() {
        val content = "\uFEFF#EXTM3U\n#EXTINF:180,Artist - Song With BOM\n/storage/bom_song.mp3"

        val entries = M3uHelper.parseM3u(content)
        assertEquals(1, entries.size)
        assertEquals("Song With BOM", entries[0].title)
        assertEquals("Artist", entries[0].artist)
    }

    @Test
    fun parseM3u_handlesMultilingualNonAsciiAndAccentedCharacters() {
        val content = """
            #EXTM3U
            #EXTINF:240,শিল্পী - বাংলা গান
            content://media/external/audio/media/201
            #EXTINF:190,宇多田ヒカル - First Love
            content://media/external/audio/media/202
            #EXTINF:210,Mötley Crüe - Kickstart My Heart
            content://media/external/audio/media/203
            #EXTINF:185,Camila Cabello - Señorita
            content://media/external/audio/media/204
        """.trimIndent()

        val entries = M3uHelper.parseM3u(content)

        assertEquals(4, entries.size)
        assertEquals("বাংলা গান", entries[0].title)
        assertEquals("শিল্পী", entries[0].artist)

        assertEquals("First Love", entries[1].title)
        assertEquals("宇多田ヒカル", entries[1].artist)

        assertEquals("Kickstart My Heart", entries[2].title)
        assertEquals("Mötley Crüe", entries[2].artist)

        assertEquals("Señorita", entries[3].title)
        assertEquals("Camila Cabello", entries[3].artist)
    }

    @Test
    fun parseM3u_handlesEmojisInTitleAndArtist() {
        val content = """
            #EXTM3U
            #EXTINF:160,🎧 DJ Beats - 🔥 Summer Drop
            /storage/music/drop.mp3
        """.trimIndent()

        val entries = M3uHelper.parseM3u(content)
        assertEquals(1, entries.size)
        assertEquals("🔥 Summer Drop", entries[0].title)
        assertEquals("🎧 DJ Beats", entries[0].artist)
    }

    // =========================================================================
    // 4. Security Path Traversal & Safe Path Filtering
    // =========================================================================

    @Test
    fun isSafePathOrUri_filtersPrivatePathsAndTraversal() {
        assertTrue(M3uHelper.isSafePathOrUri("content://media/external/audio/media/100"))
        assertTrue(M3uHelper.isSafePathOrUri("/storage/emulated/0/Music/song.mp3"))
        assertTrue(M3uHelper.isSafePathOrUri("https://radio.stream/live"))

        // Private directory attacks
        assertFalse(M3uHelper.isSafePathOrUri("file:///data/data/com.eplaytime.app/databases/playtime.db"))
        assertFalse(M3uHelper.isSafePathOrUri("/data/data/com.eplaytime.app/shared_prefs/playtime.xml"))
        assertFalse(M3uHelper.isSafePathOrUri("/storage/emulated/0/Music/../../data/system"))
    }

    // =========================================================================
    // 5. URI, Decimal Durations & Fallback Titles
    // =========================================================================

    @Test
    fun parseM3u_supportsDecimalDurations() {
        val content = """
            #EXTM3U
            #EXTINF:203.45,Dua Lipa - Levitating
            content://media/external/audio/media/101
        """.trimIndent()

        val entries = M3uHelper.parseM3u(content)
        assertEquals(1, entries.size)
        assertEquals(203L, entries[0].durationSeconds)
    }

    @Test
    fun parseM3u_handlesWindowsBackslashPathsInFallbackTitle() {
        val content = """
            #EXTM3U
            C:\Users\Music\FavoriteSong.mp3
        """.trimIndent()

        val entries = M3uHelper.parseM3u(content)
        assertEquals(1, entries.size)
        assertEquals("FavoriteSong", entries[0].title)
    }

    // =========================================================================
    // 6. Round-Trip Serialization & CRLF Sanitization
    // =========================================================================

    @Test
    fun generateM3u_sanitizesCrlfInjection() {
        val items = listOf(
            PlaylistItem(
                id = 1,
                playlistId = 10,
                songId = 501,
                songUri = "content://media/external/audio/media/501\r\n#MALICIOUS:DIRECTIVE",
                title = "Injected\r\nTitle",
                artist = "Malicious\nArtist",
                album = "Album",
                duration = 200000L,
                position = 0
            )
        )

        val exported = M3uHelper.generateM3u("Playlist\r\nName", items)

        assertFalse(exported.contains("\r"))
        // Directives must not have injected newlines breaking line structure
        val lines = exported.lines().filter { it.isNotBlank() }
        assertEquals(4, lines.size) // #EXTM3U, #PLAYLIST, #EXTINF, songUri
        assertTrue(lines[1].startsWith("#PLAYLIST:Playlist Name"))
        assertTrue(lines[2].startsWith("#EXTINF:200,Malicious Artist - Injected Title"))
    }
}
