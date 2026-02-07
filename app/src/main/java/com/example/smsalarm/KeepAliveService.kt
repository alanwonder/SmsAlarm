package com.example.smsalarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class KeepAliveService : Service() {

    companion object {
        const val CHANNEL_ID = "keep_alive_channel"
        const val NOTIFICATION_ID = 100
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "后台守护",
            NotificationManager.IMPORTANCE_MIN
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("短信监控运行中")
            .setContentText("用于保持后台运行")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }
}
