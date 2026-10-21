package com.quickalarm.prayer

import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Parses an Aladhan `/timings` response into [PrayerTimes].
 *
 * The response is untrusted third-party input, so every failure mode returns
 * [PrayerParseResult.Failure] rather than throwing: a throw here would crash the worker.
 * `Sunrise` is range-checked before it is trusted, because a bad value would otherwise be
 * scheduled as a real alarm.
 */
object PrayerTimesParser {

    private val API_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    fun parse(json: String): PrayerParseResult {
        if (json.isBlank()) return PrayerParseResult.Failure("Empty response body")

        return try {
            val root = JSONObject(json)

            val code = root.optInt("code", -1)
            if (code != 200) {
                // 5xx and 429 are server-side/transient; 4xx means our request was wrong.
                val retryable = code >= 500 || code == 429
                return PrayerParseResult.Failure("API returned code $code", retryable = retryable)
            }

            val data = root.optJSONObject("data")
                ?: return PrayerParseResult.Failure("Response has no data object")

            val timings = data.optJSONObject("timings")
                ?: return PrayerParseResult.Failure("Response has no timings object")

            val sunrise = parseTime(timings.optString("Sunrise", ""))
                ?: return PrayerParseResult.Failure("Missing or invalid Sunrise")

            val dateRaw = data.optJSONObject("date")
                ?.optJSONObject("gregorian")
                ?.optString("date", "")
                .orEmpty()
            val date = parseDate(dateRaw)
                ?: return PrayerParseResult.Failure("Missing or invalid Gregorian date")

            val timezone = data.optJSONObject("meta")
                ?.optString("timezone", "")
                ?.takeIf { it.isNotBlank() }

            PrayerParseResult.Success(PrayerTimes(date = date, sunrise = sunrise, timezone = timezone))
        } catch (e: Exception) {
            // Only the JSON parse itself can throw here; the `opt*` accessors return defaults.
            // Catching Exception (not Throwable) keeps real errors like OutOfMemoryError visible.
            PrayerParseResult.Failure(e.message ?: "Malformed response")
        }
    }

    /**
     * Parses `"HH:mm"` into a [LocalTime], or null if it is missing or out of range.
     * Aladhan sometimes appends a zone suffix (e.g. `"05:54 (PKT)"`), so anything after
     * the first space is dropped.
     */
    private fun parseTime(raw: String): LocalTime? {
        val cleaned = raw.substringBefore(' ').trim()
        val parts = cleaned.split(':')
        if (parts.size != 2) return null

        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null

        return LocalTime.of(hour, minute)
    }

    private fun parseDate(raw: String): LocalDate? = try {
        LocalDate.parse(raw.trim(), API_DATE)
    } catch (_: Throwable) {
        null
    }
}
