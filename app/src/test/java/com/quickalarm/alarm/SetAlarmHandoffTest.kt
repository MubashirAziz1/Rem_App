package com.quickalarm.alarm

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

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
}
