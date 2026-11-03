package com.drowz.app.domain.schedule

import com.drowz.app.domain.model.SleepSettings

/**
 * Schedules the bedtime trigger (and the morning wake that restores everything). The
 * interface is portable — only depends on the domain model — so it lifts to KMP; the
 * AlarmManager implementation is Android-only.
 *
 * The wake alarm is scheduled *by the running session* (via [scheduleWake]) rather than
 * alongside the bedtime, so re-arming the next bedtime can never clobber tonight's wake.
 */
interface Scheduler {

    /** (Re)schedule the next bedtime from [settings]. Cancels everything if disabled. */
    fun reschedule(settings: SleepSettings)

    /** Schedule the one-shot morning restore at [atEpochMillis]. Called when a session begins. */
    fun scheduleWake(atEpochMillis: Long)

    /** Cancel bedtime and wake alarms. */
    fun cancel()
}
