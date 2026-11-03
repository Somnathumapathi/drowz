package com.drowz.app.domain.winddown

/**
 * Where the ritual is right now, for display + coarse behaviour switches.
 * The gradual numeric values live in [WindDownState]; this is the human-legible layer.
 */
enum class WindDownPhase(val title: String, val caption: String) {
    Idle("Awake", "Everything's normal"),
    Reminder("Time to wind down", "The city is turning in"),
    Settling("Settling in", "Dimming, quieting, drifting a soundscape in"),
    Drifting("Drifting off", "Low, warm, and quiet"),
    Asleep("Asleep", "Held until morning"),
    Restoring("Good morning", "Restoring everything Drowz changed"),
}

/**
 * A fully-resolved snapshot of the wind-down at one instant. Every field is an absolute
 * target the effects layer can apply directly — no deltas, no history. This makes the
 * whole system a pure function of (elapsed time, settings), which is trivial to test and
 * safe to recompute after process death.
 *
 * Neutral ("untouched") values: brightness/volume = 1f, overlay/soundscape = 0f,
 * dndActive/grayscale = false.
 */
data class WindDownState(
    val phase: WindDownPhase,
    /** 0 = dusk, 1 = deep night. Drives the celestial background. */
    val progress: Float,
    val brightness: Float,
    /** 0..1 darkness of the neutral dimming overlay laid over the screen. */
    val overlay: Float,
    val volume: Float,
    val soundscape: Float,
    val dndActive: Boolean,
    val grayscale: Boolean,
) {
    companion object {
        val Awake = WindDownState(
            phase = WindDownPhase.Idle,
            progress = 0f,
            brightness = 1f,
            overlay = 0f,
            volume = 1f,
            soundscape = 0f,
            dndActive = false,
            grayscale = false,
        )
    }
}
