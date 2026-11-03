package com.drowz.app.platform.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.drowz.app.domain.model.SleepSettings
import com.drowz.app.domain.schedule.Scheduler
import java.time.ZonedDateTime

/**
 * Exact bedtime/wake alarms via AlarmManager (SPEC §5). Uses `setExactAndAllowWhileIdle` so
 * the trigger survives Doze; falls back to an inexact alarm if exact scheduling isn't
 * permitted (the app declares USE_EXACT_ALARM, so this is rare). One-shot alarms don't
 * repeat, so [AlarmReceiver] re-arms the next bedtime each time one fires.
 */
class AlarmBedtimeScheduler(context: Context) : Scheduler {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    override fun reschedule(settings: SleepSettings) {
        cancelBedtime()
        if (!settings.enabled || settings.activeDays.isEmpty()) {
            cancelWake()
            return
        }
        val bedtime = ScheduleTimes.nextTimeAfter(settings.bedtime, ZonedDateTime.now(), settings.activeDays)
        setExact(REQ_BEDTIME, ACTION_BEDTIME, bedtime.toInstant().toEpochMilli())
    }

    override fun scheduleWake(atEpochMillis: Long) {
        setExact(REQ_WAKE, ACTION_WAKE, atEpochMillis)
    }

    override fun cancel() {
        cancelBedtime()
        cancelWake()
    }

    private fun cancelBedtime() = alarmManager.cancel(pending(REQ_BEDTIME, ACTION_BEDTIME))
    private fun cancelWake() = alarmManager.cancel(pending(REQ_WAKE, ACTION_WAKE))

    private fun setExact(requestCode: Int, action: String, atMillis: Long) {
        val pendingIntent = pending(requestCode, action)
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
        }
    }

    private fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun pending(requestCode: Int, action: String): PendingIntent {
        val intent = Intent(appContext, AlarmReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            appContext, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        const val ACTION_BEDTIME = "com.drowz.app.action.BEDTIME"
        const val ACTION_WAKE = "com.drowz.app.action.WAKE"
        private const val REQ_BEDTIME = 2001
        private const val REQ_WAKE = 2002
    }
}
