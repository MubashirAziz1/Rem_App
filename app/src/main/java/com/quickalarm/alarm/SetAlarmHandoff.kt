package com.quickalarm.alarm

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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

    /** The dispatch threw (e.g. SecurityException, ActivityNotFoundException). */
    data class Failed(val message: String) : HandoffResult
}

object SetAlarmHandoff {

    /**
     * Builds and dispatches an ACTION_SET_ALARM intent with EXTRA_SKIP_UI so the
     * stock Clock creates the alarm silently. There is no return channel from the
     * Clock app, so "success" means we resolved a handler and dispatched without
     * throwing -- not a guarantee the alarm was persisted.
     */
    fun setAlarm(
        context: Context,
        hour: Int,
        minute: Int,
        title: String,
        now: LocalDateTime = LocalDateTime.now(),
    ): HandoffResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, title)
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
                title = title,
                day = dayFor(hour, minute, now),
            )
        } catch (t: Throwable) {
            HandoffResult.Failed(t.message ?: t.javaClass.simpleName)
        }
    }

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
