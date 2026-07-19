package com.mexc.mariabot.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.concurrent.atomic.AtomicInteger

object NotificationCenter {
    private const val TAG = "NotificationCenter"
    
    const val CHANNEL_TRADES_ID = "mariabot_trades"
    const val CHANNEL_SYSTEM_ID = "mariabot_system"
    
    private val notificationIdGenerator = AtomicInteger(1000)

    /**
     * Initializes notification channels for Android 8.0 (Oreo) and above.
     */
    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // Trade alerts channel (High importance)
            val tradeChannel = NotificationChannel(
                CHANNEL_TRADES_ID,
                "صفقات ماريا بوت (MariaBot Trades)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات فورية لفتح وإغلاق الصفقات وجني الأرباح ووقف الخسارة."
                enableVibration(true)
                setShowBadge(true)
            }

            // System notifications channel (Medium importance)
            val systemChannel = NotificationChannel(
                CHANNEL_SYSTEM_ID,
                "تنبيهات النظام (MariaBot System)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تنبيهات عامة للنظام ومستوى الاتصال والذكاء الاصطناعي."
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(tradeChannel)
            notificationManager.createNotificationChannel(systemChannel)
            Log.d(TAG, "Notification Channels initialized successfully.")
        }
    }

    /**
     * Sends a custom trade notification to the user's system tray.
     */
    fun sendTradeNotification(context: Context, title: String, message: String) {
        sendNotification(context, CHANNEL_TRADES_ID, title, message)
    }

    /**
     * Sends a custom system notification to the user's system tray.
     */
    fun sendSystemNotification(context: Context, title: String, message: String) {
        sendNotification(context, CHANNEL_SYSTEM_ID, title, message)
    }

    private fun sendNotification(context: Context, channelId: String, title: String, message: String) {
        // Handle runtime notification permission for Android 13+ (API 33)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Notification permission is not granted. Skipping system tray notification: $title - $message")
                return
            }
        }

        try {
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_chat) // Clean system tray icon
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(
                    if (channelId == CHANNEL_TRADES_ID) NotificationCompat.PRIORITY_HIGH 
                    else NotificationCompat.PRIORITY_DEFAULT
                )
                .setAutoCancel(true)

            val notificationId = notificationIdGenerator.getAndIncrement()
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
            Log.d(TAG, "Notification posted successfully: $title")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification: ${e.localizedMessage}", e)
        }
    }
}
