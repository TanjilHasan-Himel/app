package com.eplaytime.app

import com.eplaytime.app.data.network.LyricsCandidate
import com.eplaytime.app.data.network.LyricsCandidateScorer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsCandidateScorerTest {
    private val correct = LyricsCandidate(
        id = 1,
        title = "I Want to Live",
        artist = "Borislav Slavov",
        album = "Baldur's Gate 3",
        durationSeconds = 233,
        syncedLyrics = "[00:01.00] Hello",
        plainLyrics = null
    )

    @Test
    fun `exact metadata ranks first and is auto accepted`() {
        val ranked = LyricsCandidateScorer.rank(
            candidates = listOf(
                correct.copy(id = 2, artist = "Cover Artist", durationSeconds = 280),
                correct
            ),
            title = "I Want to Live",
            artist = "Borislav Slavov",
            album = "Baldur's Gate 3",
            durationSeconds = 233
        )

        assertEquals(1L, ranked.first().id)
        assertTrue(ranked.first().score >= LyricsCandidateScorer.AUTO_ACCEPT_SCORE)
    }

    @Test
    fun `wrong artist or distant duration is rejected`() {
        val ranked = LyricsCandidateScorer.rank(
            candidates = listOf(
                correct.copy(artist = "Someone Else"),
                correct.copy(durationSeconds = 300)
            ),
            title = correct.title,
            artist = correct.artist,
            album = correct.album,
            durationSeconds = correct.durationSeconds
        )

        assertTrue(ranked.isEmpty())
    }

    @Test
    fun `candidate without lyrics is rejected`() {
        val ranked = LyricsCandidateScorer.rank(
            candidates = listOf(correct.copy(syncedLyrics = null, plainLyrics = null)),
            title = correct.title,
            artist = correct.artist,
            album = correct.album,
            durationSeconds = correct.durationSeconds
        )

        assertTrue(ranked.isEmpty())
    }
}
