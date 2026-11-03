package com.drowz.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.drowz.app.data.settings.SettingsRepository
import com.drowz.app.di.AppContainer
import com.drowz.app.domain.effects.SleepEffects
import com.drowz.app.domain.model.EffectSettings
import com.drowz.app.domain.model.SleepSettings
import com.drowz.app.domain.model.TimeOfDay
import com.drowz.app.domain.model.Weekday
import com.drowz.app.domain.schedule.Scheduler
import com.drowz.app.domain.winddown.WindDownEngine
import com.drowz.app.domain.winddown.WindDownPhase
import com.drowz.app.domain.winddown.WindDownState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The single app-scoped view model. Owns the settings edits (schedule + effects), the
 * in-app preview, and the live per-permission [capabilities] the setup/settings screens
 * read. Every schedule-relevant edit re-runs [Scheduler.reschedule], so the alarm always
 * matches what's on screen.
 */
class DrowzViewModel(
    private val settingsRepository: SettingsRepository,
    private val scheduler: Scheduler,
    private val sleepEffects: SleepEffects,
    private val engine: WindDownEngine = WindDownEngine(),
) : ViewModel() {

    private val windDown = MutableStateFlow(WindDownState.Awake)
    private val previewing = MutableStateFlow(false)
    private val capabilities = MutableStateFlow(sleepEffects.capabilities())

    val uiState: StateFlow<DrowzUiState> =
        combine(settingsRepository.settings, windDown, previewing, capabilities) { s, wd, preview, caps ->
            DrowzUiState(settings = s, windDown = wd, isPreviewing = preview, capabilities = caps)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DrowzUiState.Initial)

    /** Re-read live permission state — call whenever the app resumes from system settings. */
    fun refreshCapabilities() {
        capabilities.value = sleepEffects.capabilities()
    }

    suspend fun hasCompletedSetup(): Boolean = settingsRepository.settings.first().hasCompletedSetup

    // --- edits (each reschedules so the alarm tracks the UI) ---

    fun setArmed(armed: Boolean) = edit { it.copy(enabled = armed) }
    fun setBedtime(hour: Int, minute: Int) = edit { it.copy(bedtime = TimeOfDay(hour, minute)) }
    fun setWakeTime(hour: Int, minute: Int) = edit { it.copy(wakeTime = TimeOfDay(hour, minute)) }
    fun toggleDay(day: Weekday) = edit {
        it.copy(activeDays = if (day in it.activeDays) it.activeDays - day else it.activeDays + day)
    }
    fun updateEffects(transform: (EffectSettings) -> EffectSettings) =
        edit { it.copy(effects = transform(it.effects)) }
    fun markSetupComplete() = edit { it.copy(hasCompletedSetup = true) }

    private fun edit(transform: (SleepSettings) -> SleepSettings) {
        viewModelScope.launch {
            settingsRepository.update(transform)
            scheduler.reschedule(settingsRepository.settings.first())
        }
    }

    // --- in-app preview: a time-compressed dry-run driven by the real engine ---

    private var previewJob: Job? = null
    private var previewElapsed = 0.0

    fun startPreview() {
        if (previewing.value) return
        previewJob = viewModelScope.launch {
            previewing.value = true
            val effects = settingsRepository.settings.first().effects
            previewElapsed = 0.0
            while (previewElapsed < PREVIEW_HOLD_SEC) {
                windDown.value = engine.stateAt(previewElapsed, effects)
                delay(FRAME_MS)
                previewElapsed += FRAME_SEC * PREVIEW_SPEED
            }
            windDown.value = engine.stateAt(PREVIEW_HOLD_SEC, effects)
        }
    }

    fun restorePreview() {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            val effects = settingsRepository.settings.first().effects
            while (previewElapsed > 0.0) {
                windDown.value = engine.stateAt(previewElapsed, effects).copy(phase = WindDownPhase.Restoring)
                delay(FRAME_MS)
                previewElapsed -= FRAME_SEC * RESTORE_SPEED
            }
            previewElapsed = 0.0
            windDown.value = WindDownState.Awake
            previewing.value = false
        }
    }

    companion object {
        private const val FRAME_MS = 16L
        private const val FRAME_SEC = 0.016
        private const val PREVIEW_SPEED = 8.0
        private const val RESTORE_SPEED = 40.0
        private const val PREVIEW_HOLD_SEC = 85.0

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DrowzViewModel(
                    settingsRepository = container.settingsRepository,
                    scheduler = container.scheduler,
                    sleepEffects = container.sleepEffects,
                )
            }
        }
    }
}
