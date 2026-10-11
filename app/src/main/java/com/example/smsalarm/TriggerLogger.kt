package com.example.smsalarm

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 触发链路计时日志：用于真机统计"通知出现 → 报警出声"的实际延迟。
 *
 * 事件：
 *  - receive             监听器命中关键字（备注：来源包名）
 *  - debounce            防抖窗口内被丢弃（备注：距上次触发毫秒）
 *  - trigger             精确闹钟触发 AlarmTriggerReceiver（备注：距 receive 毫秒）
 *  - play                报警服务首次启动、真正开始播放（备注：距 trigger / 距 receive 毫秒）
 *  - retrigger_during_alarm  报警仍响铃时又来一次触发，无新播放开始（备注同上；
 *                            暴露"防抖 < 响铃时长"的配置问题）
 *
 * 设计约束：
 *  - 写盘是异步的（单线程串行执行器），绝不阻塞调用线程——监听器/接收器主路径
 *    不能被文件 I/O（含 1MB 轮转的删除+新建）拖慢，否则插桩本身会污染延迟数据。
 *  - 延迟计算必须用单调时钟 SystemClock.elapsedRealtime()（调用方传入/计算），
 *    墙钟时间只用于日志行的时间戳展示，避免 NTP/手动改时间造成负值或离谱间隔。
 *  - 日志只记录时间戳、事件、包名与间隔毫秒，不记录通知正文内容。
 *  - 文件超过 1MB 时截断重开，避免无限增长。
 */
object TriggerLogger {

    private const val TAG = "TPAlert"
    private const val FILE_NAME = "trigger_timing.log"
    private const val MAX_LOG_BYTES = 1_000_000L

    // 仅被单线程执行器使用，无并发问题。
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)

    // 单线程串行写盘保证行序；daemon 线程避免阻止进程退出。
    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "tpalert-logger").apply { isDaemon = true }
    }

    fun log(context: Context, event: String, note: String) {
        Log.i(TAG, "$event: $note")
        val appContext = context.applicationContext
        // 事件时刻的墙钟值必须在入队时捕获：若延迟到写盘线程求值，队列积压时
        // 时间戳会滞后于真实事件时刻，展示时间错位。
        val eventTimeMs = System.currentTimeMillis()
        executor.execute {
            try {
                // 格式化只在单线程执行器内进行，SimpleDateFormat 无并发问题。
                val line = "${timeFormat.format(Date(eventTimeMs))}\t$event\t$note"
                val file = File(appContext.filesDir, FILE_NAME)
                if (file.exists() && file.length() > MAX_LOG_BYTES) {
                    // 简单轮转：超限则重开新文件，避免日志无限膨胀。
                    file.delete()
                }
                FileWriter(file, true).use { it.write(line + "\n") }
            } catch (t: Throwable) {
                Log.w(TAG, "写触发日志失败: ${t.message}")
            }
        }
    }

    /** 日志文件路径，供调试时导出。 */
    fun logFile(context: Context): File = File(context.filesDir, FILE_NAME)
}
