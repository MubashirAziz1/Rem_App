package com.quickalarm.prayer

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * The HTTP edge: fetches one day's prayer times from Aladhan and hands the body to
 * [PrayerTimesParser]. Uses [HttpURLConnection] so no HTTP library is added.
 *
 * Never throws. Transport failures are returned as retryable [PrayerParseResult.Failure]s
 * so the worker can decide, and a throw here would crash the fetch.
 */
object PrayerTimesClient {

    private const val BASE_URL = "https://api.aladhan.com/v1"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 10_000

    private val API_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    /**
     * Fetches the times for [date] (the device's calendar date, passed explicitly so the
     * result can never drift onto the API's own idea of "today").
     */
    fun fetch(location: PrayerLocation, date: LocalDate): PrayerParseResult {
        val url = buildUrl(location, date)
        return try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
            }

            try {
                val code = connection.responseCode
                if (code != HttpURLConnection.HTTP_OK) {
                    val retryable = code >= 500 || code == 429
                    return PrayerParseResult.Failure("HTTP $code", retryable = retryable)
                }
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                PrayerTimesParser.parse(body)
            } finally {
                connection.disconnect()
            }
        } catch (e: IOException) {
            // No network, DNS failure, timeout: all worth another attempt later.
            PrayerParseResult.Failure(e.message ?: "Network error", retryable = true)
        } catch (e: Exception) {
            PrayerParseResult.Failure(e.message ?: "Request failed")
        }
    }

    private fun buildUrl(location: PrayerLocation, date: LocalDate): String {
        val method = "method=${location.method}"
        return if (location.latitude != null && location.longitude != null) {
            val lat = location.latitude
            val lng = location.longitude
            "$BASE_URL/timings/${date.format(API_DATE)}?latitude=$lat&longitude=$lng&$method"
        } else {
            val city = URLEncoder.encode(location.city.orEmpty(), "UTF-8")
            val country = URLEncoder.encode(location.country.orEmpty(), "UTF-8")
            "$BASE_URL/timingsByCity/${date.format(API_DATE)}?city=$city&country=$country&$method"
        }
    }
}
