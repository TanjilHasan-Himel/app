package com.eplaytime.app.util

import java.util.Calendar
import java.util.TimeZone

/** Pure next-occurrence calculation shared by scheduling and tests. */
object ScheduledOccurrenceCalculator {
    private val dayNames = mapOf(
        Calendar.SUNDAY to "SUN",
        Calendar.MONDAY to "MON",
        Calendar.TUESDAY to "TUE",
        Calendar.WEDNESDAY to "WED",
        Calendar.THURSDAY to "THU",
        Calendar.FRIDAY to "FRI",
        Calendar.SATURDAY to "SAT"
    )

    fun nextTriggerMillis(
        hour: Int,
        minute: Int,
        repeatDays: String,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        require(hour in 0..23) { "hour must be 0..23" }
        require(minute in 0..59) { "minute must be 0..59" }

        val candidate = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val days = repeatDays.split(',').map(String::trim).filter(String::isNotEmpty).toSet()
        if (days.isEmpty()) {
            if (candidate.timeInMillis <= nowMillis) candidate.add(Calendar.DAY_OF_MONTH, 1)
            return candidate.timeInMillis
        }

        repeat(8) {
            if (candidate.timeInMillis > nowMillis && dayNames[candidate.get(Calendar.DAY_OF_WEEK)] in days) {
                return candidate.timeInMillis
            }
            candidate.add(Calendar.DAY_OF_MONTH, 1)
        }
        error("No valid repeat day in $repeatDays")
    }
}
