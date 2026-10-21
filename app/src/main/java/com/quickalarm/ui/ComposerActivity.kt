package com.quickalarm.ui

import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.quickalarm.alarm.SetAlarmHandoff
import com.quickalarm.ui.theme.QuickAlarmTheme

/**
 * Composer shown over the keyguard. Uses showWhenLocked/turnScreenOn so it renders
 * without unlocking, which is the whole point: the user should be able to pick a
 * time and type a title without entering their PIN.
 *
 * We deliberately do NOT call requestDismissKeyguard on open, because on a secure
 * device that pops the PIN bouncer and breaks the "no unlock" flow. Free-text IME
 * over the keyguard can be flaky on some Samsung builds; if the user hits that, they
 * can tap "Unlock to type" (see [unlockToType]) which asks the keyguard to dismiss.
 */
class ComposerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLockedCompat()
        // Let IME insets reach the sheet.
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        enableEdgeToEdge()
        setContent {
            QuickAlarmTheme {
                ComposerScreen(
                    onConfirm = { hour, minute, title ->
                        SetAlarmHandoff.setAlarm(
                            context = this,
                            hour = hour,
                            minute = minute,
                            title = title,
                        )
                    },
                    onDismiss = { finish() },
                    onUnlockToType = { unlockToType() },
                )
            }
        }
    }

    private fun setShowWhenLockedCompat() {
        // minSdk is 33 (>= O_MR1), so the platform APIs are always available.
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    }

    /**
     * Fallback for OEMs where the soft keyboard won't work over the keyguard: ask the
     * keyguard to dismiss (this shows the PIN bouncer on a secure device). Only invoked
     * when the user explicitly taps "Unlock to type".
     */
    private fun unlockToType() {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return
        if (km.isKeyguardLocked) {
            km.requestDismissKeyguard(this, null)
        }
    }
}
