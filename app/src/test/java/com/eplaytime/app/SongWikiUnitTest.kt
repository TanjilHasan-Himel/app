package com.eplaytime.app

import com.eplaytime.app.data.model.Song
import com.eplaytime.app.data.model.SongWikiData
import org.junit.Assert.*
import org.junit.Test

class SongWikiUnitTest {

    @Test
    fun testSongWikiDataModel() {
        val wiki = SongWikiData(
            title = "Jhol",
            artist = "Maanu, Annural Khalid",
            summary = "Jhol is a popular Pakistani indie song by Maanu and Annural Khalid.",
            description = "Indie Pop track",
            thumbnailUrl = "https://upload.wikimedia.org/test.jpg",
            composer = "Maanu",
            lyricist = "Maanu, Annural Khalid",
            producer = "Abdullah Siddiqui",
            releaseDate = "2024",
            sourceUrl = "https://en.wikipedia.org/wiki/Jhol"
        )

        assertEquals("Jhol", wiki.title)
        assertEquals("Maanu, Annural Khalid", wiki.artist)
        assertEquals("Abdullah Siddiqui", wiki.producer)
        assertNotNull(wiki.summary)
        assertTrue(wiki.sourceUrl?.contains("wikipedia") == true)
    }

    @Test
    fun testSongModelProperties() {
        val song = Song(
            id = 101L,
            title = "Dil Kaa Jo Haal Hai",
            artist = "Abhijeet, Shreya Ghoshal",
            album = "Besharam",
            uri = "content://media/external/audio/media/101",
            duration = 321000L,
            albumArtUri = null,
            dateAdded = System.currentTimeMillis(),
            folderPath = "/storage/emulated/0/Music/Dil Kaa Jo Haal Hai.mp3",
            bucketDisplayName = "Music"
        )

        assertEquals(101L, song.id)
        assertEquals("Dil Kaa Jo Haal Hai", song.title)
        assertEquals("Music", song.bucketDisplayName)
    }
}
