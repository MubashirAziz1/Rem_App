package com.quickalarm.prayer

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Tests for the pure offset math. The offsets are applied in the *city's* frame (so
 * "30 minutes before sunrise" stays true regardless of the device), then each instant is
 * converted to the *device's* zone for the Clock hand-off. Times at or before `now` are
 * dropped, because ACTION_SET_ALARM would otherwise silently schedule them for tomorrow.
 */
class PrayerScheduleTest {

    private val karachi = ZoneId.of("Asia/Karachi") // UTC+5, no DST
    private val utc = ZoneId.of("UTC")
    private val date = LocalDate.of(2026, 10, 21)

    private fun nowAt(hour: Int, minute: Int, zone: ZoneId = karachi): Instant =
        date.atTime(hour, minute).atZone(zone).toInstant()

    @Test
    fun `returns all six times in firing order when sunrise is later today`() {
        val times = PrayerSchedule.wakeUpTimes(
            date = date,
            sunrise = LocalTime.of(5, 54),
            cityZone = karachi,
            deviceZone = karachi,
            now = nowAt(0, 0),
        )

        assertEquals(
            listOf(
                LocalTime.of(5, 24), // -30
                LocalTime.of(5, 29), // -25
                LocalTime.of(5, 34), // -20
                LocalTime.of(5, 39), // -15
                LocalTime.of(5, 44), // -10
                LocalTime.of(5, 49), // -5
            ),
            times,
        )
    }

    @Test
    fun `drops the offsets that have already passed today`() {
        val times = PrayerSchedule.wakeUpTimes(
            date = date,
            sunrise = LocalTime.of(5, 54),
            cityZone = karachi,
            deviceZone = karachi,
            now = nowAt(5, 35),
        )

        // 5:24, 5:29 and 5:34 are at or before 5:35 and must be dropped.
        assertEquals(
            listOf(LocalTime.of(5, 39), LocalTime.of(5, 44), LocalTime.of(5, 49)),
            times,
        )
    }

    @Test
    fun `returns an empty list when every offset has passed`() {
        val times = PrayerSchedule.wakeUpTimes(
            date = date,
            sunrise = LocalTime.of(5, 54),
            cityZone = karachi,
            deviceZone = karachi,
            now = nowAt(9, 0),
        )

        assertEquals(emptyList<LocalTime>(), times)
    }

    @Test
    fun `handles midnight wraparound for an early sunrise`() {
        val times = PrayerSchedule.wakeUpTimes(
            date = date,
            sunrise = LocalTime.of(0, 20),
            cityZone = karachi,
            deviceZone = karachi,
            now = nowAt(0, 0).minusSeconds(7200), // two hours earlier, previous day
        )

        // The -30 offset wraps to the previous calendar day at 23:50.
        assertEquals(
            listOf(
                LocalTime.of(23, 50),
                LocalTime.of(23, 55),
                LocalTime.of(0, 0),
                LocalTime.of(0, 5),
                LocalTime.of(0, 10),
                LocalTime.of(0, 15),
            ),
            times,
        )
    }

    @Test
    fun `converts the city's sunrise into the device's timezone`() {
        // 05:54 in Karachi (UTC+5) is 00:54 UTC, so the device in UTC sees times from 00:24.
        val times = PrayerSchedule.wakeUpTimes(
            date = date,
            sunrise = LocalTime.of(5, 54),
            cityZone = karachi,
            deviceZone = utc,
            now = nowAt(0, 0, utc).minusSeconds(3600),
        )

        assertEquals(
            listOf(
                LocalTime.of(0, 24),
                LocalTime.of(0, 29),
                LocalTime.of(0, 34),
                LocalTime.of(0, 39),
                LocalTime.of(0, 44),
                LocalTime.of(0, 49),
            ),
            times,
        )
    }

    @Test
    fun `applies offsets in the city frame across a DST transition`() {
        // London springs forward at 01:00 on 2026-03-29 (01:00 -> 02:00). A sunrise of
        // 06:30 local is 05:30 UTC, and the -30 offset must still be exactly 30 minutes
        // before that instant, not 30 minutes of wall clock.
        val london = ZoneId.of("Europe/London")
        val dstDate = LocalDate.of(2026, 3, 29)

        val times = PrayerSchedule.wakeUpTimes(
            date = dstDate,
            sunrise = LocalTime.of(6, 30),
            cityZone = london,
            deviceZone = london,
            now = dstDate.atTime(0, 0).atZone(london).toInstant(),
        )

        assertEquals(
            listOf(
                LocalTime.of(6, 0),
                LocalTime.of(6, 5),
                LocalTime.of(6, 10),
                LocalTime.of(6, 15),
                LocalTime.of(6, 20),
                LocalTime.of(6, 25),
            ),
            times,
        )
    }

    @Test
    fun `exposes the offsets as a single source of truth`() {
        assertEquals(listOf(-30, -25, -20, -15, -10, -5), PrayerSchedule.OFFSETS_MIN)
    }
}
