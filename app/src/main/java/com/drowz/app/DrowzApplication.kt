package com.drowz.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.drowz.app.di.AppContainer
import kotlinx.coroutines.launch

class DrowzApplication : Application() {

    /** App-wide dependency graph. Constructed once, read by activities/receivers/services. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()
        recoverInterruptedSession()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SLEEP,
                    "Sleep mode",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = "The ongoing wind-down session" },
            )
        }
    }

    /**
     * If a wind-down was left applied *past* its wake time — e.g. the app was force-stopped,
     * so the wake alarm never fired — reverse it now. An in-progress hold (now < wake) is
     * left alone: its wake alarm, or the running service, will handle it.
     */
    private fun recoverInterruptedSession() {
        container.applicationScope.launch {
            val session = container.sessionStore
            if (session.active && System.currentTimeMillis() >= session.wakeAtMillis) {
                container.sleepEffects.restoreDangling()
                session.end()
            }
        }
    }

    companion object {
        const val CHANNEL_SLEEP = "sleep_mode"
    }
}
