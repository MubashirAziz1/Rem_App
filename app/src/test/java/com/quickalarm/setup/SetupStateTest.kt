package com.quickalarm.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SetupStateTest {

    private data class Case(
        val permissionGranted: Boolean,
        val appNotificationsOn: Boolean,
        val channelBlocked: Boolean,
        val hiddenByUser: Boolean,
        val expected: NotifState,
    )

    /**
     * All 16 combinations of (permissionGranted, appNotificationsOn, channelBlocked,
     * hiddenByUser) -> expected NotifState. Table-driven so every input is covered and
     * the precedence is explicit.
     */
    private val table = listOf(
        Case(false, false, false, false, NotifState.Blocked(permanent = true)),
        Case(false, false, false, true, NotifState.Blocked(permanent = true)),
        Case(false, false, true, false, NotifState.Blocked(permanent = true)),
        Case(false, false, true, true, NotifState.Blocked(permanent = true)),
        Case(false, true, false, false, NotifState.Blocked(permanent = true)),
        Case(false, true, false, true, NotifState.Blocked(permanent = true)),
        Case(false, true, true, false, NotifState.Blocked(permanent = true)),
        Case(false, true, true, true, NotifState.Blocked(permanent = true)),
        Case(true, false, false, false, NotifState.Blocked(permanent = false)),
        Case(true, false, false, true, NotifState.Blocked(permanent = false)),
        Case(true, false, true, false, NotifState.Blocked(permanent = false)),
        Case(true, false, true, true, NotifState.Blocked(permanent = false)),
        Case(true, true, false, false, NotifState.Active),
        Case(true, true, false, true, NotifState.HiddenByUser),
        Case(true, true, true, false, NotifState.Blocked(permanent = false)),
        Case(true, true, true, true, NotifState.Blocked(permanent = false)),
    )

    @Test
    fun `resolve covers all 16 input combinations`() {
        for ((i, c) in table.withIndex()) {
            val actual = SetupState.resolve(c.permissionGranted, c.appNotificationsOn, c.channelBlocked, c.hiddenByUser)
            assertEquals("case #$i $c", c.expected, actual)
        }
    }

    @Test
    fun `blocked is permanent only when the runtime permission is missing`() {
        assertEquals(
            NotifState.Blocked(permanent = true),
            SetupState.resolve(permissionGranted = false, appNotificationsOn = true, channelBlocked = false, hiddenByUser = false),
        )
        assertEquals(
            NotifState.Blocked(permanent = false),
            SetupState.resolve(permissionGranted = true, appNotificationsOn = false, channelBlocked = false, hiddenByUser = false),
        )
        assertEquals(
            NotifState.Blocked(permanent = false),
            SetupState.resolve(permissionGranted = true, appNotificationsOn = true, channelBlocked = true, hiddenByUser = false),
        )
    }

    @Test
    fun `hidden flag is honored only when notifications can actually be shown`() {
        assertEquals(NotifState.HiddenByUser, SetupState.resolve(true, true, false, true))
        // If blocked, the hidden flag does not change the Blocked result.
        assertFalse(SetupState.resolve(false, true, false, true) is NotifState.HiddenByUser)
    }

    // --- Task 8: top-level routing ---

    @Test
    fun `route sends first launch to setup and later launches to status`() {
        assertEquals(TopScreen.Setup, SetupState.route(isSetupComplete = false))
        assertEquals(TopScreen.Status, SetupState.route(isSetupComplete = true))
    }
}
