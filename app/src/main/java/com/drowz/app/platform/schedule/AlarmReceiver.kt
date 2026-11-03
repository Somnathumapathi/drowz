package com.drowz.app.platform.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.drowz.app.DrowzApplication
import com.drowz.app.platform.service.SleepModeService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Fires at bedtime and wake. Bedtime starts the wind-down service; wake stops it (which
 * restores everything). Because exact one-shot alarms don't repeat, the next bedtime is
 * re-armed here after each fire (the wake alarm belongs to the session, so we never touch it
 * during a reschedule).
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AlarmBedtimeScheduler.ACTION_BEDTIME -> SleepModeService.start(context)
            AlarmBedtimeScheduler.ACTION_WAKE -> SleepModeService.stop(context)
        }

        val pending = goAsync()
        val container = (context.applicationContext as DrowzApplication).container
        container.applicationScope.launch {
            try {
                container.scheduler.reschedule(container.settingsRepository.settings.first())
            } finally {
                pending.finish()
            }
        }
    }
}
