package com.eplaytime.app.playback

import com.eplaytime.app.data.model.Song

internal fun playbackQueue(songs: List<Song>): List<Song> =
    songs.filter { it.uri.isNotBlank() }.distinctBy { it.uri }

