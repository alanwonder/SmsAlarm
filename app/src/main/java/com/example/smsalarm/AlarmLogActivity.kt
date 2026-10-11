package com.example.smsalarm

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * 应用内触发日志查看页：不依赖 adb / 电脑，手机上直接看 trigger_timing.log。
 *
 * 展示最近 [MAX_LINES] 条（新的在上），支持刷新与清空；
 * 文件读取放在后台线程，避免主线程 I/O 阻塞（1MB 上限文件）。
 * 日志无敏感内容（只有时间戳/事件/包名/间隔毫秒，不含通知正文）。
 */
class AlarmLogActivity : AppCompatActivity() {

    companion object {
        private const val MAX_LINES = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_log)

        findViewById<Button>(R.id.btnLogRefresh).setOnClickListener { loadLog() }
        findViewById<Button>(R.id.btnLogClear).setOnClickListener { confirmClear() }
        loadLog()
    }

    override fun onResume() {
        super.onResume()
        loadLog()
    }

    private fun loadLog() {
        Thread {
            val file = TriggerLogger.logFile(this)
            val lines = if (file.exists()) file.readLines() else emptyList()
            runOnUiThread { render(lines) }
        }.start()
    }

    private fun render(lines: List<String>) {
        val tvContent = findViewById<TextView>(R.id.tvLogContent)
        val tvHint = findViewById<TextView>(R.id.tvLogHint)

        if (lines.isEmpty()) {
            tvHint.text = "还没有触发记录"
            tvContent.text = "收到包含「上海交警」的短信后，这里会出现计时链路：\n\n" +
                    "receive   → 命中关键字（通知出现时刻）\n" +
                    "trigger   → 精确闹钟触发（距 receive 毫秒）\n" +
                    "play      → 开始出声（距 trigger / receive 毫秒）\n\n" +
                    "防抖窗口内的重复短信会记为 debounce，不会响铃。"
            return
        }

        val total = lines.size
        val shown = lines.takeLast(MAX_LINES).reversed()
        tvHint.text = if (total > MAX_LINES) {
            "共 $total 条，显示最近 $MAX_LINES 条（新的在上）"
        } else {
            "共 $total 条（新的在上）"
        }
        tvContent.text = shown.joinToString("\n")
    }

    private fun confirmClear() {
        AlertDialog.Builder(this)
            .setTitle("清空触发日志")
            .setMessage("确定删除全部触发计时记录？此操作不可恢复。")
            .setPositiveButton("清空") { _, _ ->
                TriggerLogger.logFile(this).delete()
                loadLog()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
