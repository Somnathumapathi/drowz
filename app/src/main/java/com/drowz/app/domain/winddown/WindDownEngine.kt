package com.drowz.app.domain.winddown

import com.drowz.app.domain.model.EffectSettings

/**
 * The heart of Drowz: a **pure function** that turns "how long has the ritual been
 * running" into the exact target state of every effect.
 *
 * Design intent:
 *  - Deterministic & side-effect-free → unit-testable to the second, and safe to
 *    recompute in a fresh process (e.g. after the OS kills the service) just from the
 *    session start time. Nothing here depends on prior in-memory state.
 *  - Gradual by construction — this ramp/fade shape is Drowz's whole differentiator over
 *    the OS's abrupt bedtime toggle (SPEC §7).
 *  - Effects that are toggled off emit their neutral value, so the same state can drive
 *    both the real service and the in-app preview without branching.
 *
 * All times are in seconds from the bedtime trigger (T-0).
 */
class WindDownEngine(private val timeline: Timeline = Timeline.Default) {

    /**
     * @param elapsedSeconds seconds since the bedtime trigger (>= 0).
     * @param settings the user's effect configuration.
     */
    fun stateAt(elapsedSeconds: Double, settings: EffectSettings): WindDownState {
        val t = elapsedSeconds.coerceAtLeast(0.0)
        val sessionEnd = settings.windDownMinutes * 60.0

        val brightness =
            if (settings.dimScreen) {
                lerp(1f, settings.targetBrightness, ramp(t, 0.0, timeline.dimRampSec))
            } else 1f

        val volume =
            if (settings.lowerVolume) {
                lerp(1f, settings.targetVolume, ramp(t, timeline.volumeStartSec, timeline.volumeRampSec))
            } else 1f

        val overlay =
            if (settings.darkOverlay) {
                settings.overlayDarkness * ramp(t, timeline.overlayStartSec, timeline.overlayRampSec)
            } else 0f

        val soundscape =
            if (settings.soundscape) soundscapeEnvelope(t, sessionEnd) else 0f

        val dndActive = settings.doNotDisturb && t >= timeline.dndAtSec
        val grayscale = settings.grayscale && t >= timeline.grayscaleAtSec

        // Darkness/progress reaches full night as the ramps complete, then holds.
        val progress = smoothstep(ramp(t, 0.0, timeline.darknessRampSec))

        return WindDownState(
            phase = phaseAt(t, sessionEnd),
            progress = progress,
            brightness = brightness,
            overlay = overlay,
            volume = volume,
            soundscape = soundscape,
            dndActive = dndActive,
            grayscale = grayscale,
        )
    }

    /** The target the morning restore returns everything to. */
    fun restored(): WindDownState = WindDownState.Awake

    private fun phaseAt(t: Double, sessionEnd: Double): WindDownPhase = when {
        t < timeline.reminderEndSec -> WindDownPhase.Reminder
        t < timeline.settlingEndSec -> WindDownPhase.Settling
        t < sessionEnd -> WindDownPhase.Drifting
        else -> WindDownPhase.Asleep
    }

    /** Fade in over [dimRampSec], hold, then fade out over the session's final stretch. */
    private fun soundscapeEnvelope(t: Double, sessionEnd: Double): Float {
        if (t >= sessionEnd) return 0f
        val fadeIn = ramp(t, 0.0, timeline.soundFadeInSec)
        val fadeOutStart = (sessionEnd - timeline.soundFadeOutSec).coerceAtLeast(0.0)
        val fadeOut = 1f - ramp(t, fadeOutStart, timeline.soundFadeOutSec)
        return (fadeIn * fadeOut).coerceIn(0f, 1f)
    }

    /**
     * Timeline constants, extracted so they're tunable in one place and overridable in
     * tests. Defaults match SPEC §7.
     */
    data class Timeline(
        val reminderEndSec: Double = 3.0,
        val dimRampSec: Double = 60.0,
        val soundFadeInSec: Double = 60.0,
        val dndAtSec: Double = 30.0,
        val volumeStartSec: Double = 30.0,
        val volumeRampSec: Double = 30.0,
        val overlayStartSec: Double = 60.0,
        val overlayRampSec: Double = 20.0,
        val grayscaleAtSec: Double = 60.0,
        val darknessRampSec: Double = 80.0,
        val settlingEndSec: Double = 80.0,
        val soundFadeOutSec: Double = 90.0,
    ) {
        companion object {
            val Default = Timeline()
        }
    }
}

// --- small, allocation-free math helpers (private to the domain) ---

/** Fraction of a ramp of [duration] completed at time [t], starting at [start]; 0..1. */
private fun ramp(t: Double, start: Double, duration: Double): Float {
    if (duration <= 0.0) return if (t >= start) 1f else 0f
    return (((t - start) / duration).coerceIn(0.0, 1.0)).toFloat()
}

private fun lerp(from: Float, to: Float, fraction: Float): Float =
    from + (to - from) * fraction

/** Smoothstep easing (matches the landing page's ease) for the darkness transition. */
private fun smoothstep(x: Float): Float = x * x * (3f - 2f * x)
