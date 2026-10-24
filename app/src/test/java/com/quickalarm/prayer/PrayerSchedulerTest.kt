package com.quickalarm.prayer

import com.quickalarm.alarm.HandoffResult
import com.quickalarm.alarm.ScheduledDay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Guards the behaviour that fixes dropped alarms: six hand-offs are dispatched one at a
 * time with a gap between them. Before the fix they went out in a tight loop and the stock
 * Clock silently dropped all but the first.
 */
class PrayerSchedulerTest {

    private val sixTimes = listOf(
        LocalTime.of(5, 24),
        LocalTime.of(5, 29),
        LocalTime.of(5, 34),
        LocalTime.of(5, 39),
        LocalTime.of(5, 44),
        LocalTime.of(5, 49),
    )

    private fun success(hour: Int, minute: Int, title: String) =
        HandoffResult.Success(hour, minute, title, ScheduledDay.TODAY)

    @Test
    fun `waits between hand-offs but not before the first`() = runBlocking {
        val waits = mutableListOf<Long>()

        val results = PrayerScheduler.dispatchSequentially(
            times = sixTimes,
            total = 6,
            gapMs = 2_000L,
            wait = { waits += it },
            handOff = { hour, minute, title -> success(hour, minute, title) },
        )

        // Six hand-offs means five gaps, and never one before the first.
        assertEquals(6, results.size)
        assertEquals(listOf(2_000L, 2_000L, 2_000L, 2_000L, 2_000L), waits)
    }

    @Test
    fun `dispatches each time in order with a distinct numbered title`() = runBlocking {
        val dispatched = mutableListOf<Triple<Int, Int, String>>()

        PrayerScheduler.dispatchSequentially(
            times = sixTimes,
            total = 6,
            gapMs = 0L,
            wait = {},
            handOff = { hour, minute, title ->
                dispatched += Triple(hour, minute, title)
                success(hour, minute, title)
            },
        )

        assertEquals(
            listOf(
                Triple(5, 24, "Fajr 1/6"),
                Triple(5, 29, "Fajr 2/6"),
                Triple(5, 34, "Fajr 3/6"),
                Triple(5, 39, "Fajr 4/6"),
                Triple(5, 44, "Fajr 5/6"),
                Triple(5, 49, "Fajr 6/6"),
            ),
            dispatched,
        )
    }

    @Test
    fun `a shorter list of times keeps the day's total in the titles`() = runBlocking {
        // Titles are numbered by dispatch order with the day's total as the denominator
        // (unchanged from the original behavior), so a partial day reads 1/6 ... 3/6.
        val dispatched = mutableListOf<String>()

        val results = PrayerScheduler.dispatchSequentially(
            times = sixTimes.takeLast(3),
            total = 6,
            gapMs = 0L,
            wait = {},
            handOff = { hour, minute, title ->
                dispatched += title
                success(hour, minute, title)
            },
        )

        assertEquals(3, results.size)
        assertEquals(listOf("Fajr 1/6", "Fajr 2/6", "Fajr 3/6"), dispatched)
    }

    @Test
    fun `summarize counts successes and collects failures`() {
        val results = listOf(
            success(5, 24, "Fajr 1/6"),
            HandoffResult.NoClockApp,
            success(5, 34, "Fajr 3/6"),
        )

        val summary = PrayerScheduler.summarize(results, total = 6, day = ScheduledDay.TODAY)

        assertEquals(3, summary.requested)
        assertEquals(2, summary.succeeded)
        assertEquals(6, summary.total)
        assertEquals(ScheduledDay.TODAY, summary.day)
        assertEquals(listOf("No Clock app can set alarms"), summary.failures)
        assertEquals(false, summary.allSucceeded)
    }

    @Test
    fun `summarize reports full success and the day the alarms land`() {
        val results = sixTimes.mapIndexed { index, time ->
            success(time.hour, time.minute, "Fajr ${index + 1}/6")
        }

        val summary = PrayerScheduler.summarize(results, total = 6, day = ScheduledDay.TOMORROW)

        assertEquals(6, summary.succeeded)
        assertEquals(6, summary.total)
        assertEquals(ScheduledDay.TOMORROW, summary.day)
        assertEquals(emptyList<String>(), summary.failures)
        assertEquals(true, summary.allSucceeded)
    }

    @Test
    fun `dayFor reports TODAY when the first wake-up is still ahead`() {
        val karachi = ZoneId.of("Asia/Karachi")
        val now = LocalDate.of(2026, 10, 21).atTime(5, 0).atZone(karachi).toInstant()

        val day = PrayerScheduler.dayFor(
            wakeUps = listOf(LocalTime.of(5, 24), LocalTime.of(5, 29)),
            deviceZone = karachi,
            now = now,
        )

        assertEquals(ScheduledDay.TODAY, day)
    }

    @Test
    fun `dayFor reports TOMORROW when the first wake-up has passed`() {
        val karachi = ZoneId.of("Asia/Karachi")
        val now = LocalDate.of(2026, 10, 21).atTime(9, 0).atZone(karachi).toInstant()

        val day = PrayerScheduler.dayFor(
            wakeUps = listOf(LocalTime.of(5, 24), LocalTime.of(5, 29)),
            deviceZone = karachi,
            now = now,
        )

        assertEquals(ScheduledDay.TOMORROW, day)
    }
}
