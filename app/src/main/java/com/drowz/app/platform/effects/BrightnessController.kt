package com.drowz.app.platform.effects

import android.content.Context
import android.provider.Settings

/**
 * System screen brightness via WRITE_SETTINGS. Captures the user's brightness value + auto/
 * manual mode so [restore] is exact. Only ever dims *below* the captured level and never
 * brightens — so engaging mid-evening can never make the screen flare up.
 */
internal class BrightnessController(
    private val context: Context,
    private val baseline: BaselineStore,
) {
    private val resolver get() = context.contentResolver
    private var lastWritten = -1

    val available: Boolean get() = SystemPermissions.canWriteSystemSettings(context)

    fun capture() {
        if (!available) return
        baseline.brightness = Settings.System.getInt(
            resolver, Settings.System.SCREEN_BRIGHTNESS, DEFAULT_BRIGHTNESS,
        )
        baseline.brightnessMode = Settings.System.getInt(
            resolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
        )
        lastWritten = -1
    }

    /** [fraction] is the engine's 0..1 target; 1 = "full / untouched". */
    fun apply(fraction: Float) {
        if (!available || fraction >= 0.999f) return   // not dimming yet → leave it alone
        val effective = minOf(fraction, baselineFraction())   // only go darker, never up
        val value = (effective * 255f).toInt().coerceIn(MIN_BRIGHTNESS, 255)
        if (value == lastWritten) return
        Settings.System.putInt(
            resolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
        )
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, value)
        lastWritten = value
    }

    fun restore() {
        if (!available) return
        baseline.brightnessMode?.let {
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, it)
        }
        baseline.brightness?.let {
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, it)
        }
        lastWritten = -1
    }

    private fun baselineFraction(): Float = (baseline.brightness ?: DEFAULT_BRIGHTNESS) / 255f

    private companion object {
        const val DEFAULT_BRIGHTNESS = 128
        const val MIN_BRIGHTNESS = 1   // keep the panel barely lit, never fully black
    }
}
