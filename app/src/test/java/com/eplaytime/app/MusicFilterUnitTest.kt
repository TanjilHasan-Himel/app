package com.eplaytime.app

import com.eplaytime.app.data.model.Song
import com.eplaytime.app.data.repository.MusicScanFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicFilterUnitTest {

    private val sampleSongs = listOf(
        Song(
            id = 1,
            title = "Tum Hi Ho",
            artist = "Arijit Singh",
            album = "Aashiqui 2",
            uri = "content://media/1",
            duration = 262000L,
            folderPath = "/storage/emulated/0/Music/Aashiqui 2/Tum Hi Ho.mp3",
            bucketDisplayName = "Music"
        ),
        Song(
            id = 2,
            title = "Chammak Challo",
            artist = "Akon",
            album = "Ra-One",
            uri = "content://media/2",
            duration = 224000L,
            folderPath = "/storage/emulated/0/Music/Chammak Challo.mp3",
            bucketDisplayName = "Music"
        ),
        // Short audio (< 30s)
        Song(
            id = 3,
            title = "Aurora - Samsung Ringtone",
            artist = "Unknown Artist",
            album = "Ringtones",
            uri = "content://media/3",
            duration = 21864L, // 21.8s
            folderPath = "/storage/emulated/0/Ringtones/Aurora - Samsung .mp3",
            bucketDisplayName = "Ringtones"
        ),
        Song(
            id = 4,
            title = "Voice 011",
            artist = "Unknown Artist",
            album = "Voice Recorder",
            uri = "content://media/4",
            duration = 2901L, // 2.9s
            folderPath = "/storage/emulated/0/Recordings/Voice Recorder/Voice 011.m4a",
            bucketDisplayName = "Voice Recorder"
        ),
        // Long Call recording (> 30s)
        Song(
            id = 5,
            title = "Call_Recording_20261108",
            artist = "Phone",
            album = "Call",
            uri = "content://media/5",
            duration = 734812L, // 12 mins
            folderPath = "/storage/emulated/0/Recordings/Call/Call_Recording_20261108.m4a",
            bucketDisplayName = "Call"
        ),
        Song(
            id = 6,
            title = "Voice 001",
            artist = "Unknown",
            album = "Voice Recorder",
            uri = "content://media/6",
            duration = 734812L, // long voice recording
            folderPath = "/storage/emulated/0/Recordings/Voice Recorder/Voice 001.m4a",
            bucketDisplayName = "Voice Recorder"
        )
    )

    private fun filterSongs(
        songs: List<Song>,
        filterShortAudio: Boolean,
        filterCallRecordings: Boolean
    ): List<Song> {
        return MusicScanFilter.apply(
            songs, if (filterShortAudio) 30 else 0, filterCallRecordings, "EXCLUDE", emptySet()
        )
    }

    @Test
    fun testNoFilters_returnsAll() {
        val result = filterSongs(sampleSongs, filterShortAudio = false, filterCallRecordings = false)
        assertEquals(6, result.size)
    }

    @Test
    fun testFilterShortAudio_removesRingtonesAndShortVoice() {
        val result = filterSongs(sampleSongs, filterShortAudio = true, filterCallRecordings = false)
        assertEquals(4, result.size)
        assertFalse(result.any { it.title.contains("Aurora") })
        assertFalse(result.any { it.title == "Voice 011" })
        assertTrue(result.any { it.title == "Tum Hi Ho" })
        assertTrue(result.any { it.title == "Chammak Challo" })
    }

    @Test
    fun testFilterCallRecordings_removesCallsAndVoiceRecordings() {
        val result = filterSongs(sampleSongs, filterShortAudio = false, filterCallRecordings = true)
        assertEquals(3, result.size)
        assertFalse(result.any { it.bucketDisplayName == "Call" })
        assertFalse(result.any { it.bucketDisplayName == "Voice Recorder" })
        assertTrue(result.any { it.title == "Tum Hi Ho" })
        assertTrue(result.any { it.title == "Chammak Challo" })
    }

    @Test
    fun testBothFiltersActive_retainsOnlyRealMusic() {
        val result = filterSongs(sampleSongs, filterShortAudio = true, filterCallRecordings = true)
        assertEquals(2, result.size)
        assertEquals("Tum Hi Ho", result[0].title)
        assertEquals("Chammak Challo", result[1].title)
    }

    @Test
    fun testCategoryDerivation_matchesFilteredSongs() {
        val filtered = filterSongs(sampleSongs, filterShortAudio = true, filterCallRecordings = true)
        val albums = filtered.groupBy { it.album }.keys
        val artists = filtered.groupBy { it.artist }.keys

        assertEquals(setOf("Aashiqui 2", "Ra-One"), albums)
        assertEquals(setOf("Arijit Singh", "Akon"), artists)
        assertFalse(albums.contains("Voice Recorder"))
        assertFalse(albums.contains("Call"))
        assertFalse(albums.contains("Ringtones"))
    }

    // --- Echo Music Advanced Scan Filters Tests ---

    private fun filterWithAdvancedOptions(
        songs: List<Song>,
        minDurationSec: Int,
        filterCallRecordings: Boolean,
        folderFilterMode: String,
        filteredFolders: Set<String>
    ): List<Song> {
        return MusicScanFilter.apply(songs, minDurationSec, filterCallRecordings, folderFilterMode, filteredFolders)
    }

    @Test
    fun testWhatsAppVoiceNotesAreHiddenButWhatsAppSongsRemain() {
        val voiceNote = sampleSongs.first().copy(
            id = 20,
            title = "PTT-20261112-WA0001",
            folderPath = "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Voice Notes/voice.opus",
            bucketDisplayName = "WhatsApp Voice Notes"
        )
        val sharedSong = sampleSongs.first().copy(
            id = 21,
            title = "Shared Song",
            folderPath = "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Audio/song.mp3",
            bucketDisplayName = "WhatsApp Audio"
        )

        val result = MusicScanFilter.apply(listOf(voiceNote, sharedSong), 0, true, "EXCLUDE", emptySet())

        assertEquals(listOf(sharedSong), result)
    }

    @Test
    fun testMinDurationSec_45SecondsFilter() {
        // Tum Hi Ho: 262s, Chammak Challo: 224s, Aurora: 21s, Voice 011: 2.9s, Call: 734s, Voice 001: 734s
        val result = filterWithAdvancedOptions(
            sampleSongs,
            minDurationSec = 45,
            filterCallRecordings = false,
            folderFilterMode = "exclude",
            filteredFolders = emptySet()
        )
        // Only songs >= 45 seconds should remain (4 songs)
        assertEquals(4, result.size)
        assertFalse(result.any { it.duration < 45000L })
    }

    @Test
    fun testFolderFilterMode_ExcludeFolder() {
        val result = filterWithAdvancedOptions(
            sampleSongs,
            minDurationSec = 0,
            filterCallRecordings = false,
            folderFilterMode = "exclude",
            filteredFolders = setOf("/storage/emulated/0/Recordings")
        )
        // Songs in /Recordings (Voice 011, Call, Voice 001) should be excluded
        assertEquals(3, result.size)
        assertFalse(result.any { it.folderPath.contains("/Recordings") })
        assertTrue(result.any { it.title == "Tum Hi Ho" })
        assertTrue(result.any { it.title == "Chammak Challo" })
        assertTrue(result.any { it.title.contains("Aurora") })
    }

    @Test
    fun testFolderFilterMode_IncludeOnlyFolder() {
        val result = filterWithAdvancedOptions(
            sampleSongs,
            minDurationSec = 0,
            filterCallRecordings = false,
            folderFilterMode = "include",
            filteredFolders = setOf("/storage/emulated/0/Music")
        )
        // Only songs in /Music should be included
        assertEquals(2, result.size)
        assertTrue(result.all { it.folderPath.contains("/Music") })
        assertEquals("Tum Hi Ho", result[0].title)
        assertEquals("Chammak Challo", result[1].title)
    }
}

