package com.example.smsalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * 由 AlarmManager 的精确闹钟（setAlarmClock）触发。
 *
 * 为什么需要这一层中转：
 * SmsNotificationListener.onNotificationPosted 属于后台回调，在 Android 12+ 上
 * 直接从那里 startForegroundService(AlarmService) 会被系统抛
 * ForegroundServiceStartNotAllowedException，报警根本起不来。
 * 而"应用调用精确闹钟完成用户请求的操作"是官方豁免场景之一——
 * 由闹钟触发广播接收器、再在接收器里拉起前台服务，属于前台豁免，可正常启动。
 *
 * 这里不做短信匹配，匹配与防抖已在 SmsNotificationListener 完成，此处只负责"响铃"。
 */
class AlarmTriggerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 用户在闹钟真正触发前（约 1 秒的延迟窗口内）关闭了监控，做一次兜底检查。
        if (!MonitorConfig.isEnabled(context)) return

        ContextCompat.startForegroundService(
            context,
            Intent(context, AlarmService::class.java)
        )
    }
}
