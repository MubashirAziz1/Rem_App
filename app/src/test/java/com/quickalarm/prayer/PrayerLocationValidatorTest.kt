package com.quickalarm.prayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validation for the location tab's input. This is a system boundary: whatever the user
 * types goes straight into a URL, so it is validated here before anything is persisted
 * or fetched. Pure, so it is testable without a device.
 */
class PrayerLocationValidatorTest {

    @Test
    fun `accepts a city and country`() {
        val result = PrayerLocationValidator.validate(
            city = "Lahore",
            country = "Pakistan",
            latitude = null,
            longitude = null,
            method = 1,
        )

        assertTrue("expected valid, got $result", result is PrayerLocationResult.Valid)
        val location = (result as PrayerLocationResult.Valid).location
        assertEquals("Lahore", location.city)
        assertEquals("Pakistan", location.country)
        assertEquals(1, location.method)
    }

    @Test
    fun `trims surrounding whitespace from city and country`() {
        val result = PrayerLocationValidator.validate(
            city = "  Lahore  ",
            country = "  Pakistan ",
            latitude = null,
            longitude = null,
            method = 1,
        )

        val location = (result as PrayerLocationResult.Valid).location
        assertEquals("Lahore", location.city)
        assertEquals("Pakistan", location.country)
    }

    @Test
    fun `rejects a blank city`() {
        val result = PrayerLocationValidator.validate("   ", "Pakistan", null, null, 1)

        assertTrue("expected invalid, got $result", result is PrayerLocationResult.Invalid)
    }

    @Test
    fun `rejects a blank country`() {
        val result = PrayerLocationValidator.validate("Lahore", "", null, null, 1)

        assertTrue("expected invalid, got $result", result is PrayerLocationResult.Invalid)
    }

    @Test
    fun `rejects an unknown calculation method`() {
        val result = PrayerLocationValidator.validate("Lahore", "Pakistan", null, null, 42)

        assertTrue("expected invalid, got $result", result is PrayerLocationResult.Invalid)
    }

    @Test
    fun `accepts coordinates instead of a city`() {
        val result = PrayerLocationValidator.validate(
            city = null,
            country = null,
            latitude = 31.5497,
            longitude = 74.3436,
            method = 1,
        )

        assertTrue("expected valid, got $result", result is PrayerLocationResult.Valid)
        val location = (result as PrayerLocationResult.Valid).location
        assertEquals(31.5497, location.latitude!!, 0.0001)
        assertEquals(74.3436, location.longitude!!, 0.0001)
    }

    @Test
    fun `accepts the coordinate boundaries`() {
        val result = PrayerLocationValidator.validate(null, null, 90.0, 180.0, 1)
        assertTrue("expected valid, got $result", result is PrayerLocationResult.Valid)

        val negative = PrayerLocationValidator.validate(null, null, -90.0, -180.0, 1)
        assertTrue("expected valid, got $negative", negative is PrayerLocationResult.Valid)
    }

    @Test
    fun `rejects out-of-range coordinates`() {
        val tooHigh = PrayerLocationValidator.validate(null, null, 90.1, 0.0, 1)
        assertTrue("expected invalid, got $tooHigh", tooHigh is PrayerLocationResult.Invalid)

        val tooLow = PrayerLocationValidator.validate(null, null, 0.0, -180.1, 1)
        assertTrue("expected invalid, got $tooLow", tooLow is PrayerLocationResult.Invalid)
    }

    @Test
    fun `rejects when neither a city nor coordinates are given`() {
        val result = PrayerLocationValidator.validate(null, null, null, null, 1)

        assertTrue("expected invalid, got $result", result is PrayerLocationResult.Invalid)
    }

    @Test
    fun `rejects a half-specified coordinate pair`() {
        val result = PrayerLocationValidator.validate(null, null, 31.5, null, 1)

        assertTrue("expected invalid, got $result", result is PrayerLocationResult.Invalid)
    }

    @Test
    fun `accepts every supported calculation method`() {
        for (method in PrayerLocationValidator.SUPPORTED_METHODS) {
            val result = PrayerLocationValidator.validate("Lahore", "Pakistan", null, null, method)
            assertTrue("method $method should be valid, got $result", result is PrayerLocationResult.Valid)
        }
    }
}
