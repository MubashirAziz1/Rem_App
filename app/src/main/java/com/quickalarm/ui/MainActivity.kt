package com.quickalarm.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.quickalarm.data.AppPrefs
import com.quickalarm.service.QuickAlarmService
import com.quickalarm.setup.SetupState
import com.quickalarm.setup.TopScreen

/**
 * Hosts and routes. First launch (setup not complete) shows [SetupScreen]; every later
 * launch shows [StatusScreen]. The activity only reads the setup-complete flag, hosts
 * the composables, and wires edge callbacks (start service, persist setup, open the
 * composer). All screen logic lives in the composables.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                var top by remember {
                    mutableStateOf(
                        SetupState.route(AppPrefs.isSetupComplete(this)),
                    )
                }

                when (top) {
                    TopScreen.Setup -> SetupScreen(
                        onPermissionGranted = {
                            AppPrefs.setNotificationEnabled(this, true)
                            QuickAlarmService.start(this)
                        },
                        onSetupComplete = { AppPrefs.setSetupComplete(this) },
                        onDone = { top = TopScreen.Status },
                        onOpenNotificationSettings = {
                            SettingsIntents.openAppNotificationSettings(this)
                        },
                        onOpenLockScreenSettings = {
                            SettingsIntents.openLockScreenSettings(this)
                        },
                    )

                    TopScreen.Status -> StatusScreen(
                        onTestAlarm = {
                            startActivity(Intent(this, ComposerActivity::class.java))
                        },
                        onOpenNotificationSettings = {
                            SettingsIntents.openAppNotificationSettings(this)
                        },
                        onOpenLockScreenSettings = {
                            SettingsIntents.openLockScreenSettings(this)
                        },
                        onOpenBatteryOptimizationSettings = {
                            SettingsIntents.openBatteryOptimizationSettings(this)
                        },
                    )
                }
            }
        }
    }
}
