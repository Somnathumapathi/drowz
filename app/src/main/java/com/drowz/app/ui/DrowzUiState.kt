package com.drowz.app.ui

import com.drowz.app.domain.effects.EffectCapabilities
import com.drowz.app.domain.model.SleepSettings
import com.drowz.app.domain.winddown.WindDownState

/**
 * Everything the app's screens render, as one immutable snapshot: the user's settings, the
 * live wind-down/preview state, and which effects can currently run (per permissions).
 */
data class DrowzUiState(
    val settings: SleepSettings,
    val windDown: WindDownState,
    val isPreviewing: Boolean,
    val capabilities: EffectCapabilities,
) {
    companion object {
        val Initial = DrowzUiState(
            settings = SleepSettings.Default,
            windDown = WindDownState.Awake,
            isPreviewing = false,
            capabilities = EffectCapabilities.None,
        )
    }
}
