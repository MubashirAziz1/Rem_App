package com.quickalarm.prayer

import android.content.Context
import com.quickalarm.alarm.HandoffResult
import com.quickalarm.alarm.ScheduledDay
import com.quickalarm.alarm.SetAlarmHandoff
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** What actually happened when the day's wake-up alarms were handed to the Clock. */
data class ScheduleSummary(
    val requested: Int,
    val succeeded: Int,
    val failures: List<String>,
    /** When the scheduled window actually lands. */
    val day: ScheduledDay,
    /** How many alarms the full day contains ([PrayerSchedule.OFFSETS_MIN] size). */
    val total: Int,
) {
    val allSucceeded: Boolean get() = failures.isEmpty() && succeeded == requested
}

/**
 * Hands the six wake-up times to the stock Clock, one [SetAlarmHandoff] per time.
 *
 * Titles are distinct (`Fajr 1/6` ... `Fajr 6/6`) so the alarms are identifiable in the
 * Clock app and are less likely to be treated as duplicates.
 *
 * **The dispatches are spaced out on purpose.** `ACTION_SET_ALARM` is an *Activity* action,
 * so each hand-off starts the Clock app. When several arrive in one tight burst the stock
 * Clock accepts the first and silently drops the rest, which is why dispatching all six in
 * a loop produced only the first alarm. Waiting [DISPATCH_GAP_MS] between hand-offs lets the
 * Clock finish handling each one before the next arrives.
 */
object PrayerScheduler {

    /**
     * Gap between successive hand-offs. Community reports of this exact symptom settle at
     * "works when rate-limited to about one per second"; 2s leaves margin for a slower Clock
     * app (Samsung One UI on the target A72) without approaching a worker's run-time budget.
     */
    const val DISPATCH_GAP_MS = 2_000L

    suspend fun schedule(
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
        val day = dayFor(wakeUps, deviceZone, now)

        val results = dispatchSequentially(
            times = wakeUps,
            total = total,
            gapMs = DISPATCH_GAP_MS,
            wait = { delay(it) },
        ) { hour, minute, title ->
            SetAlarmHandoff.setAlarm(context, hour, minute, title)
        }

        return summarize(results, total, day)
    }

    /**
     * Which day the first wake-up lands on, in the **device's** zone -- the same frame the
     * Clock uses to decide today vs tomorrow. Derived from the actual chosen times rather
     * than assumed, so the status screen can state it accurately.
     */
    internal fun dayFor(
        wakeUps: List<LocalTime>,
        deviceZone: ZoneId,
        now: Instant,
    ): ScheduledDay {
        val first = wakeUps.firstOrNull() ?: return ScheduledDay.TODAY
        // LocalTime carries no date, so compare on the same calendar day as `now` in the
        // device's zone -- the frame the Clock itself uses.
        val candidate = first.atDate(now.atZone(deviceZone).toLocalDate())
            .atZone(deviceZone)
            .toInstant()
        return if (candidate.isAfter(now)) ScheduledDay.TODAY else ScheduledDay.TOMORROW
    }

    /**
     * Dispatches one hand-off per time, in firing order, waiting [gapMs] before every
     * dispatch after the first. Split out from [schedule] with the wait and the hand-off
     * injected so the *spacing policy* -- the actual fix for the dropped alarms -- is
     * unit-testable without a device or real delays.
     */
    internal suspend fun dispatchSequentially(
        times: List<LocalTime>,
        total: Int,
        gapMs: Long,
        wait: suspend (Long) -> Unit,
        handOff: (hour: Int, minute: Int, title: String) -> HandoffResult,
    ): List<HandoffResult> {
        val results = mutableListOf<HandoffResult>()
        times.forEachIndexed { index, time ->
            // Never wait before the first hand-off; only between them.
            if (index > 0) wait(gapMs)
            val title = "Fajr ${index + 1}/$total"
            results += handOff(time.hour, time.minute, title)
        }
        return results
    }

    /** Reduces the per-alarm results into the summary shown to the user. Pure. */
    internal fun summarize(
        results: List<HandoffResult>,
        total: Int,
        day: ScheduledDay = ScheduledDay.TODAY,
    ): ScheduleSummary {
        var succeeded = 0
        val failures = mutableListOf<String>()
        for (result in results) {
            when (result) {
                is HandoffResult.Success -> succeeded++
                is HandoffResult.NoClockApp -> failures += "No Clock app can set alarms"
                is HandoffResult.Invalid -> failures += result.message
                is HandoffResult.Failed -> failures += result.message
            }
        }
        return ScheduleSummary(
            requested = results.size,
            succeeded = succeeded,
            failures = failures,
            day = day,
            total = total,
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
