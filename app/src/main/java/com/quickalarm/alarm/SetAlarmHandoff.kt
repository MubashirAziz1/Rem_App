package com.quickalarm.alarm

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

/** Which day the stock Clock will use for the next occurrence of the chosen time. */
enum class ScheduledDay { TODAY, TOMORROW }

/** Result of attempting to hand the alarm off to the stock Clock. */
sealed interface HandoffResult {
    data class Success(
        val hour: Int,
        val minute: Int,
        val title: String,
        val day: ScheduledDay,
    ) : HandoffResult

    /** No app handled ACTION_SET_ALARM. */
    data object NoClockApp : HandoffResult

    /** Caller supplied an out-of-range time or a blank title; no intent was dispatched. */
    data class Invalid(val message: String) : HandoffResult

    /** The dispatch threw (e.g. SecurityException, ActivityNotFoundException). */
    data class Failed(val message: String) : HandoffResult
}

object SetAlarmHandoff {

    /**
     * Builds and dispatches an ACTION_SET_ALARM intent with EXTRA_SKIP_UI so the
     * stock Clock creates the alarm silently. There is no return channel from the
     * Clock app, so "success" means we resolved a handler and dispatched without
     * throwing -- not a guarantee the alarm was persisted.
     *
     * Inputs are validated before any intent is built: the wheels can't produce an
     * out-of-range time, but this function is public, so it defends itself.
     */
    fun setAlarm(
        context: Context,
        hour: Int,
        minute: Int,
        title: String,
        now: LocalDateTime = LocalDateTime.now(),
    ): HandoffResult {
        validateInput(hour, minute, title)?.let { return it }
        val trimmed = title.trim()

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, trimmed)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(context.packageManager) == null) {
            return HandoffResult.NoClockApp
        }

        return try {
            context.startActivity(intent)
            HandoffResult.Success(
                hour = hour,
                minute = minute,
                title = trimmed,
                day = dayFor(hour, minute, now),
            )
        } catch (t: Throwable) {
            HandoffResult.Failed(t.message ?: t.javaClass.simpleName)
        }
    }

    /**
     * Pure input check. Returns [HandoffResult.Invalid] if the time is out of range or
     * the title is blank/whitespace, else null ("proceed"). Pure so it's JVM-testable
     * without a [Context].
     */
    fun validateInput(hour: Int, minute: Int, title: String): HandoffResult.Invalid? {
        if (validateTitle(title) == null) {
            return HandoffResult.Invalid("Enter a title for the alarm")
        }
        if (hour !in 0..23 || minute !in 0..59) {
            return HandoffResult.Invalid("Time must be between 00:00 and 23:59")
        }
        return null
    }

    /**
     * Returns the trimmed title, or null if it is blank/whitespace-only. Pure so the
     * composer can show an inline error without dispatching.
     */
    fun validateTitle(input: String): String? {
        val trimmed = input.trim()
        return if (trimmed.isEmpty()) null else trimmed
    }

    /**
     * Formats the time as `HH:mm` with ASCII digits regardless of the device locale
     * (e.g. Urdu/Arabic locales would otherwise render non-Latin digits).
     */
    fun formatTime(hour: Int, minute: Int): String =
        String.format(Locale.US, "%02d:%02d", hour, minute)

    /**
     * ACTION_SET_ALARM schedules the next occurrence of hour:minute. If that time
     * has already passed today, the alarm fires tomorrow.
     */
    fun dayFor(hour: Int, minute: Int, now: LocalDateTime): ScheduledDay {
        val target = LocalTime.of(hour, minute)
        return if (!now.toLocalTime().isBefore(target)) ScheduledDay.TOMORROW else ScheduledDay.TODAY
    }

    /** Human-readable date for the scheduled day, for confirmation copy. */
    fun dateFor(day: ScheduledDay, today: LocalDate = LocalDate.now()): LocalDate =
        if (day == ScheduledDay.TOMORROW) today.plusDays(1) else today
}
