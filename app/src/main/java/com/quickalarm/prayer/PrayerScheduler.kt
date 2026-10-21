package com.quickalarm.prayer

import android.content.Context
import com.quickalarm.alarm.HandoffResult
import com.quickalarm.alarm.SetAlarmHandoff
import java.time.Instant
import java.time.ZoneId

/** What actually happened when the day's wake-up alarms were handed to the Clock. */
data class ScheduleSummary(
    val requested: Int,
    val succeeded: Int,
    val skipped: Int,
    val failures: List<String>,
) {
    val allSucceeded: Boolean get() = failures.isEmpty() && succeeded == requested
}

/**
 * Hands the six wake-up times to the stock Clock, one [SetAlarmHandoff] per time.
 *
 * Titles are distinct (`Fajr 1/6` ... `Fajr 6/6`) so the alarms are identifiable in the
 * Clock app and are less likely to be treated as duplicates.
 */
object PrayerScheduler {

    fun schedule(
        context: Context,
        times: PrayerTimes,
        now: Instant = Instant.now(),
        deviceZone: ZoneId = ZoneId.systemDefault(),
    ): ScheduleSummary {
        val cityZone = resolveZone(times.timezone, deviceZone)
        val wakeUps = PrayerSchedule.wakeUpTimes(
            date = times.date,
            sunrise = times.sunrise,
            cityZone = cityZone,
            deviceZone = deviceZone,
            now = now,
        )
        val total = PrayerSchedule.OFFSETS_MIN.size

        val failures = mutableListOf<String>()
        var succeeded = 0

        wakeUps.forEachIndexed { index, time ->
            val title = "Fajr ${index + 1}/$total"
            when (val result = SetAlarmHandoff.setAlarm(context, time.hour, time.minute, title)) {
                is HandoffResult.Success -> succeeded++
                is HandoffResult.NoClockApp -> failures += "No Clock app can set alarms"
                is HandoffResult.Invalid -> failures += result.message
                is HandoffResult.Failed -> failures += result.message
            }
        }

        return ScheduleSummary(
            requested = wakeUps.size,
            succeeded = succeeded,
            skipped = total - wakeUps.size,
            failures = failures,
        )
    }

    /**
     * Falls back to the device zone when the API's timezone is missing or unrecognised, so a
     * bad zone name degrades to "assume same zone" rather than failing to schedule at all.
     */
    private fun resolveZone(timezone: String?, fallback: ZoneId): ZoneId =
        if (timezone.isNullOrBlank()) {
            fallback
        } else {
            try {
                ZoneId.of(timezone)
            } catch (_: Exception) {
                fallback
            }
        }
}
