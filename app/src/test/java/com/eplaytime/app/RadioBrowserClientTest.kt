package com.eplaytime.app

import com.eplaytime.app.data.network.RadioBrowserClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioBrowserClientTest {
    @Test
    fun `parser prefers resolved stream and removes duplicate uuid`() {
        val json = """[
          {"stationuuid":"one","name":" Test FM ","url":"http://old","url_resolved":"https://live","favicon":"https://logo","countrycode":"BD","country":"Bangladesh","language":"bangla","tags":"music","codec":"MP3","bitrate":128,"hls":0,"lastcheckok":1},
          {"stationuuid":"one","name":"Duplicate","url":"https://duplicate","lastcheckok":1}
        ]"""

        val stations = RadioBrowserClient().parseStations(json)

        assertEquals(1, stations.size)
        assertEquals("https://live", stations.single().streamUrl)
        assertEquals("http://old", stations.single().fallbackUrl)
        assertTrue(stations.single().isWorking)
    }

    @Test
    fun `parser rejects rows without identity or stream`() {
        val stations = RadioBrowserClient().parseStations(
            """[{"stationuuid":"","name":"No id","url":"https://x"},{"stationuuid":"two","name":"No stream","url":""}]"""
        )

        assertTrue(stations.isEmpty())
    }
}
