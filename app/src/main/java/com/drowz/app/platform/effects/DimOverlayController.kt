package com.drowz.app.platform.effects

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/**
 * A full-screen, click-through overlay that darkens the whole display — used to dim below
 * the panel's own minimum brightness. Uses TYPE_APPLICATION_OVERLAY (SYSTEM_ALERT_WINDOW).
 *
 * WindowManager add/update/remove MUST run on the main thread — [AndroidSleepEffects]
 * dispatches to it. The view is added lazily on the first non-transparent colour and
 * removed when the colour goes fully transparent, so an idle overlay holds no window.
 */
internal class DimOverlayController(private val context: Context) {

    private val windowManager get() =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: View? = null
    private var lastColor = 0

    val available: Boolean get() = SystemPermissions.canDrawOverlay(context)

    /** Set the overlay colour. A fully-transparent (alpha 0) colour removes the overlay. */
    fun setColor(argb: Int) {
        if (!available) return
        if (argb ushr 24 == 0) { remove(); return }
        val v = view ?: addView()
        if (argb != lastColor) {
            v.setBackgroundColor(argb)
            lastColor = argb
        }
    }

    fun remove() {
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
        lastColor = 0
    }

    private fun addView(): View {
        val v = View(context)
        @Suppress("DEPRECATION")
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }
        windowManager.addView(v, params)
        view = v
        return v
    }
}
