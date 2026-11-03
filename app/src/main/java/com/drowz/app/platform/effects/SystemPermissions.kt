package com.drowz.app.platform.effects

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Runtime permission checks plus the system-settings intents needed to grant each Drowz
 * effect. One source of truth for both [AndroidSleepEffects.capabilities] and the setup UI.
 */
object SystemPermissions {

    fun canWriteSystemSettings(context: Context): Boolean =
        Settings.System.canWrite(context)

    fun canDrawOverlay(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    fun hasDndAccess(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    /** True grayscale needs WRITE_SECURE_SETTINGS — ADB-granted only (SPEC §4). */
    fun hasSecureSettings(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.canScheduleExactAlarms()
    }

    // --- grant intents (caller starts them from an Activity) ---

    fun writeSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, packageUri(context))

    fun overlayIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri(context))

    fun dndAccessIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    fun exactAlarmIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri(context))

    private fun packageUri(context: Context): Uri = Uri.parse("package:${context.packageName}")
}
