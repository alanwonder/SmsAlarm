package com.example.smsalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.content.edit

class SmsNotificationListener : NotificationListenerService() {

    companion object {
        private const val SP_NAME = "alarm"
        private const val KEY_LAST_TRIGGER = "last_trigger"
        private const val ALARM_DELAY_MS = 1000L
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!MonitorConfig.isEnabled(this)) return

        val pkg = sbn.packageName
        if (!isSmsPackage(pkg)) return

        if (!isNewNotification(sbn)) return

        val text = extractNotificationText(sbn)
        if (text.isNullOrBlank() || !text.contains("上海交警")) return

        val now = System.currentTimeMillis()
        TriggerLogger.log(this, "receive", "pkg=$pkg")

        val sp = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        val last = sp.getLong(KEY_LAST_TRIGGER, 0L)
        val debounceInterval = MonitorConfig.getDebounceMinutes(this) * 60 * 1000L

        if (now - last < debounceInterval) {
            TriggerLogger.log(this, "debounce", "skip=${now - last}ms < window=${debounceInterval}ms")
            return
        }

        sp.edit { putLong(KEY_LAST_TRIGGER, now) }

        scheduleAlarm(now)
    }

    /**
     * 关键：不在 onNotificationPosted 里直接 startForegroundService。
     * 这是后台回调，Android 12+ 会抛 ForegroundServiceStartNotAllowedException。
     * 改为用精确闹钟 setAlarmClock（无需 SCHEDULE_EXACT_ALARM 权限、Doze 下仍准点）
     * 中转，由 AlarmTriggerReceiver 在前台豁免场景下再拉起 AlarmService。
     * tReceiveMs 随 PendingIntent extra 传递，供下游计算"通知→闹钟→播放"间隔。
     */
    private fun scheduleAlarm(tReceiveMs: Long) {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmTriggerReceiver::class.java).apply {
            putExtra(AlarmTriggerReceiver.EXTRA_T_RECEIVE, tReceiveMs)
        }
        val pi = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setAlarmClock(
            AlarmManager.AlarmClockInfo(System.currentTimeMillis() + ALARM_DELAY_MS, null),
            pi
        )
    }

    /**
     * 合并 title + text + bigText + MessagingStyle 多条消息正文。
     * 避免会话式通知（如 Google Messages）把正文放在 bigText / MessagingStyle，
     * 而 android.text 只是摘要或发送者名时漏报。
     */
    private fun extractNotificationText(sbn: StatusBarNotification): CharSequence? {
        val extras = sbn.notification.extras
        val sb = StringBuilder()

        NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(sbn.notification)
            ?.messages
            ?.forEach { appendIfNotNull(sb, it.text) }

        appendIfNotNull(sb, extras.getCharSequence(NotificationCompat.EXTRA_TITLE))
        appendIfNotNull(sb, extras.getCharSequence(NotificationCompat.EXTRA_TEXT))
        appendIfNotNull(sb, extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT))

        return if (sb.isBlank()) null else sb
    }

    private fun appendIfNotNull(sb: StringBuilder, cs: CharSequence?) {
        if (!cs.isNullOrBlank()) {
            sb.append(cs).append(' ')
        }
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
