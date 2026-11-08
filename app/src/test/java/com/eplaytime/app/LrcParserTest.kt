package com.eplaytime.app

import com.eplaytime.app.util.LrcParser
import org.junit.Assert.*
import org.junit.Test

class LrcParserTest {

    @Test
    fun testStandardLrcParsing() {
        val lrc = """
            [ti:Chammak Challo]
            [ar:Akon]
            [00:12.50]Kaisa sharmana aaja nach ke dikha de
            [00:18.20]Aa meri hove aaja parda gira de
            [00:24.00]Wanna be my Chammak Challo
        """.trimIndent()

        val parsed = LrcParser.parse(lrc)
        assertTrue(parsed.isSynced)
        assertEquals(3, parsed.lines.size)

        assertEquals(12500L, parsed.lines[0].timestampMs)
        assertEquals("Kaisa sharmana aaja nach ke dikha de", parsed.lines[0].content)

        assertEquals(18200L, parsed.lines[1].timestampMs)
        assertEquals(24000L, parsed.lines[2].timestampMs)
    }

    @Test
    fun testActiveIndexLookup() {
        val lrc = """
            [00:10.00]Line 1
            [00:20.00]Line 2
            [00:30.00]Line 3
        """.trimIndent()

        val parsed = LrcParser.parse(lrc)

        // Instrumental prelude before 10.00s returns -1 (no lyric active yet)
        assertEquals(-1, LrcParser.findActiveLyricIndex(parsed.lines, 0L))
        assertEquals(-1, LrcParser.findActiveLyricIndex(parsed.lines, 5000L))
        assertEquals(-1, LrcParser.findActiveLyricIndex(parsed.lines, 9999L))

        // Line 1 becomes active exactly at 10.00s
        assertEquals(0, LrcParser.findActiveLyricIndex(parsed.lines, 10000L))
        assertEquals(0, LrcParser.findActiveLyricIndex(parsed.lines, 15000L))

        // Line 2 becomes active at 20.00s
        assertEquals(1, LrcParser.findActiveLyricIndex(parsed.lines, 20000L))
        assertEquals(1, LrcParser.findActiveLyricIndex(parsed.lines, 29999L))

        // Line 3 becomes active at 30.00s
        assertEquals(2, LrcParser.findActiveLyricIndex(parsed.lines, 30000L))
        assertEquals(2, LrcParser.findActiveLyricIndex(parsed.lines, 60000L))
    }

    @Test
    fun testEmptyOrInvalidLrc() {
        val emptyParsed = LrcParser.parse("")
        assertFalse(emptyParsed.isSynced)
        assertTrue(emptyParsed.lines.isEmpty())

        val nullParsed = LrcParser.parse(null)
        assertFalse(nullParsed.isSynced)
        assertTrue(nullParsed.lines.isEmpty())
    }
}

