package com.example.smsalarm

import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat
import androidx.core.content.edit

class SmsNotificationListener : NotificationListenerService() {

    companion object {
        private const val SP_NAME = "alarm"
        private const val KEY_LAST_TRIGGER = "last_trigger"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!MonitorConfig.isEnabled(this)) return

        val pkg = sbn.packageName
        if (!isSmsPackage(pkg)) return

        if (!isNewNotification(sbn)) return

        val extras = sbn.notification.extras
        val text = extras.getCharSequence("android.text")
            ?: extras.getCharSequence("android.bigText")
            ?: return

        if (!text.contains("上海交警")) return

        val sp = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        val last = sp.getLong(KEY_LAST_TRIGGER, 0L)
        val now = System.currentTimeMillis()
        val debounceInterval = MonitorConfig.getDebounceMinutes(this) * 60 * 1000L

        if (now - last < debounceInterval) {
            return
        }

        sp.edit { putLong(KEY_LAST_TRIGGER, now) }

        val intent = Intent(this, AlarmService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun isSmsPackage(pkg: String): Boolean {
        return pkg == "com.google.android.apps.messaging"
                || pkg == "com.android.mms"
                || pkg.contains("sms", ignoreCase = true)
    }

    private fun isNewNotification(sbn: StatusBarNotification): Boolean {
        val sp = getSharedPreferences("alarm", MODE_PRIVATE)

        val lastKey = sp.getString("last_key", null)
        val currentKey = "${sbn.id}_${sbn.postTime}"

        if (currentKey == lastKey) return false

        sp.edit { putString("last_key", currentKey) }
        return true
    }


}
