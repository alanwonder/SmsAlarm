package com.example.smsalarm

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

class SmsNotificationListener : NotificationListenerService() {

    private val keywords = listOf("交通提示") // 可以扩展多个关键词

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        val content = "$title $text"

        if (keywords.any { content.contains(it) }) {
            // 启动前台服务播放铃声
            val serviceIntent = Intent(this, AlarmService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }
}
