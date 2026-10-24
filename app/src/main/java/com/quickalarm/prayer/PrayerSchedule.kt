package com.quickalarm.prayer

import java.time.Duration
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
 * `ACTION_SET_ALARM` carries no date: the Clock always schedules the *next* occurrence of the
 * time it is given. That single fact drives both branches below.
 */
object PrayerSchedule {

    /** Minutes relative to sunrise, in firing order. The single source of truth for the count. */
    val OFFSETS_MIN = listOf(-30, -25, -20, -15, -10, -5)

    /**
     * Returns the wake-up times to hand to the Clock, expressed in [deviceZone].
     *
     * - If any offset is still ahead today, only those are returned, so the Clock fires them
     *   **today**. (Passing an already-passed time would roll it to tomorrow and ring a day late.)
     * - If the whole window has already passed, all six are returned shifted to the next day --
     *   the Clock's own next-occurrence rule then lands them **tomorrow**, which is what the user
     *   wants from a daily alarm rather than silently scheduling nothing.
     *
     * The result is therefore never empty while a sunrise is known.
     */
    fun wakeUpTimes(
        date: LocalDate,
        sunrise: LocalTime,
        cityZone: ZoneId,
        deviceZone: ZoneId,
        now: Instant,
    ): List<LocalTime> {
        val sunriseInstant = date.atTime(sunrise).atZone(cityZone).toInstant()
        val today = OFFSETS_MIN.map { sunriseInstant.plusSeconds(it * 60L) }
        val stillAheadToday = today.filter { it.isAfter(now) }
        val chosen = if (stillAheadToday.isNotEmpty()) {
            stillAheadToday
        } else {
            // `Duration.ofDays(1)` is exact 24h of absolute time, so tomorrow's wall clock
            // stays correct across a DST transition at night.
            today.map { it.plus(Duration.ofDays(1)) }
        }
        return chosen.map { LocalTime.ofInstant(it, deviceZone) }
    }
}
