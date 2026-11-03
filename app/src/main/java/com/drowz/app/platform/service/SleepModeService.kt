package com.drowz.app.platform.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.drowz.app.DrowzApplication
import com.drowz.app.MainActivity
import com.drowz.app.R
import com.drowz.app.di.AppContainer
import com.drowz.app.domain.winddown.WindDownEngine
import com.drowz.app.domain.winddown.WindDownPhase
import com.drowz.app.platform.schedule.ScheduleTimes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * The foreground service that runs a wind-down (SPEC §7–§8).
 *
 * On bedtime it captures the baseline, then ramps the effects by feeding real elapsed time
 * into the pure [WindDownEngine] each tick. Once the ramp settles it stops ticking but stays
 * foreground, *holding* the sleep environment (overlay + system settings) until the wake
 * alarm — or the "I'm awake" action — restores everything.
 *
 * Robustness: the session start time is persisted, so a `START_STICKY` restart after a kill
 * recomputes and re-applies the exact same state; nothing relies on in-memory progress.
 */
class SleepModeService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val engine = WindDownEngine()
    private lateinit var container: AppContainer
    private var runJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        container = (application as DrowzApplication).container
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (intent?.action == ACTION_STOP) {
            stopSession()
            START_NOT_STICKY
        } else {
            startSession()
            START_STICKY
        }
    }

    private fun startSession() {
        goForeground(WindDownPhase.Reminder)
        if (runJob?.isActive == true) return
        runJob = scope.launch {
            val settings = container.settingsRepository.settings.first()
            val effects = settings.effects
            val session = container.sessionStore

            val startAt: Long = if (session.active) {
                // Resuming after a process restart. If we've slept past wake, just restore.
                if (System.currentTimeMillis() >= session.wakeAtMillis) {
                    stopSession(); return@launch
                }
                session.startAtMillis
            } else {
                val now = System.currentTimeMillis()
                val wakeAt = ScheduleTimes.nextTimeAfter(settings.wakeTime, ZonedDateTime.now(), null)
                    .toInstant().toEpochMilli()
                session.begin(now, wakeAt)
                container.sleepEffects.captureBaseline()
                container.scheduler.scheduleWake(wakeAt)
                now
            }

            val windowSeconds = effects.windDownMinutes * 60.0 + HOLD_TAIL_SECONDS
            var lastPhase: WindDownPhase? = null
            while (isActive) {
                val elapsed = (System.currentTimeMillis() - startAt) / 1000.0
                val state = engine.stateAt(elapsed, effects)
                container.sleepEffects.apply(state)
                if (state.phase != lastPhase) {
                    updateNotification(state.phase)
                    lastPhase = state.phase
                }
                if (elapsed >= windowSeconds) break   // settled → hold (service stays foreground)
                delay(TICK_MS)
            }
        }
    }

    private fun stopSession() {
        runJob?.cancel()
        goForeground(WindDownPhase.Restoring)
        scope.launch {
            if (container.sessionStore.active) {
                container.sleepEffects.restore()
                container.sessionStore.end()
            }
            ServiceCompat.stopForeground(this@SleepModeService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun goForeground(phase: WindDownPhase) {
        val type = if (Build.VERSION.SDK_INT >= 34) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(phase), type)
    }

    private fun updateNotification(phase: WindDownPhase) {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification(phase))
    }

    private fun buildNotification(phase: WindDownPhase): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, SleepModeService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, DrowzApplication.CHANNEL_SLEEP)
            .setSmallIcon(R.drawable.ic_moon)
            .setContentTitle(phase.title)
            .setContentText(phase.caption)
            .setOngoing(true)
            .setShowWhen(false)
            .setSilent(true)
            .setContentIntent(open)
            .addAction(0, "I'm awake", stop)
            .build()
    }

    companion object {
        private const val NOTIF_ID = 1001
        private const val TICK_MS = 1000L
        private const val HOLD_TAIL_SECONDS = 30.0
        const val ACTION_STOP = "com.drowz.app.action.STOP_SESSION"

        fun start(context: Context) = send(context, null)
        fun stop(context: Context) = send(context, ACTION_STOP)

        private fun send(context: Context, action: String?) {
            val intent = Intent(context, SleepModeService::class.java)
            if (action != null) intent.action = action
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
