package com.drowz.app.platform.effects

import android.content.Context

/**
 * Persists the system values Drowz overwrote, so [AndroidSleepEffects.restore] works even
 * from a fresh process after the OS kills the service (SPEC §5, §8). Backed by
 * SharedPreferences rather than DataStore because restore may run very early in a new
 * process and must be dead-simple and synchronous.
 *
 * [sessionActive] is the crash-recovery flag: set true at [AndroidSleepEffects.captureBaseline],
 * cleared at [AndroidSleepEffects.restore]. If it's still true on next launch, a session was
 * interrupted and must be reversed.
 */
internal class BaselineStore(context: Context) {

    private val prefs = context.getSharedPreferences("drowz_baseline", Context.MODE_PRIVATE)

    var sessionActive: Boolean
        get() = prefs.getBoolean(KEY_ACTIVE, false)
        set(value) { prefs.edit().putBoolean(KEY_ACTIVE, value).apply() }

    var brightness: Int?
        get() = getIntOrNull(KEY_BRIGHTNESS)
        set(value) = putIntOrNull(KEY_BRIGHTNESS, value)

    var brightnessMode: Int?
        get() = getIntOrNull(KEY_BRIGHTNESS_MODE)
        set(value) = putIntOrNull(KEY_BRIGHTNESS_MODE, value)

    var interruptionFilter: Int?
        get() = getIntOrNull(KEY_DND)
        set(value) = putIntOrNull(KEY_DND, value)

    var daltonizerEnabled: Int?
        get() = getIntOrNull(KEY_GRAY_ENABLED)
        set(value) = putIntOrNull(KEY_GRAY_ENABLED, value)

    var daltonizerValue: Int?
        get() = getIntOrNull(KEY_GRAY_VALUE)
        set(value) = putIntOrNull(KEY_GRAY_VALUE, value)

    fun volume(stream: Int): Int? = getIntOrNull("$KEY_VOL_PREFIX$stream")
    fun setVolume(stream: Int, value: Int?) = putIntOrNull("$KEY_VOL_PREFIX$stream", value)

    /** Wipe every captured value (keeps nothing) — called after a clean restore. */
    fun clear() = prefs.edit().clear().apply()

    private fun getIntOrNull(key: String): Int? =
        if (prefs.contains(key)) prefs.getInt(key, 0) else null

    private fun putIntOrNull(key: String, value: Int?) {
        val editor = prefs.edit()
        if (value == null) editor.remove(key) else editor.putInt(key, value)
        editor.apply()
    }

    private companion object {
        const val KEY_ACTIVE = "session_active"
        const val KEY_BRIGHTNESS = "brightness"
        const val KEY_BRIGHTNESS_MODE = "brightness_mode"
        const val KEY_DND = "dnd_filter"
        const val KEY_GRAY_ENABLED = "gray_enabled"
        const val KEY_GRAY_VALUE = "gray_value"
        const val KEY_VOL_PREFIX = "vol_"
    }
}
