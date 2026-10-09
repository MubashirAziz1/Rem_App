package com.quickalarm.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.util.Locale

class SetAlarmHandoffTest {

    @Test
    fun `dayFor returns TODAY when chosen time is later today`() {
        val now = LocalDateTime.of(2026, 10, 9, 8, 0)
        assertEquals(ScheduledDay.TODAY, SetAlarmHandoff.dayFor(12, 30, now))
    }

    @Test
    fun `dayFor returns TOMORROW when chosen time equals now`() {
        val now = LocalDateTime.of(2026, 10, 9, 8, 0)
        assertEquals(ScheduledDay.TOMORROW, SetAlarmHandoff.dayFor(8, 0, now))
    }

    @Test
    fun `dayFor returns TOMORROW when chosen time is earlier today`() {
        val now = LocalDateTime.of(2026, 10, 9, 8, 0)
        assertEquals(ScheduledDay.TOMORROW, SetAlarmHandoff.dayFor(7, 30, now))
    }

    @Test
    fun `dayFor returns TODAY for 23-59 later today at midnight`() {
        val now = LocalDateTime.of(2026, 10, 9, 0, 0)
        assertEquals(ScheduledDay.TODAY, SetAlarmHandoff.dayFor(23, 59, now))
    }

    @Test
    fun `dayFor returns TOMORROW for 00-00 equal to now at midnight`() {
        val now = LocalDateTime.of(2026, 10, 9, 0, 0)
        assertEquals(ScheduledDay.TOMORROW, SetAlarmHandoff.dayFor(0, 0, now))
    }

    @Test
    fun `dayFor returns TODAY for 23-59 just before midnight`() {
        val now = LocalDateTime.of(2026, 10, 9, 23, 58)
        assertEquals(ScheduledDay.TODAY, SetAlarmHandoff.dayFor(23, 59, now))
    }

    @Test
    fun `dayFor returns TOMORROW for 00-00 earlier today`() {
        val now = LocalDateTime.of(2026, 10, 9, 0, 1)
        assertEquals(ScheduledDay.TOMORROW, SetAlarmHandoff.dayFor(0, 0, now))
    }

    // --- Task 5: 24-hour range validation ---

    @Test
    fun `validateInput rejects hour -1`() {
        val r = SetAlarmHandoff.validateInput(-1, 0, "Wake up")
        assert(r is HandoffResult.Invalid) { "expected Invalid for hour -1, got $r" }
    }

    @Test
    fun `validateInput rejects hour 24`() {
        val r = SetAlarmHandoff.validateInput(24, 0, "Wake up")
        assert(r is HandoffResult.Invalid) { "expected Invalid for hour 24, got $r" }
    }

    @Test
    fun `validateInput rejects minute 60`() {
        val r = SetAlarmHandoff.validateInput(0, 60, "Wake up")
        assert(r is HandoffResult.Invalid) { "expected Invalid for minute 60, got $r" }
    }

    @Test
    fun `validateInput accepts boundary 0 0`() {
        assertNull(SetAlarmHandoff.validateInput(0, 0, "Wake up"))
    }

    @Test
    fun `validateInput accepts boundary 23 59`() {
        assertNull(SetAlarmHandoff.validateInput(23, 59, "Wake up"))
    }

    // --- Task 5: title validation ---

    @Test
    fun `validateTitle returns null for empty string`() {
        assertNull(SetAlarmHandoff.validateTitle(""))
    }

    @Test
    fun `validateTitle returns null for whitespace only`() {
        assertNull(SetAlarmHandoff.validateTitle("   "))
    }

    @Test
    fun `validateTitle trims and returns a padded title`() {
        assertEquals("Wake up", SetAlarmHandoff.validateTitle("  Wake up  "))
    }

    @Test
    fun `validateInput rejects blank title even with valid time`() {
        val r = SetAlarmHandoff.validateInput(7, 5, "   ")
        assert(r is HandoffResult.Invalid) { "expected Invalid for blank title, got $r" }
    }

    // --- Task 5: locale-independent HH:mm formatting ---

    @Test
    fun `formatTime produces zero-padded ASCII HH mm`() {
        assertEquals("07:05", SetAlarmHandoff.formatTime(7, 5))
        assertEquals("00:00", SetAlarmHandoff.formatTime(0, 0))
        assertEquals("23:59", SetAlarmHandoff.formatTime(23, 59))
    }

    @Test
    fun `formatTime uses ASCII digits regardless of default locale`() {
        val original = Locale.getDefault()
        try {
            // A locale whose native digits are not Latin.
            Locale.setDefault(Locale.forLanguageTag("ar"))
            assertEquals("07:05", SetAlarmHandoff.formatTime(7, 5))
        } finally {
            Locale.setDefault(original)
        }
    }
}
