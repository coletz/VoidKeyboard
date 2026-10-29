package dev.coletz.voidkeyboard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri

object OverlayPermissionHelper {

    private const val CHANNEL_ID = "voidkeyboard_overlay_permission"
    private const val NOTIFICATION_ID = 1001

    fun hasPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

    /**
     * Called from the IME when a popup can't be shown. A notification doesn't interrupt typing and,
     * unlike starting an activity from the IME, can't be blocked by background activity start
     * restrictions, so the settings screen is opened directly only if notifications are disabled.
     */
    fun requestPermission(context: Context) {
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            showPermissionNotification(context)
        } else {
            openSettings(context)
        }
    }

    fun openSettings(context: Context) {
        val intent = settingsIntent(context).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            "Overlay permission settings not available: $e".log()
        }
    }

    private fun settingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
    }

    private fun showPermissionNotification(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.overlay_permission_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.overlay_permission_channel_desc)
        }
        notificationManager.createNotificationChannel(channel)

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            settingsIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.overlay_permission_title))
            .setContentText(context.getString(R.string.overlay_permission_notification_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            // Every long press without the permission re-posts it, don't buzz each time
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
