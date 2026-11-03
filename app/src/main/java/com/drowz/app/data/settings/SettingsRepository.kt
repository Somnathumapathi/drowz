package com.drowz.app.data.settings

import com.drowz.app.domain.model.SleepSettings
import kotlinx.coroutines.flow.Flow

/**
 * Reads/writes the user's [SleepSettings]. Interface lives in shared code (only depends on
 * domain models + coroutines `Flow`), so it ports to KMP; the DataStore-backed
 * implementation is Android-only.
 */
interface SettingsRepository {

    /** Emits the current settings and every subsequent change. */
    val settings: Flow<SleepSettings>

    /**
     * Atomically transform the stored settings. Callers express edits declaratively:
     * `repo.update { it.copy(enabled = true) }`.
     */
    suspend fun update(transform: (SleepSettings) -> SleepSettings)
}
