package com.quickalarm.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.quickalarm.alarm.SetAlarmHandoff
import com.quickalarm.data.AppPrefs
import com.quickalarm.prayer.PrayerLocationResult
import com.quickalarm.prayer.PrayerLocationValidator
import com.quickalarm.prayer.PrayerTimes
import com.quickalarm.prayer.PrayerWork
import com.quickalarm.ui.theme.GhostButton
import com.quickalarm.ui.theme.PrimaryButton
import com.quickalarm.ui.theme.QuickAlarmColors
import com.quickalarm.ui.theme.Reveal
import com.quickalarm.ui.theme.Spacing
import java.time.LocalDate

/** How the user identifies their location. City+country is the friendly path; coordinates
 * are the precise fallback for a place the API doesn't know by name. */
private enum class LocationMode { City, Coordinates }

/**
 * The location tab: where the user allocates the location once, and it stays until changed.
 * Saving persists the choice and triggers an immediate fetch, so the new sunrise appears
 * without waiting for the daily job.
 */
@Composable
fun LocationScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val saved = remember { AppPrefs.getPrayerLocation(context) }
    val savedByCoordinates = saved?.latitude != null && saved?.longitude != null

    var mode by remember {
        mutableStateOf(if (savedByCoordinates) LocationMode.Coordinates else LocationMode.City)
    }
    var city by remember { mutableStateOf(saved?.city.orEmpty()) }
    var country by remember { mutableStateOf(saved?.country.orEmpty()) }
    var latitude by remember { mutableStateOf(saved?.latitude?.toString().orEmpty()) }
    var longitude by remember { mutableStateOf(saved?.longitude?.toString().orEmpty()) }
    var method by remember { mutableIntStateOf(saved?.method ?: DEFAULT_METHOD) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastTimes by remember { mutableStateOf(AppPrefs.getLastPrayerTimes(context)) }

    // Re-read the stored time when returning to the screen, since the fetch runs in the
    // background and the value may have arrived while we were away.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lastTimes = AppPrefs.getLastPrayerTimes(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg),
    ) {
        Spacer(Modifier.height(Spacing.xl))

        Reveal(0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Wake-up location",
                    style = MaterialTheme.typography.titleLarge,
                    color = QuickAlarmColors.TextPrimary,
                )
                GhostButton(text = "Back", onClick = onBack)
            }
        }

        Spacer(Modifier.height(Spacing.sm))
        Reveal(1) {
            Text(
                text = "Sunrise is the Fajr deadline. Six alarms are set 30, 25, 20, 15, " +
                    "10 and 5 minutes before it, so you can wake up gradually.",
                style = MaterialTheme.typography.bodyMedium,
                color = QuickAlarmColors.TextSecondary,
            )
        }

        Spacer(Modifier.height(Spacing.lg))

        Reveal(2) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = QuickAlarmColors.Surface,
            ) {
                Column(Modifier.padding(Spacing.lg)) {
                    Text(
                        text = "Today's sunrise",
                        style = MaterialTheme.typography.titleMedium,
                        color = QuickAlarmColors.TextPrimary,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = sunriseSummary(lastTimes),
                        style = MaterialTheme.typography.headlineSmall,
                        color = QuickAlarmColors.TextPrimary,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = freshnessDetail(lastTimes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isStale(lastTimes)) {
                            QuickAlarmColors.StatusHidden
                        } else {
                            QuickAlarmColors.TextSecondary
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        Reveal(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ModeChip(
                    label = "City",
                    selected = mode == LocationMode.City,
                    onClick = { mode = LocationMode.City; error = null },
                    modifier = Modifier.weight(1f),
                )
                ModeChip(
                    label = "Coordinates",
                    selected = mode == LocationMode.Coordinates,
                    onClick = { mode = LocationMode.Coordinates; error = null },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))

        if (mode == LocationMode.City) {
            Reveal(4) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it; error = null },
                    label = { Text("City") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            Reveal(5) {
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it; error = null },
                    label = { Text("Country") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Reveal(4) {
                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it; error = null },
                    label = { Text("Latitude") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            Reveal(5) {
                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it; error = null },
                    label = { Text("Longitude") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        Reveal(6) {
            Text(
                text = "Calculation method",
                style = MaterialTheme.typography.titleMedium,
                color = QuickAlarmColors.TextPrimary,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Reveal(7) {
            MethodPicker(selected = method, onSelect = { method = it; error = null })
        }

        error?.let { message ->
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = QuickAlarmColors.StatusBlocked,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(Spacing.lg))

        Reveal(8) {
            PrimaryButton(
                text = "Save and fetch now",
                onClick = {
                    val useCoordinates = mode == LocationMode.Coordinates
                    val parsedLat = if (useCoordinates) latitude.trim().toDoubleOrNull() else null
                    val parsedLng = if (useCoordinates) longitude.trim().toDoubleOrNull() else null

                    // Distinguish "not a number" from "out of range" so the message is useful.
                    if (useCoordinates && (parsedLat == null || parsedLng == null)) {
                        error = "Enter latitude and longitude as numbers"
                        return@PrimaryButton
                    }

                    val result = PrayerLocationValidator.validate(
                        city = if (useCoordinates) null else city,
                        country = if (useCoordinates) null else country,
                        latitude = parsedLat,
                        longitude = parsedLng,
                        method = method,
                    )

                    when (result) {
                        is PrayerLocationResult.Valid -> {
                            AppPrefs.setPrayerLocation(context, result.location)
                            error = null
                            // Fetch straight away so the new time is visible without
                            // waiting for the daily job to come around.
                            PrayerWork.refreshNow(context)
                        }
                        is PrayerLocationResult.Invalid -> error = result.message
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(Spacing.sm))
        Reveal(9) {
            Text(
                text = "The time is refreshed once a day. Changing the location here " +
                    "fetches it again immediately.",
                style = MaterialTheme.typography.bodyMedium,
                color = QuickAlarmColors.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(Spacing.xl))
    }
}

/** Pill toggle for choosing how the location is described. */
@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) QuickAlarmColors.TextPrimary else QuickAlarmColors.SurfaceRaised,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) QuickAlarmColors.Background else QuickAlarmColors.TextPrimary,
            )
        }
    }
}

@Composable
private fun MethodPicker(selected: Int, onSelect: (Int) -> Unit) {
    Column {
        PrayerLocationValidator.SUPPORTED_METHODS.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                row.forEach { method ->
                    MethodChip(
                        method = method,
                        selected = method == selected,
                        onClick = { onSelect(method) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keep a lone chip in the last row at half width instead of stretching it.
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(Spacing.xs))
        }
    }
}

@Composable
private fun MethodChip(
    method: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) QuickAlarmColors.TextPrimary else QuickAlarmColors.SurfaceRaised,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = methodName(method),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) QuickAlarmColors.Background else QuickAlarmColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "method $method",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) {
                        QuickAlarmColors.Background.copy(alpha = 0.7f)
                    } else {
                        QuickAlarmColors.TextSecondary
                    },
                )
            }
        }
    }
}

/** Human-readable sunrise, or a prompt when nothing has been fetched yet. */
private fun sunriseSummary(times: PrayerTimes?): String =
    if (times == null) {
        "Not fetched yet"
    } else {
        SetAlarmHandoff.formatTime(times.sunrise.hour, times.sunrise.minute)
    }

private fun isStale(times: PrayerTimes?): Boolean =
    times == null || times.date != LocalDate.now()

private fun freshnessDetail(times: PrayerTimes?): String = when {
    times == null -> "Choose a location below and save to fetch today's time."
    times.date == LocalDate.now() -> "Updated today. The six alarms are set from this time."
    else -> "Last updated ${times.date}. Open the app with a connection to refresh it."
}

private const val DEFAULT_METHOD = 1

private fun methodName(method: Int): String = when (method) {
    1 -> "Karachi"
    2 -> "ISNA"
    3 -> "Muslim World League"
    4 -> "Umm Al-Qura"
    5 -> "Egyptian"
    7 -> "Tehran"
    8 -> "Gulf"
    9 -> "Kuwait"
    10 -> "Qatar"
    11 -> "Singapore"
    12 -> "France"
    13 -> "Diyanet"
    14 -> "Russia"
    15 -> "Moonsighting"
    16 -> "Dubai"
    99 -> "Custom"
    else -> "Method $method"
}
