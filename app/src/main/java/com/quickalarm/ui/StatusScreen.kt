package com.quickalarm.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.quickalarm.service.QuickAlarmService
import com.quickalarm.setup.NotifState
import com.quickalarm.setup.NotificationStatus

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
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(NotificationStatus.current(context)) }
    var troubleshootingOpen by remember { mutableStateOf(false) }

    // Re-read on resume so toggling system settings is reflected when the user returns.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state = NotificationStatus.current(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun refresh() {
        state = NotificationStatus.current(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("Quick Alarm", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = statusTitle(state),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(statusDetail(state), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (state) {
                        is NotifState.Active -> OutlinedButton(onClick = {
                            QuickAlarmService.hide(context)
                            refresh()
                        }) { Text("Hide") }
                        is NotifState.HiddenByUser -> Button(onClick = {
                            QuickAlarmService.start(context)
                            refresh()
                        }) { Text("Show notification") }
                        is NotifState.Blocked -> Button(onClick = onOpenNotificationSettings) {
                            Text("Fix in settings")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = onTestAlarm, modifier = Modifier.fillMaxWidth()) { Text("Test alarm") }

        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { troubleshootingOpen = !troubleshootingOpen }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Notification not showing or keeps disappearing?",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(if (troubleshootingOpen) "−" else "+")
        }
        AnimatedVisibility(visible = troubleshootingOpen) {
            Column(Modifier.padding(top = 8.dp)) {
                TroubleshootItem(
                    title = "Battery: set Quick Alarm to Unrestricted",
                    desc = "One UI can put sleeping apps' foreground services to sleep. " +
                        "Setting Quick Alarm to Unrestricted keeps the notification alive.",
                    buttonText = "Battery settings",
                    onClick = onOpenBatteryOptimizationSettings,
                )
                TroubleshootItem(
                    title = "Lock screen: show content",
                    desc = "If the notification isn't visible on the lock screen, set lock " +
                        "screen notifications to show content so the shortcut is tappable.",
                    buttonText = "Open lock screen settings",
                    onClick = onOpenLockScreenSettings,
                )
            }
        }
    }
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

@Composable
private fun TroubleshootItem(
    title: String,
    desc: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(desc, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onClick) { Text(buttonText) }
        }
    }
}
