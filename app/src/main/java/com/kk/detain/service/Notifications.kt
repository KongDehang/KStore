package com.kk.detain.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.kk.detain.MainActivity
import com.kk.detain.R

object Notifications {
    const val GUARD_CHANNEL = "guard_status"
    const val ALERT_CHANNEL = "guard_alert"
    const val GUARD_NOTIFICATION_ID = 1001
    const val CONNECTION_LOST_ID = 1002
    private const val CHANNEL_NAME = "守护状态"
    private const val ALERT_CHANNEL_NAME = "守护提醒"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(GUARD_CHANNEL) == null) {
            val channel = NotificationChannel(
                GUARD_CHANNEL,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "显示正在锁定或即将锁定的应用"
                setShowBadge(false)
                enableVibration(false)
            }
            manager.createNotificationChannel(channel)
        }
        if (manager.getNotificationChannel(ALERT_CHANNEL) == null) {
            val alert = NotificationChannel(
                ALERT_CHANNEL,
                ALERT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "守护断开或权限失效时提醒你"
                enableVibration(true)
            }
            manager.createNotificationChannel(alert)
        }
    }

    fun guardNotification(context: Context, title: String, text: String): Notification {
        ensureChannels(context)
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, GUARD_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(contentIntent)
            .build()
    }

    /**
     * Shown when the accessibility service is still switched on in settings but the system
     * has stopped delivering events, which silently turns the guard off.
     */
    fun connectionLostNotification(context: Context): Notification {
        ensureChannels(context)
        val settingsIntent = PendingIntent.getActivity(
            context,
            CONNECTION_LOST_ID,
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, ALERT_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("静水暂停了")
            .setContentText("无障碍服务被系统关掉了，点这里重新开启，否则守护不会生效。")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("无障碍服务被系统关掉了，守护暂时失效。点这里到无障碍设置里把「静水 · 应用守护」关闭再打开。"),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(settingsIntent)
            .build()
    }
}
