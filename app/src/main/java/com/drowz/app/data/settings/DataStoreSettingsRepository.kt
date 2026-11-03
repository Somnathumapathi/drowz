package com.drowz.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.drowz.app.domain.model.EffectSettings
import com.drowz.app.domain.model.SleepSettings
import com.drowz.app.domain.model.TimeOfDay
import com.drowz.app.domain.model.Weekday
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "drowz_settings")

/**
 * DataStore-backed settings. Preferences are flat, so we hand-map each domain field to a
 * key. Reads always fall back to [SleepSettings.Default], so a partially-written or empty
 * store still yields valid settings — no nullable soup upstream.
 */
class DataStoreSettingsRepository(context: Context) : SettingsRepository {

    private val store: DataStore<Preferences> = context.dataStore

    override val settings: Flow<SleepSettings> =
        store.data.map { prefs -> prefs.toSleepSettings() }

    override suspend fun update(transform: (SleepSettings) -> SleepSettings) {
        store.edit { prefs ->
            val next = transform(prefs.toSleepSettings())
            prefs.writeSleepSettings(next)
        }
    }

    // --- mapping ---

    private fun Preferences.toSleepSettings(): SleepSettings {
        val d = SleepSettings.Default
        return SleepSettings(
            enabled = this[Keys.enabled] ?: d.enabled,
            bedtime = this[Keys.bedtimeMinute]?.let(TimeOfDay::ofMinuteOfDay) ?: d.bedtime,
            wakeTime = this[Keys.wakeMinute]?.let(TimeOfDay::ofMinuteOfDay) ?: d.wakeTime,
            activeDays = this[Keys.activeDays]?.toWeekdays() ?: d.activeDays,
            hasCompletedSetup = this[Keys.hasCompletedSetup] ?: d.hasCompletedSetup,
            effects = EffectSettings(
                dimScreen = this[Keys.dimScreen] ?: d.effects.dimScreen,
                targetBrightness = this[Keys.targetBrightness] ?: d.effects.targetBrightness,
                darkOverlay = this[Keys.darkOverlay] ?: d.effects.darkOverlay,
                overlayDarkness = this[Keys.overlayDarkness] ?: d.effects.overlayDarkness,
                grayscale = this[Keys.grayscale] ?: d.effects.grayscale,
                doNotDisturb = this[Keys.doNotDisturb] ?: d.effects.doNotDisturb,
                lowerVolume = this[Keys.lowerVolume] ?: d.effects.lowerVolume,
                targetVolume = this[Keys.targetVolume] ?: d.effects.targetVolume,
                soundscape = this[Keys.soundscape] ?: d.effects.soundscape,
                windDownMinutes = this[Keys.windDownMinutes] ?: d.effects.windDownMinutes,
            ),
        )
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.writeSleepSettings(
        s: SleepSettings,
    ) {
        this[Keys.enabled] = s.enabled
        this[Keys.bedtimeMinute] = s.bedtime.minuteOfDay
        this[Keys.wakeMinute] = s.wakeTime.minuteOfDay
        this[Keys.activeDays] = s.activeDays.map(Weekday::name).toSet()
        this[Keys.hasCompletedSetup] = s.hasCompletedSetup
        this[Keys.dimScreen] = s.effects.dimScreen
        this[Keys.targetBrightness] = s.effects.targetBrightness
        this[Keys.darkOverlay] = s.effects.darkOverlay
        this[Keys.overlayDarkness] = s.effects.overlayDarkness
        this[Keys.grayscale] = s.effects.grayscale
        this[Keys.doNotDisturb] = s.effects.doNotDisturb
        this[Keys.lowerVolume] = s.effects.lowerVolume
        this[Keys.targetVolume] = s.effects.targetVolume
        this[Keys.soundscape] = s.effects.soundscape
        this[Keys.windDownMinutes] = s.effects.windDownMinutes
    }

    private fun Set<String>.toWeekdays(): Set<Weekday> =
        mapNotNull { name -> Weekday.entries.firstOrNull { it.name == name } }.toSet()

    private object Keys {
        val enabled = booleanPreferencesKey("enabled")
        val bedtimeMinute = intPreferencesKey("bedtime_minute")
        val wakeMinute = intPreferencesKey("wake_minute")
        val activeDays = stringSetPreferencesKey("active_days")
        val hasCompletedSetup = booleanPreferencesKey("has_completed_setup")
        val dimScreen = booleanPreferencesKey("dim_screen")
        val targetBrightness = floatPreferencesKey("target_brightness")
        val darkOverlay = booleanPreferencesKey("dark_overlay")
        val overlayDarkness = floatPreferencesKey("overlay_darkness")
        val grayscale = booleanPreferencesKey("grayscale")
        val doNotDisturb = booleanPreferencesKey("do_not_disturb")
        val lowerVolume = booleanPreferencesKey("lower_volume")
        val targetVolume = floatPreferencesKey("target_volume")
        val soundscape = booleanPreferencesKey("soundscape")
        val windDownMinutes = intPreferencesKey("wind_down_minutes")
    }
}
