package com.eplaytime.app

import com.eplaytime.app.ui.components.verticalFaderOffsetPx
import org.junit.Assert.assertEquals
import org.junit.Test

class VerticalFaderGeometryTest {
    @Test
    fun knobOffsetMatchesTopCenterAndBottomOfTrack() {
        assertEquals(-75f, verticalFaderOffsetPx(1f, 150f), 0.001f)
        assertEquals(0f, verticalFaderOffsetPx(0.5f, 150f), 0.001f)
        assertEquals(75f, verticalFaderOffsetPx(0f, 150f), 0.001f)
    }
}
