package com.example.smsalarm

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 触发链路计时日志：用于真机统计"通知出现 → 报警出声"的实际延迟。
 *
 * 事件：
 *  - receive   监听器命中关键字（备注：来源包名）
 *  - debounce  防抖窗口内被丢弃（备注：距上次触发毫秒）
 *  - trigger   精确闹钟触发 AlarmTriggerReceiver（备注：距 receive 毫秒）
 *  - play      AlarmService 开始播放（备注：距 trigger / 距 receive 毫秒）
 *
 * 输出：logcat（Tag=TPAlert）+ 应用私有目录 trigger_timing.log（TSV，ISO 毫秒时间戳）。
 * 日志只记录时间戳、事件、包名与间隔毫秒，不记录通知正文内容。
 * 文件超过 1MB 时截断重开，避免无限增长。
 */
object TriggerLogger {

    private const val TAG = "TPAlert"
    private const val FILE_NAME = "trigger_timing.log"
    private const val MAX_LOG_BYTES = 1_000_000L

    private val lock = ReentrantLock()
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US)

    fun log(context: Context, event: String, note: String) {
        val line = "${timeFormat.format(Date())}\t$event\t$note"
        Log.i(TAG, "$event: $note")

        lock.withLock {
            try {
                val file = File(context.filesDir, FILE_NAME)
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
