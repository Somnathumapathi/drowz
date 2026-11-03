package com.drowz.app.platform.effects

import android.content.Context
import android.provider.Settings

/**
 * Power-user true grayscale via the accessibility display daltonizer. Needs
 * WRITE_SECURE_SETTINGS, which is ADB-granted only (SPEC §4). Captures the prior daltonizer
 * state so [restore] returns colour exactly as it was — including if the user already had a
 * colour-correction mode on.
 */
internal class GrayscaleController(
    private val context: Context,
    private val baseline: BaselineStore,
) {
    private val resolver get() = context.contentResolver
    private var lastActive: Boolean? = null

    val available: Boolean get() = SystemPermissions.hasSecureSettings(context)

    fun capture() {
        if (!available) return
        baseline.daltonizerEnabled = Settings.Secure.getInt(resolver, DALTONIZER_ENABLED, 0)
        baseline.daltonizerValue = Settings.Secure.getInt(resolver, DALTONIZER, -1)
        lastActive = null
    }

    fun apply(active: Boolean) {
        if (!available || lastActive == active) return
        if (active) {
            runCatching {
                Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, 1)
                Settings.Secure.putInt(resolver, DALTONIZER, MODE_MONOCHROME)
            }
        } else {
            restore()
        }
        lastActive = active
    }

    fun restore() {
        if (!available) return
        runCatching {
            Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, baseline.daltonizerEnabled ?: 0)
            Settings.Secure.putInt(resolver, DALTONIZER, baseline.daltonizerValue ?: -1)
        }
        lastActive = null
    }

    private companion object {
        const val DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
        const val DALTONIZER = "accessibility_display_daltonizer"
        const val MODE_MONOCHROME = 0   // 0 = grayscale
    }
}
