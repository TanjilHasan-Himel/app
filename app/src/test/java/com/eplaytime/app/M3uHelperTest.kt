package com.eplaytime.app

import com.eplaytime.app.data.model.PlaylistItem
import com.eplaytime.app.util.M3uHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uHelperTest {

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
    fun parseM3u_titleOnlyFormat_returnsFallbackArtist() {
        val content = """
            #EXTM3U
            #EXTINF:180,Solo Piano Melody
            content://media/external/audio/media/102
        """.trimIndent()

        val entries = M3uHelper.parseM3u(content)

        assertEquals(1, entries.size)
        assertEquals("Solo Piano Melody", entries[0].title)
        assertEquals("", entries[0].artist)
        assertEquals(180L, entries[0].durationSeconds)
    }

    @Test
    fun generateM3u_createsValidExtendedM3u() {
        val items = listOf(
            PlaylistItem(
                id = 1,
                playlistId = 10,
                songId = 501,
                songUri = "content://media/external/audio/media/501",
                title = "Starboy",
                artist = "The Weeknd",
                album = "Starboy",
                duration = 230000L,
                position = 0
            ),
            PlaylistItem(
                id = 2,
                playlistId = 10,
                songId = 502,
                songUri = "content://media/external/audio/media/502",
                title = "Physical",
                artist = "Dua Lipa",
                album = "Future Nostalgia",
                duration = 193000L,
                position = 1
            )
        )

        val exported = M3uHelper.generateM3u("Synthwave", items)

        assertTrue(exported.contains("#EXTM3U"))
        assertTrue(exported.contains("#PLAYLIST:Synthwave"))
        assertTrue(exported.contains("#EXTINF:230,The Weeknd - Starboy"))
        assertTrue(exported.contains("content://media/external/audio/media/501"))

        // Round-trip verification
        val reParsed = M3uHelper.parseM3u(exported)
        assertEquals(2, reParsed.size)
        assertEquals("Starboy", reParsed[0].title)
        assertEquals("The Weeknd", reParsed[0].artist)
        assertEquals(230L, reParsed[0].durationSeconds)
    }
}
