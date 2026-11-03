package com.drowz.app.domain.model

/**
 * A wall-clock time of day. Deliberately framework-free (no `java.time`) so the whole
 * domain layer is portable to KMP `commonMain`.
 */
data class TimeOfDay(val hour: Int, val minute: Int) {
    init {
        require(hour in 0..23) { "hour out of range: $hour" }
        require(minute in 0..59) { "minute out of range: $minute" }
    }

    val minuteOfDay: Int get() = hour * 60 + minute

    /** "22:30" — stable, locale-independent. UI formats 12h/AM-PM for display. */
    fun format24(): String =
        "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

    companion object {
        fun ofMinuteOfDay(m: Int): TimeOfDay {
            val wrapped = ((m % 1440) + 1440) % 1440
            return TimeOfDay(wrapped / 60, wrapped % 60)
        }
    }
}

enum class Weekday { MON, TUE, WED, THU, FRI, SAT, SUN }

/**
 * Everything the user can configure. One immutable tree — the single source of truth the
 * scheduler, the wind-down engine, and the UI all read from.
 */
data class SleepSettings(
    val enabled: Boolean = false,
    val bedtime: TimeOfDay = TimeOfDay(22, 30),
    val wakeTime: TimeOfDay = TimeOfDay(7, 0),
    val activeDays: Set<Weekday> = Weekday.entries.toSet(),
    val effects: EffectSettings = EffectSettings(),
    /** First-run flag: false until the user finishes (or skips) the setup screen. */
    val hasCompletedSetup: Boolean = false,
) {
    companion object {
        val Default = SleepSettings()
    }
}

/**
 * Per-effect configuration. Each effect is independently toggleable; when off, the
 * wind-down engine emits that effect's neutral value and the service skips it entirely.
 *
 * @property targetBrightness 0..1 system screen brightness at the bottom of the dim ramp.
 * @property overlayDarkness 0..1 darkness of the dimming overlay at full night — darkens
 *   the screen *below* the panel's own minimum brightness.
 * @property grayscale power-user true system grayscale (needs ADB-granted permission).
 * @property targetVolume 0..1 media/ring volume at the bottom of the ramp.
 * @property windDownMinutes how long the soundscape plays before it fades to silence.
 */
data class EffectSettings(
    val dimScreen: Boolean = true,
    val targetBrightness: Float = 0.04f,
    val darkOverlay: Boolean = true,
    val overlayDarkness: Float = 0.6f,
    val grayscale: Boolean = false,
    val doNotDisturb: Boolean = true,
    val lowerVolume: Boolean = true,
    val targetVolume: Float = 0.15f,
    val soundscape: Boolean = true,
    val windDownMinutes: Int = 20,
) {
    init {
        require(targetBrightness in 0f..1f)
        require(overlayDarkness in 0f..1f)
        require(targetVolume in 0f..1f)
        require(windDownMinutes in 1..240)
    }
}
