package com.drowz.app.platform.effects

import android.app.NotificationManager
import android.content.Context

/**
 * Do Not Disturb via NotificationManager. Uses INTERRUPTION_FILTER_PRIORITY (not ALARMS or
 * NONE) so the user's own priority exceptions still come through — and, crucially, so the
 * morning wake alarm can still ring. Captures the prior filter for an exact restore.
 */
internal class DndController(
    private val context: Context,
    private val baseline: BaselineStore,
) {
    private val nm get() =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var lastActive: Boolean? = null

    val available: Boolean get() = SystemPermissions.hasDndAccess(context)

    fun capture() {
        if (!available) return
        baseline.interruptionFilter = nm.currentInterruptionFilter
        lastActive = null
    }

    fun apply(active: Boolean) {
        if (!available || lastActive == active) return
        nm.setInterruptionFilter(
            if (active) NotificationManager.INTERRUPTION_FILTER_PRIORITY
            else baseline.interruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL,
        )
        lastActive = active
    }

    fun restore() {
        if (!available) return
        nm.setInterruptionFilter(
            baseline.interruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL,
        )
        lastActive = null
    }
}
