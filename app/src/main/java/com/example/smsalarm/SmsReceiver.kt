/*package com.example.smsalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.content.ContextCompat
import androidx.core.content.edit

class SmsReceiver : BroadcastReceiver() {

    companion object {
        var lastTriggerTime = 0L
        const val DEBOUNCE_INTERVAL = 1 * 60 * 1000L // 15分钟防抖
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val content = messages.joinToString("") { it.messageBody }

            if (content.contains("上海交警")) {

                val sp = context.getSharedPreferences("alarm", Context.MODE_PRIVATE)
                val lastTime = sp.getLong("last_trigger", 0L)
                val now = System.currentTimeMillis()

                if (now - lastTime > DEBOUNCE_INTERVAL) {
                    sp.edit { putLong("last_trigger", now) }

                    val serviceIntent = Intent(context, AlarmService::class.java)
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
            }
        }
    }

}
*/
