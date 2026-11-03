package com.drowz.app.domain.effects

import com.drowz.app.domain.winddown.WindDownState

/**
 * The single seam between shared logic and platform-specific system control.
 *
 * The wind-down engine computes *what* the device should look/sound like; this interface
 * is *how* that happens on a given platform. On Android the implementation drives the
 * overlay, DND, brightness, volume, grayscale and soundscape. When the iOS target is
 * added, this becomes an `expect`/`actual` pair whose iOS `actual` simply does less
 * (SPEC §10) — no shared code above this line has to change.
 *
 * Contract:
 *  - [captureBaseline] MUST run once before any mutation, persisting prior system values
 *    so [restore] works even from a fresh process (SPEC §5, §8).
 *  - [apply] is idempotent: called repeatedly with successive [WindDownState]s during the
 *    ramp; implementations diff against what they last set.
 *  - Every method degrades gracefully — a missing permission skips that one effect, never
 *    throwing and never aborting the rest of the ritual (SPEC §6).
 */
interface SleepEffects {

    /** Which effects can actually run right now, given current permissions. */
    fun capabilities(): EffectCapabilities

    /** Snapshot & persist the system values Drowz is about to change. */
    suspend fun captureBaseline()

    /** Move the device toward [state]. Called on every tick of the ramp. */
    suspend fun apply(state: WindDownState)

    /** Reverse everything, in the safe order of SPEC §8, back to the captured baseline. */
    suspend fun restore()

    /**
     * If a previous session was left applied (e.g. the service's process was killed
     * mid-ritual), reverse it back to the captured baseline. Safe no-op if nothing dangles.
     */
    suspend fun restoreDangling() {}
}

/**
 * Per-effect availability. The UI reads this to show honest, per-permission status and to
 * gate the power-user grayscale toggle until its ADB permission is detected.
 */
data class EffectCapabilities(
    val canDim: Boolean = false,
    val canDarken: Boolean = false,
    val canGrayscale: Boolean = false,
    val canDnd: Boolean = false,
    val canChangeVolume: Boolean = true,   // no special permission on Android
    val canPlaySoundscape: Boolean = true,
) {
    companion object {
        /** Everything off — used by previews and before permissions are known. */
        val None = EffectCapabilities(
            canDim = false,
            canDarken = false,
            canGrayscale = false,
            canDnd = false,
            canChangeVolume = false,
            canPlaySoundscape = false,
        )
    }
}
