package com.quickalarm.prayer

import java.time.LocalDate
import java.time.LocalTime

/**
 * The one prayer-time value this app needs: the day's sunrise, which doubles as the Fajr
 * deadline ("qaza" cutoff). [timezone] is the city's IANA zone as reported by the API, used
 * to convert the instant into the device's zone before scheduling.
 */
data class PrayerTimes(
    val date: LocalDate,
    val sunrise: LocalTime,
    val timezone: String?,
)

/** Result of parsing an API response. The body is untrusted, so failures are values, not throws. */
sealed interface PrayerParseResult {
    data class Success(val times: PrayerTimes) : PrayerParseResult

    /**
     * [retryable] tells the worker whether re-running could plausibly succeed. Transport and
     * server-side problems are worth retrying; bad data or a rejected request is not.
     */
    data class Failure(val message: String, val retryable: Boolean = false) : PrayerParseResult
}
