package com.eplaytime.app

import com.eplaytime.app.util.ScheduledOccurrenceCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ScheduledOccurrenceCalculatorTest {
    private val zone = TimeZone.getTimeZone("Asia/Dhaka")

    @Test
    fun oneTimePastTodayMovesToTomorrow() {
        val now = at(2026, Calendar.NOVEMBER, 13, 9, 0)
        val result = ScheduledOccurrenceCalculator.nextTriggerMillis(8, 30, "", now, zone)
        assertEquals(at(2026, Calendar.NOVEMBER, 14, 8, 30), result)
    }

    @Test
    fun repeatSelectsNextConfiguredWeekday() {
        // 13 Nov 2026 is Friday; Monday is the next selected day.
        val now = at(2026, Calendar.NOVEMBER, 13, 9, 0)
        val result = ScheduledOccurrenceCalculator.nextTriggerMillis(5, 30, "MON,WED", now, zone)
        assertEquals(at(2026, Calendar.NOVEMBER, 16, 5, 30), result)
    }

    @Test
    fun repeatLaterTodayUsesToday() {
        val now = at(2026, Calendar.NOVEMBER, 13, 4, 0)
        val result = ScheduledOccurrenceCalculator.nextTriggerMillis(5, 30, "FRI", now, zone)
        assertEquals(at(2026, Calendar.NOVEMBER, 13, 5, 30), result)
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(year, month, day, hour, minute)
        }.timeInMillis
}
