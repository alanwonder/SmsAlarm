package com.example.smsalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
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
 * 这里不做短信匹配，匹配与防抖已在 SmsNotificationListener 完成，此处只负责"响铃"，
 * 并记录"通知出现 → 闹钟触发"的间隔。
 */
class AlarmTriggerReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_T_RECEIVE = "extra_t_receive"
        const val EXTRA_T_TRIGGER = "extra_t_trigger"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 用户在闹钟真正触发前（约 1 秒的延迟窗口内）关闭了监控，做一次兜底检查。
        if (!MonitorConfig.isEnabled(context)) {
            TriggerLogger.log(context, "trigger_skipped", "monitor_disabled")
            return
        }

        // 计时一律用单调时钟（elapsedRealtime），避免系统时间跳变污染间隔统计。
        val tReceive = intent.getLongExtra(EXTRA_T_RECEIVE, -1L)
        val tTrigger = SystemClock.elapsedRealtime()
        val note = if (tReceive > 0) "from_receive=${tTrigger - tReceive}ms" else "from_receive=unknown"

        // 先启动服务，再记录日志：报警启动绝不能排在文件 I/O 后面
        // （TriggerLogger 已异步写盘，这里再保持顺序作为双保险）。
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(EXTRA_T_RECEIVE, tReceive)
            putExtra(EXTRA_T_TRIGGER, tTrigger)
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        TriggerLogger.log(context, "trigger", note)
    }
}
