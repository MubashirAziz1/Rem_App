package com.quickalarm.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import com.quickalarm.data.AppPrefs
import com.quickalarm.service.QuickAlarmService
import com.quickalarm.setup.NotifState
import com.quickalarm.setup.NotificationStatus
import com.quickalarm.ui.theme.Motion
import com.quickalarm.ui.theme.PrimaryButton
import com.quickalarm.ui.theme.QuickAlarmColors
import com.quickalarm.ui.theme.Reveal
import com.quickalarm.ui.theme.SecondaryButton
import com.quickalarm.ui.theme.Spacing
import com.quickalarm.ui.theme.StatusDot

/**
 * The status screen shown on every launch after setup. Reflects only what the app can
 * detect, re-reads on resume so returning from system settings updates it, and offers
 * Hide/Show, "Fix in settings", and "Test alarm". The troubleshooting section is
 * collapsed by default and holds the optional Samsung battery and lock-screen help
 * only. No overlay or battery permission is ever requested.
 */
@Composable
fun StatusScreen(
    onTestAlarm: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenLockScreenSettings: () -> Unit,
    onOpenBatteryOptimizationSettings: () -> Unit,
    onOpenLocation: () -> Unit = {},
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(NotificationStatus.current(context)) }
    var troubleshootingOpen by remember { mutableStateOf(false) }
    var lastTimes by remember { mutableStateOf(AppPrefs.getLastPrayerTimes(context)) }
    var lastSchedule by remember { mutableStateOf(AppPrefs.getLastSchedule(context)) }

    // Re-read on resume so toggling system settings is reflected when the user returns.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state = NotificationStatus.current(context)
                lastTimes = AppPrefs.getLastPrayerTimes(context)
                lastSchedule = AppPrefs.getLastSchedule(context)
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
            Text(
                text = "Quick Alarm",
                style = MaterialTheme.typography.titleLarge,
                color = QuickAlarmColors.TextPrimary,
            )
        }
        Spacer(Modifier.height(Spacing.lg))

        Reveal(1) {
            StatusCard(
                state = state,
                onHide = {
                    QuickAlarmService.hide(context)
                    // Optimistic update so the UI reflects Hide immediately; the async
                    // pref write would otherwise leave the screen showing "Active" until
                    // the next resume. ON_RESUME re-derives from prefs to stay correct.
                    state = NotifState.HiddenByUser
                },
                onShow = {
                    QuickAlarmService.start(context)
                    state = NotifState.Active
                },
                onFix = onOpenNotificationSettings,
            )
        }

        Spacer(Modifier.height(Spacing.md))
        Reveal(2) {
            PrimaryButton(
                text = "Test alarm",
                onClick = onTestAlarm,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(Spacing.sm))
        Reveal(3) {
            WakeUpCard(times = lastTimes, schedule = lastSchedule, onClick = onOpenLocation)
        }

        Spacer(Modifier.height(Spacing.xl))
        Reveal(4) {
            TroubleshootingHeader(
                open = troubleshootingOpen,
                onToggle = { troubleshootingOpen = !troubleshootingOpen },
            )
        }
        AnimatedVisibility(
            visible = troubleshootingOpen,
            enter = fadeIn(tween(Motion.DurationMs, easing = Motion.Fluid)) +
                expandVertically(tween(Motion.DurationMs, easing = Motion.Fluid)),
            exit = fadeOut(tween(Motion.ExitMs)) +
                shrinkVertically(tween(Motion.ExitMs)),
        ) {
            Column(Modifier.padding(top = Spacing.sm)) {
                TroubleshootItem(
                    title = "Battery: set Quick Alarm to Unrestricted",
                    desc = "One UI can put sleeping apps' foreground services to sleep. " +
                        "Setting Quick Alarm to Unrestricted keeps the notification alive.",
                    buttonText = "Battery settings",
                    onClick = onOpenBatteryOptimizationSettings,
                )
                Spacer(Modifier.height(Spacing.sm))
                TroubleshootItem(
                    title = "Lock screen: show content",
                    desc = "If the notification isn't visible on the lock screen, set lock " +
                        "screen notifications to show content so the shortcut is tappable.",
                    buttonText = "Open lock screen settings",
                    onClick = onOpenLockScreenSettings,
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))
    }
}

@Composable
private fun StatusCard(
    state: NotifState,
    onHide: () -> Unit,
    onShow: () -> Unit,
    onFix: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = QuickAlarmColors.Surface,
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(color = statusColor(state))
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = statusTitle(state),
                    style = MaterialTheme.typography.titleLarge,
                    color = QuickAlarmColors.TextPrimary,
                )
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = statusDetail(state),
                style = MaterialTheme.typography.bodyMedium,
                color = QuickAlarmColors.TextSecondary,
            )
            Spacer(Modifier.height(Spacing.md))
            when (state) {
                is NotifState.Active -> SecondaryButton(text = "Hide notification", onClick = onHide)
                is NotifState.HiddenByUser -> PrimaryButton(text = "Show notification", onClick = onShow)
                is NotifState.Blocked -> PrimaryButton(text = "Fix in settings", onClick = onFix)
            }
        }
    }
}

@Composable
private fun WakeUpCard(
    times: com.quickalarm.prayer.PrayerTimes?,
    schedule: AppPrefs.LastSchedule?,
    onClick: () -> Unit,
) {
    val stale = times == null || times.date != java.time.LocalDate.now()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = QuickAlarmColors.Surface,
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Text(
                text = "Gradual wake-up",
                style = MaterialTheme.typography.titleMedium,
                color = QuickAlarmColors.TextPrimary,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                // Report what the Clock was actually given. Never claim six alarms just
                // because a time is stored -- that is what made the old copy misleading.
                text = wakeUpSummary(times, schedule),
                style = MaterialTheme.typography.bodyMedium,
                color = QuickAlarmColors.TextSecondary,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = when {
                    times == null -> "Tap to choose a location"
                    stale -> "Last updated ${times.date}. Tap to refresh."
                    else -> "Updated today. Tap to change location"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (stale) QuickAlarmColors.StatusHidden else QuickAlarmColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun TroubleshootingHeader(
    open: Boolean,
    onToggle: () -> Unit,
) {
    // The "+" rotates 45 degrees into a close mark when the section expands.
    val rotation by animateFloatAsState(
        targetValue = if (open) 45f else 0f,
        animationSpec = tween(Motion.DurationMs, easing = Motion.Fluid),
        label = "troubleshootToggle",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Notification not showing or keeps disappearing?",
            style = MaterialTheme.typography.titleMedium,
            color = QuickAlarmColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = "+",
            style = MaterialTheme.typography.titleLarge,
            color = QuickAlarmColors.TextSecondary,
            modifier = Modifier.graphicsLayer { rotationZ = rotation },
        )
    }
}

@Composable
private fun TroubleshootItem(
    title: String,
    desc: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = QuickAlarmColors.SurfaceRaised,
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = QuickAlarmColors.TextPrimary,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = QuickAlarmColors.TextSecondary,
            )
            Spacer(Modifier.height(Spacing.sm))
            SecondaryButton(text = buttonText, onClick = onClick)
        }
    }
}

/**
 * The truthful one-line status for the wake-up card, built only from facts we recorded:
 * the fetched time and the actual dispatch result. Shows "not set yet" until a fetch has
 * both succeeded *and* reported how many alarms reached the Clock.
 */
private fun wakeUpSummary(
    times: com.quickalarm.prayer.PrayerTimes?,
    schedule: AppPrefs.LastSchedule?,
): String = when {
    times == null -> "Set your city to fetch sunrise and schedule the alarms."
    schedule == null -> "Fetched, but no alarms set yet."
    schedule.succeeded == 0 ->
        "No alarms were set. Open the Clock app, then tap to try again."
    else -> {
        val when_ = if (schedule.tomorrow) "tomorrow" else "today"
        "${schedule.succeeded} alarm${if (schedule.succeeded == 1) "" else "s"} set for $when_."
    }
}

private fun statusColor(state: NotifState) = when (state) {
    is NotifState.Active -> QuickAlarmColors.StatusActive
    is NotifState.HiddenByUser -> QuickAlarmColors.StatusHidden
    is NotifState.Blocked -> QuickAlarmColors.StatusBlocked
}

private fun statusTitle(state: NotifState): String = when (state) {
    is NotifState.Active -> "Active"
    is NotifState.HiddenByUser -> "Hidden by you"
    is NotifState.Blocked -> "Notifications blocked"
}

private fun statusDetail(state: NotifState): String = when (state) {
    is NotifState.Active -> "The Quick Alarm notification is on your lock screen. Tap it to create a titled alarm."
    is NotifState.HiddenByUser -> "You hid the notification. Tap \"Show notification\" to bring it back."
    is NotifState.Blocked -> "The notification can't show. Open notification settings to allow it."
}
