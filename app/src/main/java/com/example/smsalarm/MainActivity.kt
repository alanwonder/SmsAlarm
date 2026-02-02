package com.example.smsalarm

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val smsPermissions = arrayOf(
        Manifest.permission.POST_NOTIFICATIONS
        //Manifest.permission.RECEIVE_SMS,
        //Manifest.permission.READ_SMS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestRuntimePermissions()

        val btnToggle = findViewById<Button>(R.id.btnToggle)
        updateText(btnToggle)

        btnToggle.setOnClickListener {

            // ① 未开启通知监听 → 引导一次
            if (!hasNotificationListenerPermission()) {
                Toast.makeText(
                    this,
                    "请开启【短信报警】的通知读取权限",
                    Toast.LENGTH_LONG
                ).show()

                startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                )
                return@setOnClickListener
            }

            // ② 已授权 → 切换监控状态
            val enabled = !MonitorConfig.isEnabled(this)
            MonitorConfig.setEnabled(this, enabled)
            updateText(btnToggle)

            Toast.makeText(
                this,
                if (enabled) "监控已启用" else "监控已停止",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * 运行时权限申请
     */
    private fun requestRuntimePermissions() {
        val need = smsPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (need.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                need.toTypedArray(),
                1001
            )
        }
    }

    /**
     * 判断是否已开启 NotificationListener 权限
     */
    private fun hasNotificationListenerPermission(): Boolean {
        val cn = ComponentName(this, SmsNotificationListener::class.java)

        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        return enabledListeners.contains(cn.flattenToString())
    }


    private fun updateText(btn: Button) {
        btn.text = if (MonitorConfig.isEnabled(this)) {
            "停止监控"
        } else {
            "启用监控"
        }
    }
}
