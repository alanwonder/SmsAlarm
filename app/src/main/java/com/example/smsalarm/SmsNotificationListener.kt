package com.example.smsalarm

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Intent
import androidx.core.content.ContextCompat

class SmsNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName

        // 系统短信包名（不同ROM可能不同）
        if (pkg == "com.google.android.apps.messaging"
            || pkg == "com.android.mms"
            || pkg.contains("sms")) {

            val extras = sbn.notification.extras
            val text = extras.getCharSequence("android.text")?.toString() ?: return

            if (text.contains("上海交警")) {
                val intent = Intent(this, AlarmService::class.java)
                startForegroundService(intent)
            }
        }
    }
}

