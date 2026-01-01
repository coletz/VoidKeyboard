package dev.coletz.voidkeyboard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationCompat

object OverlayPermissionHelper {

    private const val CHANNEL_ID = "voidkeyboard_overlay_permission"
    private const val NOTIFICATION_ID = 1001

    fun hasPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun requestPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            showPermissionNotification(context)
        }
    }

    private fun showPermissionNotification(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Overlay Permission",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notification to request overlay permission"
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("VoidKeyboard Permission Required")
            .setContentText("Tap to grant overlay permission for popups")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
