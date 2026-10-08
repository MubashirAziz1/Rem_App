package com.quickalarm

import android.app.Application
import com.quickalarm.notif.Notifications

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannel(this)
    }
}
