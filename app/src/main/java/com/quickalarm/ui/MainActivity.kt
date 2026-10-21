package com.quickalarm.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.quickalarm.data.AppPrefs
import com.quickalarm.prayer.PrayerWork
import com.quickalarm.service.QuickAlarmService
import com.quickalarm.setup.SetupState
import com.quickalarm.setup.TopScreen
import com.quickalarm.ui.theme.Motion
import com.quickalarm.ui.theme.QuickAlarmTheme
import kotlinx.coroutines.delay

/**
 * Hosts and routes. Every launch starts on [WelcomeScreen] while the app initializes;
 * then first launch (setup not complete) routes to [SetupScreen] and every later
 * launch routes to [StatusScreen]. The activity only reads the setup-complete flag,
 * hosts the composables, and wires edge callbacks (start service, persist setup, open
 * the composer). All screen logic lives in the composables.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the daily sunrise fetch registered, and refresh now so the stored time is
        // current whenever the app is opened. Both no-op safely until a location is saved.
        PrayerWork.ensureDaily(this)
        PrayerWork.refreshNow(this)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        setContent {
            QuickAlarmTheme {
                // null = still on the welcome/loading screen.
                var top by remember { mutableStateOf<TopScreen?>(null) }

                LaunchedEffect(Unit) {
                    delay(WELCOME_MIN_MS.toLong())
                    top = SetupState.route(AppPrefs.isSetupComplete(this@MainActivity))
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AnimatedContent(
                        targetState = top,
                        transitionSpec = {
                            fadeIn(tween(Motion.DurationMs, easing = Motion.Fluid)) togetherWith
                                fadeOut(tween(Motion.ExitMs))
                        },
                        label = "topLevel",
                    ) { destination ->
                        when (destination) {
                            null -> WelcomeScreen()

                            TopScreen.Setup -> SetupScreen(
                                onPermissionGranted = {
                                    AppPrefs.setNotificationEnabled(this@MainActivity, true)
                                    QuickAlarmService.start(this@MainActivity)
                                },
                                onSetupComplete = { AppPrefs.setSetupComplete(this@MainActivity) },
                                onDone = { top = TopScreen.Status },
                                onOpenNotificationSettings = {
                                    SettingsIntents.openAppNotificationSettings(this@MainActivity)
                                },
                                onOpenLockScreenSettings = {
                                    SettingsIntents.openLockScreenSettings(this@MainActivity)
                                },
                            )

                            TopScreen.Status -> StatusScreen(
                                onTestAlarm = {
                                    startActivity(Intent(this@MainActivity, ComposerActivity::class.java))
                                },
                                onOpenNotificationSettings = {
                                    SettingsIntents.openAppNotificationSettings(this@MainActivity)
                                },
                                onOpenLockScreenSettings = {
                                    SettingsIntents.openLockScreenSettings(this@MainActivity)
                                },
                                onOpenBatteryOptimizationSettings = {
                                    SettingsIntents.openBatteryOptimizationSettings(this@MainActivity)
                                },
                                onOpenLocation = { top = TopScreen.Location },
                            )

                            TopScreen.Location -> LocationScreen(
                                onBack = { top = TopScreen.Status },
                            )
                        }
                    }
                }
            }
        }
    }
}
