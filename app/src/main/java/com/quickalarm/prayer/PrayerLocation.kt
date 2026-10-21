package com.quickalarm.prayer

/**
 * The user's chosen location and calculation method, set once in the location tab and kept
 * until changed. Either [city]/[country] or [latitude]/[longitude] is populated, never both.
 */
data class PrayerLocation(
    val city: String?,
    val country: String?,
    val latitude: Double?,
    val longitude: Double?,
    val method: Int,
)

/** Result of validating the location form. */
sealed interface PrayerLocationResult {
    data class Valid(val location: PrayerLocation) : PrayerLocationResult
    data class Invalid(val message: String) : PrayerLocationResult
}

/**
 * Pure validation for the location tab. This is a system boundary: the values go into a URL,
 * so they are checked here before anything is persisted or fetched.
 */
object PrayerLocationValidator {

    /** Aladhan calculation methods. The user picks one; there is no universally correct value. */
    val SUPPORTED_METHODS = listOf(1, 2, 3, 4, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 99)

    fun validate(
        city: String?,
        country: String?,
        latitude: Double?,
        longitude: Double?,
        method: Int,
    ): PrayerLocationResult {
        if (method !in SUPPORTED_METHODS) {
            return PrayerLocationResult.Invalid("Choose a calculation method")
        }

        val hasCoordinates = latitude != null || longitude != null
        if (hasCoordinates) {
            if (latitude == null || longitude == null) {
                return PrayerLocationResult.Invalid("Enter both latitude and longitude")
            }
            if (latitude !in -90.0..90.0) {
                return PrayerLocationResult.Invalid("Latitude must be between -90 and 90")
            }
            if (longitude !in -180.0..180.0) {
                return PrayerLocationResult.Invalid("Longitude must be between -180 and 180")
            }
            return PrayerLocationResult.Valid(
                PrayerLocation(null, null, latitude, longitude, method),
            )
        }

        val trimmedCity = city?.trim().orEmpty()
        val trimmedCountry = country?.trim().orEmpty()
        if (trimmedCity.isEmpty()) {
            return PrayerLocationResult.Invalid("Enter a city")
        }
        if (trimmedCountry.isEmpty()) {
            return PrayerLocationResult.Invalid("Enter a country")
        }

        return PrayerLocationResult.Valid(
            PrayerLocation(trimmedCity, trimmedCountry, null, null, method),
        )
    }
}
