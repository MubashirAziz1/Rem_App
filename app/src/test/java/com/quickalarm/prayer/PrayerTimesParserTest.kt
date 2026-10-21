package com.quickalarm.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * The Aladhan response is untrusted third-party input: it can be truncated, error-shaped,
 * or carry an impossible time. Every failure mode must return a typed failure and never
 * throw, because a throw inside the worker would crash the fetch.
 */
class PrayerTimesParserTest {

    private val validJson = """
        {
          "code": 200,
          "status": "OK",
          "data": {
            "timings": {
              "Fajr": "04:05",
              "Sunrise": "05:54",
              "Dhuhr": "13:04",
              "Asr": "17:00",
              "Maghrib": "20:13",
              "Isha": "22:01"
            },
            "date": {
              "readable": "21 Oct 2026",
              "gregorian": { "date": "21-10-2026" }
            },
            "meta": { "timezone": "Asia/Karachi" }
          }
        }
    """.trimIndent()

    @Test
    fun `parses a valid response into date, sunrise and timezone`() {
        val result = PrayerTimesParser.parse(validJson)

        assertTrue("expected success, got $result", result is PrayerParseResult.Success)
        val times = (result as PrayerParseResult.Success).times
        assertEquals(LocalTime.of(5, 54), times.sunrise)
        assertEquals(LocalDate.of(2026, 10, 21), times.date)
        assertEquals("Asia/Karachi", times.timezone)
    }

    @Test
    fun `returns a failure when sunrise is missing`() {
        val json = validJson.replace("\"Sunrise\": \"05:54\",", "")

        val result = PrayerTimesParser.parse(json)

        assertTrue("expected failure, got $result", result is PrayerParseResult.Failure)
    }

    @Test
    fun `returns a failure for a non-200 code`() {
        val json = validJson.replace("\"code\": 200", "\"code\": 400")

        val result = PrayerTimesParser.parse(json)

        assertTrue("expected failure, got $result", result is PrayerParseResult.Failure)
    }

    @Test
    fun `returns a failure for malformed json`() {
        val result = PrayerTimesParser.parse("{ this is not json")

        assertTrue("expected failure, got $result", result is PrayerParseResult.Failure)
    }

    @Test
    fun `returns a failure for an out-of-range sunrise`() {
        val json = validJson.replace("\"Sunrise\": \"05:54\"", "\"Sunrise\": \"25:99\"")

        val result = PrayerTimesParser.parse(json)

        assertTrue("expected failure, got $result", result is PrayerParseResult.Failure)
    }

    @Test
    fun `returns a failure for a sunrise that is not a time`() {
        val json = validJson.replace("\"Sunrise\": \"05:54\"", "\"Sunrise\": \"sunrise\"")

        val result = PrayerTimesParser.parse(json)

        assertTrue("expected failure, got $result", result is PrayerParseResult.Failure)
    }

    @Test
    fun `returns a failure when the gregorian date is missing`() {
        val json = validJson.replace("""{ "date": "21-10-2026" }""", "{}")

        val result = PrayerTimesParser.parse(json)

        assertTrue("expected failure, got $result", result is PrayerParseResult.Failure)
    }

    @Test
    fun `accepts a null timezone without failing`() {
        val json = validJson.replace("""{ "timezone": "Asia/Karachi" }""", "{}")

        val result = PrayerTimesParser.parse(json)

        assertTrue("expected success, got $result", result is PrayerParseResult.Success)
        assertEquals(null, (result as PrayerParseResult.Success).times.timezone)
    }

    @Test
    fun `returns a failure for an empty body`() {
        val result = PrayerTimesParser.parse("")

        assertTrue("expected failure, got $result", result is PrayerParseResult.Failure)
    }

    @Test
    fun `marks a server-side error as retryable`() {
        val json = validJson.replace("\"code\": 200", "\"code\": 500")

        val result = PrayerTimesParser.parse(json) as PrayerParseResult.Failure

        assertTrue("a 5xx should be retryable", result.retryable)
    }

    @Test
    fun `marks a rate-limit error as retryable`() {
        val json = validJson.replace("\"code\": 200", "\"code\": 429")

        val result = PrayerTimesParser.parse(json) as PrayerParseResult.Failure

        assertTrue("a 429 should be retryable", result.retryable)
    }

    @Test
    fun `marks a bad-request error as not retryable`() {
        val json = validJson.replace("\"code\": 200", "\"code\": 400")

        val result = PrayerTimesParser.parse(json) as PrayerParseResult.Failure

        assertTrue("a 4xx should not be retried", !result.retryable)
    }

    @Test
    fun `marks a malformed body as not retryable`() {
        val result = PrayerTimesParser.parse("{ this is not json") as PrayerParseResult.Failure

        assertTrue("bad data should not be retried", !result.retryable)
    }

    /**
     * A real response captured from the live API on 2026-10-21. This guards against the
     * documented shape drifting from the actual one, which a hand-written fixture would miss.
     */
    @Test
    fun `parses a real captured API response`() {
        val realJson = """
            {"code":200,"status":"OK","data":{"timings":{"Fajr":"04:49","Sunrise":"06:10",
            "Dhuhr":"11:47","Asr":"15:00","Sunset":"17:24","Maghrib":"17:24","Isha":"18:46",
            "Imsak":"04:39","Midnight":"23:47","Firstthird":"21:40","Lastthird":"01:55"},
            "date":{"readable":"21 Oct 2026","timestamp":"1792548000","hijri":{"date":"10-05-1448",
            "format":"DD-MM-YYYY","day":"10","weekday":{"en":"Al Arba'a","ar":"\u0627\u0644\u0623\u0631\u0628\u0639\u0627\u0621"},
            "month":{"number":5,"en":"Jum\u0101d\u00e1 al-\u016bl\u00e1","ar":"\u062c\u0645\u0627\u062f\u0649",
            "days":30},"year":"1448","designation":{"abbreviated":"AH","expanded":"Anno Hegirae"},
            "holidays":[],"adjustedHolidays":[],"method":"HJCoSA"},
            "gregorian":{"date":"21-10-2026","format":"DD-MM-YYYY","day":"21",
            "weekday":{"en":"Wednesday"},"month":{"number":10,"en":"October"},"year":"2026",
            "designation":{"abbreviated":"AD","expanded":"Anno Domini"},"lunarSighting":false}},
            "meta":{"latitude":8.8888888,"longitude":7.7777777,"timezone":"Asia/Karachi",
            "method":{"id":1,"name":"University of Islamic Sciences, Karachi",
            "params":{"Fajr":18,"Isha":18},"location":{"latitude":24.8614622,"longitude":67.0099388}},
            "latitudeAdjustmentMethod":"ANGLE_BASED","midnightMode":"STANDARD","school":"STANDARD",
            "offset":{"Imsak":0,"Fajr":0,"Sunrise":0,"Dhuhr":0,"Asr":0,"Maghrib":0,"Sunset":0,
            "Isha":0,"Midnight":0}}}}
        """.trimIndent().replace("\n", "")

        val result = PrayerTimesParser.parse(realJson)

        assertTrue("expected success, got $result", result is PrayerParseResult.Success)
        val times = (result as PrayerParseResult.Success).times
        assertEquals(LocalTime.of(6, 10), times.sunrise)
        assertEquals(LocalDate.of(2026, 10, 21), times.date)
        assertEquals("Asia/Karachi", times.timezone)
    }
}
