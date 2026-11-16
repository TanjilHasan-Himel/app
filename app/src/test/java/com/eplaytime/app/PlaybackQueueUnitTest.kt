package com.eplaytime.app

import com.eplaytime.app.data.model.Song
import com.eplaytime.app.playback.playbackQueue
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackQueueUnitTest {
    @Test
    fun removesInvalidAndDuplicateUrisWithoutChangingOrder() {
        val songs = listOf(song(1, "one"), song(2, ""), song(3, "one"), song(4, "two"))

        assertEquals(listOf("one", "two"), playbackQueue(songs).map { it.uri })
    }

    private fun song(id: Long, uri: String) = Song(
        id = id,
        title = "Song $id",
        artist = "Artist",
        album = "Album",
        uri = uri,
        duration = 1_000L
    )
}
