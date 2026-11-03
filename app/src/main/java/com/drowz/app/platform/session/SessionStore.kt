package com.drowz.app.platform.session

import android.content.Context

/**
 * The active wind-down session, persisted so it survives process death (SPEC §5). The
 * service recomputes the whole ritual from [startAtMillis], so nothing depends on in-memory
 * state; [wakeAtMillis] lets crash-recovery tell an *in-progress* hold (leave it) from an
 * *overdue* one whose wake alarm never fired (restore it).
 */
class SessionStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("drowz_session", Context.MODE_PRIVATE)

    val active: Boolean get() = prefs.getBoolean(KEY_ACTIVE, false)
    val startAtMillis: Long get() = prefs.getLong(KEY_START, 0L)
    val wakeAtMillis: Long get() = prefs.getLong(KEY_WAKE, 0L)

    fun begin(startAtMillis: Long, wakeAtMillis: Long) {
        prefs.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putLong(KEY_START, startAtMillis)
            .putLong(KEY_WAKE, wakeAtMillis)
            .apply()
    }

    fun end() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_ACTIVE = "active"
        const val KEY_START = "start_at"
        const val KEY_WAKE = "wake_at"
    }
}
