package com.quickalarm.setup

/**
 * What the status screen can honestly know about the persistent notification.
 *
 * The global One UI "show content on lock screen" setting is not readable by the app,
 * so it is intentionally not represented here -- only values the app can detect
 * (runtime permission, app-level notification enablement, channel importance, and
 * the app's own hide flag) drive the state.
 */
sealed interface NotifState {
    /** The persistent notification is up. */
    data object Active : NotifState

    /** The user tapped Hide; the service is stopped and the notification is gone. */
    data object HiddenByUser : NotifState

    /**
     * The notification can't be shown. [permanent] is true when the runtime permission
     * itself is missing (the only path that needs "Open notification settings"); it is
     * false when the permission is granted but app-level notifications or the channel
     * are disabled.
     */
    data class Blocked(val permanent: Boolean) : NotifState
}

/**
 * Pure setup/status model. Maps what the app can detect to a [NotifState]. Kept free
 * of Android calls so it is JVM-testable; the activity reads the real values at the
 * edge and passes them in.
 */
object SetupState {

    fun resolve(
        permissionGranted: Boolean,
        appNotificationsOn: Boolean,
        channelBlocked: Boolean,
        hiddenByUser: Boolean,
    ): NotifState = when {
        !permissionGranted || !appNotificationsOn || channelBlocked ->
            NotifState.Blocked(permanent = !permissionGranted)
        hiddenByUser -> NotifState.HiddenByUser
        else -> NotifState.Active
    }

    /** Which top-level destination the launcher shows. Pure, for a unit test. */
    fun route(isSetupComplete: Boolean): TopScreen =
        if (isSetupComplete) TopScreen.Status else TopScreen.Setup
}

/** Top-level destination shown by [MainActivity]. */
enum class TopScreen { Setup, Status }
