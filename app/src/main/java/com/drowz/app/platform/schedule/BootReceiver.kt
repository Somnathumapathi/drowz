package com.drowz.app.platform.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.drowz.app.DrowzApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Alarms don't survive a reboot, so re-arm the next bedtime once the device comes back up. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
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
