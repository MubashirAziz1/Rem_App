package com.quickalarm.prayer

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The six gradual wake-up times for one day, derived from sunrise.
 *
 * The offsets are applied in the **city's** frame, so "30 minutes before sunrise" stays
 * literally true, and only then is each instant converted to the **device's** zone for the
 * Clock hand-off. Working in [Instant]s rather than wall-clock arithmetic means a DST
 * transition inside the offset window still produces exact 5-minute gaps.
 *
 * Times at or before [Instant] now are dropped: `ACTION_SET_ALARM` carries no date and would
 * silently schedule a past time for tomorrow, ringing a day late.
 */
object PrayerSchedule {

    /** Minutes relative to sunrise, in firing order. The single source of truth for the count. */
    val OFFSETS_MIN = listOf(-30, -25, -20, -15, -10, -5)

    /**
     * Returns sunrise plus each offset that is still in the future, expressed in [deviceZone].
     * The result can be shorter than [OFFSETS_MIN] (or empty) when part of the window has passed.
     */
    fun wakeUpTimes(
        date: LocalDate,
        sunrise: LocalTime,
        cityZone: ZoneId,
        deviceZone: ZoneId,
        now: Instant,
    ): List<LocalTime> {
        val sunriseInstant = date.atTime(sunrise).atZone(cityZone).toInstant()
        return OFFSETS_MIN
            .map { sunriseInstant.plusSeconds(it * 60L) }
            .filter { it.isAfter(now) }
            .map { LocalTime.ofInstant(it, deviceZone) }
    }
}
