package com.drowz.app.di

import android.content.Context
import com.drowz.app.data.settings.DataStoreSettingsRepository
import com.drowz.app.data.settings.SettingsRepository
import com.drowz.app.domain.effects.SleepEffects
import com.drowz.app.domain.schedule.Scheduler
import com.drowz.app.platform.effects.AndroidSleepEffects
import com.drowz.app.platform.schedule.AlarmBedtimeScheduler
import com.drowz.app.platform.session.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency container — the single place app-wide singletons are wired.
 *
 * Deliberately not Hilt: for a v1 this size, a hand-rolled container keeps the build simple
 * (no annotation processing) and the wiring explicit and greppable. If the graph grows,
 * swapping this for Hilt is a contained change because construction already flows through
 * one seam.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    /** Long-lived scope for app-level background work (receivers, crash-recovery restore). */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsRepository: SettingsRepository by lazy {
        DataStoreSettingsRepository(appContext)
    }

    /**
     * The platform seam — the real Android controllers (overlay, DND, brightness, volume,
     * grayscale) with persisted baseline capture + restore. Driven by [SessionStore]-backed
     * [com.drowz.app.platform.service.SleepModeService]; on iOS this becomes the `actual`.
     */
    val sleepEffects: SleepEffects by lazy {
        AndroidSleepEffects(appContext)
    }

    /** Exact bedtime/wake alarms. */
    val scheduler: Scheduler by lazy {
        AlarmBedtimeScheduler(appContext)
    }

    /** The active wind-down session (persisted; source of truth for crash-recovery). */
    val sessionStore: SessionStore by lazy {
        SessionStore(appContext)
    }
}
