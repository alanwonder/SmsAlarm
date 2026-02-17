package com.example.smsalarm

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_PERMISSION_CODE = 1001
        private const val MIN_DEBOUNCE_MINUTES = 1
        private const val MAX_DEBOUNCE_MINUTES = 30
        private const val MIN_RING_MINUTES = 1
        private const val MAX_RING_MINUTES = 30
    }

    private val runtimePermissions by lazy {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private var pendingDebounceMinutes = MonitorConfig.DEFAULT_DEBOUNCE_MINUTES
    private var pendingRingMinutes = MonitorConfig.DEFAULT_RING_DURATION_MINUTES

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestRuntimePermissions()

        val btnToggle = findViewById<Button>(R.id.btnToggle)
        val switchEditMode = findViewById<SwitchCompat>(R.id.switchEditMode)
        val seekDebounce = findViewById<SeekBar>(R.id.seekDebounce)
        val seekRingDuration = findViewById<SeekBar>(R.id.seekRingDuration)
        val tvDebounceValue = findViewById<TextView>(R.id.tvDebounceValue)
        val tvRingDurationValue = findViewById<TextView>(R.id.tvRingDurationValue)

        updateText(btnToggle)

        pendingDebounceMinutes = MonitorConfig.getDebounceMinutes(this)
        pendingRingMinutes = MonitorConfig.getRingDurationMinutes(this)

        seekDebounce.max = MAX_DEBOUNCE_MINUTES - MIN_DEBOUNCE_MINUTES
        seekRingDuration.max = MAX_RING_MINUTES - MIN_RING_MINUTES
        seekDebounce.progress = pendingDebounceMinutes - MIN_DEBOUNCE_MINUTES
        seekRingDuration.progress = pendingRingMinutes - MIN_RING_MINUTES

        updateSliderValueText(tvDebounceValue, tvRingDurationValue)
        setSliderEditable(false, seekDebounce, seekRingDuration)

        seekDebounce.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                pendingDebounceMinutes = MIN_DEBOUNCE_MINUTES + progress
                updateSliderValueText(tvDebounceValue, tvRingDurationValue)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        seekRingDuration.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                pendingRingMinutes = MIN_RING_MINUTES + progress
                updateSliderValueText(tvDebounceValue, tvRingDurationValue)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        switchEditMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                pendingDebounceMinutes = MonitorConfig.getDebounceMinutes(this)
                pendingRingMinutes = MonitorConfig.getRingDurationMinutes(this)
                seekDebounce.progress = pendingDebounceMinutes - MIN_DEBOUNCE_MINUTES
                seekRingDuration.progress = pendingRingMinutes - MIN_RING_MINUTES
                updateSliderValueText(tvDebounceValue, tvRingDurationValue)
                setSliderEditable(true, seekDebounce, seekRingDuration)
                Toast.makeText(this, "已进入编辑模式", Toast.LENGTH_SHORT).show()
            } else {
                MonitorConfig.setDebounceMinutes(this, pendingDebounceMinutes)
                MonitorConfig.setRingDurationMinutes(this, pendingRingMinutes)
                setSliderEditable(false, seekDebounce, seekRingDuration)
                Toast.makeText(this, "参数已生效", Toast.LENGTH_SHORT).show()
            }
        }

        if (MonitorConfig.isEnabled(this)) {
            startKeepAliveIfAllowed()
        }

        btnToggle.setOnClickListener {
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

            val enabled = !MonitorConfig.isEnabled(this)
            MonitorConfig.setEnabled(this, enabled)
            updateText(btnToggle)

            if (enabled) {
                if (!canPostNotifications()) {
                    requestRuntimePermissions()
                    Toast.makeText(
                        this,
                        "请授予通知权限以保持后台监控",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    startKeepAliveIfAllowed()
                }
            } else {
                stopService(Intent(this, KeepAliveService::class.java))
            }

            Toast.makeText(
                this,
                if (enabled) "监控已启用" else "监控已停止",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE && MonitorConfig.isEnabled(this) && canPostNotifications()) {
            startKeepAliveIfAllowed()
        }
    }

    private fun setSliderEditable(editable: Boolean, vararg seekBars: SeekBar) {
        seekBars.forEach {
            it.isEnabled = editable
            it.isClickable = editable
        }
    }

    private fun updateSliderValueText(tvDebounce: TextView, tvRingDuration: TextView) {
        tvDebounce.text = "当前防抖阈值：${pendingDebounceMinutes} 分钟"
        tvRingDuration.text = "当前响铃时长：${pendingRingMinutes} 分钟"
    }

    private fun requestRuntimePermissions() {
        if (runtimePermissions.isEmpty()) {
            return
        }

        val need = runtimePermissions.filter { permission ->
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (need.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                need.toTypedArray(),
                REQUEST_PERMISSION_CODE
            )
        }
    }

    private fun hasNotificationListenerPermission(): Boolean {
        val cn = ComponentName(this, SmsNotificationListener::class.java)

        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        return enabledListeners.contains(cn.flattenToString())
    }

    private fun canPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun startKeepAliveIfAllowed() {
        if (!canPostNotifications()) {
            return
        }
        ContextCompat.startForegroundService(
            this,
            Intent(this, KeepAliveService::class.java)
        )
    }

    private fun updateText(btn: Button) {
        btn.text = if (MonitorConfig.isEnabled(this)) {
            "停止监控"
        } else {
            "启用监控"
        }
    }
}
