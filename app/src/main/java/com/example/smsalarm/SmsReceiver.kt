package com.example.smsalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.content.ContextCompat

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val text = messages.joinToString("") { msg ->
                msg.messageBody
            }

            if (text.contains("交警")) {
                val serviceIntent = Intent(context, AlarmService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }
}
