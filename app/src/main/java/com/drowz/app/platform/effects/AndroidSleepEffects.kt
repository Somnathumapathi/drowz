package com.drowz.app.platform.effects

import android.content.Context
import com.drowz.app.domain.effects.EffectCapabilities
import com.drowz.app.domain.effects.SleepEffects
import com.drowz.app.domain.winddown.WindDownState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The real Android implementation of [SleepEffects] (SPEC §5–§8). Delegates to one small
 * controller per subsystem, captures a persisted baseline before any change, applies the
 * engine's absolute [WindDownState] idempotently on each tick, and reverses everything in
 * the safe order on [restore] — even from a fresh process after a crash ([restoreDangling]).
 *
 * Every effect degrades gracefully: a missing permission simply skips that one effect while
 * the rest of the ritual proceeds (SPEC §6). Settings-backed writes run on the caller's
 * dispatcher; the WindowManager overlay is dispatched to the main thread.
 */
class AndroidSleepEffects(context: Context) : SleepEffects {

    private val appContext = context.applicationContext
    private val baseline = BaselineStore(appContext)

    private val brightness = BrightnessController(appContext, baseline)
    private val dnd = DndController(appContext, baseline)
    private val volume = VolumeController(appContext, baseline)
    private val grayscale = GrayscaleController(appContext, baseline)
    private val overlay = DimOverlayController(appContext)

    override fun capabilities() = EffectCapabilities(
        canDim = brightness.available,
        canDarken = overlay.available,
        canGrayscale = grayscale.available,
        canDnd = dnd.available,
        canChangeVolume = volume.available,
        canPlaySoundscape = true,
    )

    override suspend fun captureBaseline() {
        brightness.capture()
        dnd.capture()
        volume.capture()
        grayscale.capture()
        baseline.sessionActive = true   // arm crash-recovery
    }

    override suspend fun apply(state: WindDownState) {
        brightness.apply(state.brightness)
        volume.apply(state.volume)
        dnd.apply(state.dndActive)
        grayscale.apply(state.grayscale)
        withContext(Dispatchers.Main) { overlay.setColor(overlayColor(state)) }
    }

    override suspend fun restore() {
        // Safe order (SPEC §8): overlay → grayscale → brightness → volume → DND.
        withContext(Dispatchers.Main) { overlay.remove() }
        grayscale.restore()
        brightness.restore()
        volume.restore()
        dnd.restore()
        baseline.sessionActive = false
        baseline.clear()
    }

    override suspend fun restoreDangling() {
        if (baseline.sessionActive) restore()
    }

    /**
     * A neutral black dimming overlay. Its alpha is the [WindDownState.overlay] darkness,
     * combined (straight-alpha `over`, both layers black) with an extra brightness-based
     * term *only* when the system-brightness permission is missing — so the overlay can
     * dim on its own as a fallback:
     *
     *   outAlpha = 1 − (1 − overlay)(1 − brightnessFallback)
     *
     * Capped so the screen is never driven fully black (you can still glance at it).
     */
    private fun overlayColor(state: WindDownState): Int {
        val darkness = state.overlay.coerceIn(0f, 1f)
        val brightnessFallback =
            if (brightness.available) 0f
            else ((1f - state.brightness) * MAX_OVERLAY_DIM).coerceIn(0f, 1f)

        val outAlpha = (1f - (1f - darkness) * (1f - brightnessFallback)).coerceIn(0f, MAX_TOTAL_ALPHA)
        if (outAlpha <= 0.001f) return 0
        val a = (outAlpha * 255f).toInt().coerceIn(0, 255)
        return a shl 24   // black (RGB = 0)
    }

    private companion object {
        // Deepest the overlay-only dim fallback goes when WRITE_SETTINGS isn't granted.
        const val MAX_OVERLAY_DIM = 0.85f
        // Never fully black — always leave the screen faintly visible.
        const val MAX_TOTAL_ALPHA = 0.92f
    }
}
