package com.drowz.app.platform.schedule

import com.drowz.app.domain.model.TimeOfDay
import com.drowz.app.domain.model.Weekday
import java.time.ZonedDateTime

/**
 * Wall-clock scheduling math, kept in one place. Uses `java.time` (available on minSdk 26)
 * and the device's current zone so DST and midnight-wrap are handled correctly.
 */
internal object ScheduleTimes {

    /**
     * The next occurrence of [time] strictly after [from]. If [days] is non-null, only days
     * in that set qualify (used for bedtime); pass null to allow any day (used for wake,
     * which is simply the next morning after the bedtime it belongs to).
     */
    fun nextTimeAfter(time: TimeOfDay, from: ZonedDateTime, days: Set<Weekday>?): ZonedDateTime {
        for (i in 0..7) {
            val candidate = from.toLocalDate().plusDays(i.toLong())
                .atTime(time.hour, time.minute)
                .atZone(from.zone)
            if (candidate.isAfter(from) && (days == null || weekdayOf(candidate) in days)) {
                return candidate
            }
        }
        return from.plusDays(1)   // unreachable when days is null or non-empty
    }

    private fun weekdayOf(zdt: ZonedDateTime): Weekday =
        Weekday.entries[zdt.dayOfWeek.value - 1]   // DayOfWeek: MON=1..SUN=7 → ordinal 0..6
}
